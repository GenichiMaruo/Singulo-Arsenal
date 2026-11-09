package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.client.art.ArsenalRenderTypes;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import io.github.genichimaruo.singuloarsenal.client.art.MachineArt;
import io.github.genichimaruo.singuloarsenal.client.art.WeaponArt;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Everything drawn in the world rather than on a body: impact rings, jets, the tidal focus, the SQUID overlay, spaghettification. */
final class FxClient {
    private record Fx(FxPayload data, long born) {}
    private record Stretch(float held, long heldUntil, long shearBorn, float shear) {}
    private record Muzzle(Vec3 position, long frame) {}

    private static final List<Fx> ACTIVE = new ArrayList<>();
    private static final Map<Integer, Stretch> STRETCHES = new HashMap<>();
    private static final Map<Integer, Muzzle> MUZZLES = new HashMap<>();
    /** Counts world passes, so a muzzle remembered from an earlier frame (its owner now culled) is not trusted. */
    private static long frame;
    private static ClientLevel scene;

    private FxClient() {}

    static void register() {
        FxPayload.setClientHandler(FxClient::receive);
        NeoForge.EVENT_BUS.addListener(FxClient::tick);
        NeoForge.EVENT_BUS.addListener(FxClient::world);
        // Lowest priority: if anyone cancels the render, the matching Post never fires and neither must the push.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, FxClient::stretchBegin);
        NeoForge.EVENT_BUS.addListener(FxClient::stretchEnd);
    }

    private static int lifetime(int kind) {
        return switch (kind) {
            case FxPayload.RIPPLE -> 12;
            case FxPayload.COLLAPSE, FxPayload.SLAM -> 18;
            case FxPayload.HORIZON -> 40;
            case FxPayload.SHEAR -> 16;
            case FxPayload.RUPTURE -> 14;
            case FxPayload.BIT_SHOT -> 6;
            default -> 16;
        };
    }

    private static void receive(FxPayload fx) {
        var mc = Minecraft.getInstance();
        if (mc.level == null || !finite(fx.a()) || !finite(fx.b())) return;
        if (scene != mc.level) clear(mc.level);
        long now = mc.level.getGameTime();
        if (fx.kind() == FxPayload.STRETCH) {
            Stretch old = STRETCHES.get(fx.entity());
            if (fx.value() < 0) {
                STRETCHES.put(fx.entity(), new Stretch(-fx.value(), now + Rules.STRESS_TICKS, old == null ? 0 : old.shearBorn, old == null ? 0 : old.shear));
            } else {
                STRETCHES.put(fx.entity(), new Stretch(0, 0, now, fx.value()));
            }
            return;
        }
        if (ACTIVE.size() >= 64) ACTIVE.removeFirst();
        ACTIVE.add(new Fx(fx, now));
    }

    private static boolean finite(Vec3 v) {
        return Double.isFinite(v.x) && Double.isFinite(v.y) && Double.isFinite(v.z);
    }

    private static void clear(ClientLevel next) {
        ACTIVE.clear();
        STRETCHES.clear();
        MUZZLES.clear();
        scene = next;
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (scene != mc.level) clear(mc.level);
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        ACTIVE.removeIf(fx -> now - fx.born > lifetime(fx.data.kind()));
        STRETCHES.values().removeIf(s -> s.heldUntil < now && now - s.shearBorn > 20);
    }

    // ---- spaghettification ---------------------------------------------------------------------------------------

    private static void stretchBegin(RenderLivingEvent.Pre<?, ?> event) {
        Stretch stretch = STRETCHES.get(event.getEntity().getId());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        if (stretch == null || scene == null) return;
        float age = scene.getGameTime() - stretch.shearBorn + event.getPartialTick();
        float fade = stretch.shearBorn == 0 ? 0 : Math.max(0, 1 - age / 16);
        float stress = scene.getGameTime() < stretch.heldUntil ? stretch.held : 0;
        float length = Rules.stretchFor((int) stress, fade * (0.6F + 0.1F * stretch.shear));
        float width = 1 / (float) Math.sqrt(length);
        pose.scale(width, length, width);
    }

    private static void stretchEnd(RenderLivingEvent.Post<?, ?> event) {
        event.getPoseStack().popPose();
    }

    // ---- world pass ----------------------------------------------------------------------------------------------

    private static void world(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        PoseStack pose = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        pose.pushPose();
        // AFTER_LEVEL supplies an identity pose; the terrain renderer has already restored its view stack.
        pose.mulPose(event.getModelViewMatrix());
        pose.translate(-camera.x, -camera.y, -camera.z);
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();

        drawHoles(pose, buffers, mc, partial);
        drawBits(pose, buffers, mc, partial);
        VertexConsumer glow = buffers.getBuffer(ArsenalRenderTypes.GLOW);
        long now = mc.level.getGameTime();
        for (Fx fx : ACTIVE) {
            float age = now - fx.born + partial;
            float life = lifetime(fx.data.kind());
            if (age < 0 || age > life) continue;
            drawFx(pose, glow, fx.data, age, 1 - age / life);
        }
        for (Player player : mc.level.players()) {
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof JetLanceItem) drawImpact(pose, glow, player, partial);
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof TidalReaverItem) drawThreads(pose, glow, mc.level, player, partial, now);
        }
        buffers.endBatch(ArsenalRenderTypes.GLOW);
        drawWorldJets(pose, buffers, mc, partial, camera);
        drawSquid(pose, buffers, mc, partial);
        pose.popPose();
        frame++;
    }

    private static void drawFx(PoseStack pose, VertexConsumer glow, FxPayload fx, float age, float fade) {
        Vec3 at = fx.a();
        float strength = fade * fade;
        switch (fx.kind()) {
            case FxPayload.RIPPLE -> {
                int rgb = fx.value() >= 2 ? 0xFFB347 : 0x7CE8FF;
                Vec3 axis = fx.b().lengthSqr() > 1.0E-3 ? fx.b() : new Vec3(0, 1, 0);
                Canvas.worldRing(pose, glow, at, axis, 0.25 + age * 0.09, 0.14, rgb, strength * 0.9F);
                Canvas.worldRing(pose, glow, at, axis, 0.12 + age * 0.05, 0.08, 0xFFFFFF, strength * 0.7F);
            }
            case FxPayload.COLLAPSE, FxPayload.SLAM -> {
                double radius = 0.4 + (fx.value() - 0.4) * (1 - Math.pow(fade, 2));
                Vec3 up = new Vec3(0, 1, 0);
                Canvas.worldRing(pose, glow, at, up, radius, 0.5, 0xFFB347, strength);
                Canvas.worldRing(pose, glow, at, up, radius * 0.7, 0.3, 0xFFF0C8, strength * 0.8F);
            }
            case FxPayload.QUENCH -> {
                double radius = 0.3 + fx.value() * (1 - fade);
                Canvas.worldRing(pose, glow, at, new Vec3(0, 1, 0), radius, 0.4, 0x7CE8FF, strength);
                Canvas.worldRing(pose, glow, at, new Vec3(1, 0, 0), radius * 0.8, 0.25, 0xFFFFFF, strength * 0.7F);
                Canvas.worldRing(pose, glow, at, new Vec3(0, 0, 1), radius * 0.8, 0.25, 0xFFFFFF, strength * 0.7F);
            }
            case FxPayload.HORIZON -> {
                double radius = fx.value() * (1 - fade * fade);
                for (Vec3 axis : new Vec3[]{new Vec3(0, 1, 0), new Vec3(1, 0, 0), new Vec3(0, 0, 1)}) {
                    Canvas.worldRing(pose, glow, at, axis, radius, 0.9, 0xFFD28A, strength);
                }
                Canvas.worldRing(pose, glow, at, new Vec3(0, 1, 0), radius * 0.6, 0.6, 0xFFFFFF, fade);
            }
            case FxPayload.JET_BURST -> {
                Vec3 thrust = at.subtract(fx.b()).normalize();
                Canvas.worldRing(pose, glow, at, thrust, 0.5 + age * 0.3, 0.2, 0xBFE6FF, strength);
            }
            case FxPayload.BIT_SHOT -> {
                if (Minecraft.getInstance().level.getEntity(fx.entity()) instanceof Player owner) {
                    int count = bitCount(owner);
                    if (count > 0) {
                        Vec3 from = bitPosition(owner, (int) fx.value(), count, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
                        Canvas.tube(pose, glow, from, at, 0.05 * fade, 0x7CE8FF, strength * 0.6F);
                        Canvas.tube(pose, glow, from, at, 0.02 * fade, 0xFFFFFF, strength);
                        Canvas.worldRing(pose, glow, at, at.subtract(from).normalize(), 0.12 + age * 0.05, 0.08, 0x7CE8FF, strength);
                    }
                }
            }
            case FxPayload.RUPTURE -> {
                Canvas.worldRing(pose, glow, at, fx.b(), 0.2 + age * 0.14, 0.16, 0xB8A8FF, strength);
                Canvas.worldRing(pose, glow, at, fx.b(), 0.1 + age * 0.08, 0.07, 0xFFFFFF, strength);
                Canvas.tube(pose, glow, at.subtract(fx.b().scale(1.5)), at.add(fx.b().scale(1.5)), 0.05, 0xFFFFFF, strength);
            }
            case FxPayload.SHEAR -> {
                Canvas.worldRing(pose, glow, at, fx.b(), fx.value() * (1 - fade), 0.6, 0xFFE2A0, strength);
                Canvas.worldRing(pose, glow, at, fx.b(), fx.value() * (1 - fade) * 0.6, 0.3, 0xFFFFFF, strength * 0.8F);
            }
            default -> {}
        }
    }

    private static boolean firstPerson(Player player) {
        return player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    /** Where the jet lands: the same eye-ray the server damages along, so the beam always meets what it is hitting. */
    private static net.minecraft.world.phys.BlockHitResult aim(Player player, float partial) {
        Vec3 eye = player.getEyePosition(partial);
        return player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(partial).scale(Rules.LANCE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    private static float focus(float held) {
        return Math.max(0, Math.min(1, (held - Rules.LANCE_IGNITE) / (Rules.LANCE_COLLIMATE - Rules.LANCE_IGNITE)));
    }

    /**
     * Turns the lance on its grip so the barrel points exactly at what the player is aiming at. The beam then leaves the muzzle
     * along the barrel's own axis, instead of merely starting at the muzzle. Run before anything else moves the art.
     */
    static void aimLance(Player owner, PoseStack pose, ItemDisplayContext context, float partial) {
        var mc = Minecraft.getInstance();
        // The inventory's paper doll is drawn in GUI space, where world directions mean nothing.
        if (!context.firstPerson() && mc.screen instanceof net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen) return;
        Vec3 eye = owner.getEyePosition(partial);
        Vec3 look = owner.getViewVector(partial);
        boolean firing = owner.isUsingItem() && owner.getUseItem().getItem() instanceof JetLanceItem;
        // Never closer than a few blocks, or a wall at arm's length would wrench the gun sideways.
        double distance = firing ? Math.max(3, eye.distanceTo(aim(owner, partial).getLocation())) : 40;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 target = eye.add(look.scale(distance)).subtract(camera);
        Matrix4f matrix = pose.last().pose();
        Vector3f origin = matrix.transformPosition(0, 0, 0, new Vector3f());
        Vector3f direction = new Vector3f((float) target.x, (float) target.y, (float) target.z).sub(origin).normalize();
        new Matrix4f(matrix).invert().transformDirection(direction).normalize();
        pose.mulPose(new Quaternionf().rotationTo(0, 1, 0, direction.x, direction.y, direction.z));
    }

    /**
     * Called from the item renderer with the pose of the lance actually on screen. In third person the muzzle is remembered for the
     * world pass. First-person hands are drawn later with their own projection and a cleared depth buffer, so the jet is drawn here.
     */
    static void lanceRendered(Player owner, PoseStack pose, MultiBufferSource buffers, ItemDisplayContext context, float partial) {
        Vector3f tip = new Matrix4f(pose.last().pose()).transformPosition(0, (float) WeaponArt.LANCE_MUZZLE, 0, new Vector3f());
        var mc = Minecraft.getInstance();
        if (!context.firstPerson()) {
            Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
            MUZZLES.put(owner.getId(), new Muzzle(camera.add(tip.x, tip.y, tip.z), frame));
            return;
        }
        if (!owner.isUsingItem() || !(owner.getUseItem().getItem() instanceof JetLanceItem)) return;
        float held = JetLanceItem.held(owner.getUseItem(), owner) + partial;
        double distance = owner.getEyePosition(partial).distanceTo(aim(owner, partial).getLocation());
        // The hand pose starts from the inverse of the view rotation, so this space has world axes centred on the camera.
        drawJet(new PoseStack(), buffers, Vec3.ZERO, new Vec3(tip.x, tip.y, tip.z), owner.getViewVector(partial).scale(distance), held, 0.55F);
    }

    /** One jet: a short plasma plume while igniting, then narrowing through the three rings into a tight beam. */
    private static void drawJet(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 from, Vec3 to, float held, float scale) {
        float radius = Rules.lanceRadius((int) held) * scale;
        float pulse = 1 + 0.1F * (float) Math.sin(held * 1.3);
        if (held < Rules.LANCE_IGNITE) {
            float grow = held / Rules.LANCE_IGNITE;
            Vec3 axis = to.subtract(from).normalize();
            JetTube.draw(pose, buffers, camera, from, from.add(axis.scale(0.4 + grow * 3)), radius * grow, 0, 0.8F * grow, 0.6F);
            return;
        }
        JetTube.draw(pose, buffers, camera, from, to, radius * pulse, focus(held), 1, 1.1F);
    }

    /** Jets of everyone drawn in the world. The local first-person jet is drawn with the hand instead. */
    private static void drawWorldJets(PoseStack pose, MultiBufferSource.BufferSource buffers, Minecraft mc, float partial, Vec3 camera) {
        for (Player player : mc.level.players()) {
            if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof JetLanceItem)) continue;
            if (firstPerson(player) && !mc.options.hideGui) continue;
            Muzzle known = MUZZLES.get(player.getId());
            Vec3 from;
            if (known != null && known.frame == frame) {
                from = known.position;
            } else {
                Vec3 look = player.getViewVector(partial);
                from = player.getEyePosition(partial).add(look.scale(1.5)).add(0, -0.4, 0);
            }
            drawJet(pose, buffers, camera, from, aim(player, partial).getLocation(), JetLanceItem.held(player.getUseItem(), player) + partial, 0.85F);
        }
        long now = mc.level.getGameTime();
        for (Fx fx : ACTIVE) {
            if (fx.data.kind() != FxPayload.JET_BURST) continue;
            float age = now - fx.born + partial, fade = 1 - age / lifetime(FxPayload.JET_BURST);
            if (age < 0 || fade <= 0) continue;
            JetTube.draw(pose, buffers, camera, fx.data.a(), fx.data.a().lerp(fx.data.b(), Math.min(1, age / 3)), 0.5F * fade, 0.2F, fade, 0.4F);
        }
        buffers.endBatch(JetTube.type());
    }

    private static void drawImpact(PoseStack pose, VertexConsumer glow, Player player, float partial) {
        float held = JetLanceItem.held(player.getUseItem(), player) + partial;
        if (held < Rules.LANCE_COLLIMATE) return;
        var hit = aim(player, partial);
        if (hit.getType() != HitResult.Type.BLOCK) return;
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        float pulse = 0.85F + 0.15F * (float) Math.sin(held * 1.3);
        Canvas.worldRing(pose, glow, hit.getLocation().add(normal.scale(0.05)), normal, 0.5 * pulse, 0.25, 0x8CD2FF, 0.8F);
        Canvas.worldRing(pose, glow, hit.getLocation().add(normal.scale(0.06)), normal, 0.22 * pulse, 0.14, 0xFFFFFF, 0.9F);
    }

    private static int bitCount(Player player) {
        return Modules.count(player.getItemBySlot(EquipmentSlot.CHEST), ModuleType.BIT_DRONE) * Rules.BITS_PER_MODULE;
    }

    /** Bits circle the wearer at shoulder height. Purely a function of time, so every client agrees where each one is. */
    private static Vec3 bitPosition(Player player, int index, int count, float partial) {
        double t = (player.level().getGameTime() + partial) * 0.06 + index * Math.PI * 2 / count;
        return player.getPosition(partial).add(Math.cos(t) * 1.05, 1.55 + 0.14 * Math.sin(t * 2.3 + index), Math.sin(t) * 1.05);
    }

    private static void drawBits(PoseStack pose, MultiBufferSource.BufferSource buffers, Minecraft mc, float partial) {
        boolean any = false;
        float t = mc.level.getGameTime() + partial;
        for (Player player : mc.level.players()) {
            int count = bitCount(player);
            if (count == 0 || player.isInvisible()) continue;
            any = true;
            for (int i = 0; i < count; i++) {
                Vec3 at = bitPosition(player, i, count, partial);
                pose.pushPose();
                pose.translate(at.x, at.y, at.z);
                pose.mulPose(com.mojang.math.Axis.YP.rotation(-t * 0.06F - (float) (i * Math.PI * 2 / count)));
                pose.scale(1F / 16, 1F / 16, 1F / 16);
                int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(mc.level, net.minecraft.core.BlockPos.containing(at));
                Canvas.drawFine(pose, buffers, t, light, Canvas.noOverlay(), true, c -> MachineArt.bit(c, false));
                pose.popPose();
            }
        }
        if (any) buffers.endBatch();
    }

    private static void drawHoles(PoseStack pose, MultiBufferSource.BufferSource buffers, Minecraft mc, float partial) {
        boolean any = false;
        for (Player player : mc.level.players()) any |= player.isUsingItem() && player.getUseItem().getItem() instanceof TidalReaverItem;
        if (!any) return;
        for (Player player : mc.level.players()) {
            if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof TidalReaverItem)) continue;
            Vec3 focus = TidalReaverItem.focus(player, partial);
            float use = Math.min(1, (player.getTicksUsingItem() + partial) / Rules.REAVER_MAX_HOLD);
            pose.pushPose();
            pose.translate(focus.x, focus.y, focus.z);
            float scale = 1F / 16;
            pose.scale(scale, scale, scale);
            float t = mc.level.getGameTime() + partial;
            Canvas.draw(pose, buffers, t, 0xF000F0, Canvas.noOverlay(), c -> {
                c.faceCamera();
                c.pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(-14));
                c.blackHole(4 + use * 5, 3.4, 0.7F + use * 0.3F);
            });
            pose.popPose();
        }
        buffers.endBatch(RenderType.entityCutoutNoCull(Canvas.WHITE));
        buffers.endBatch(RenderType.entityTranslucentEmissive(Canvas.WHITE));
    }

    private static void drawThreads(PoseStack pose, VertexConsumer glow, ClientLevel level, Player player, float partial, long now) {
        Vec3 focus = TidalReaverItem.focus(player, partial);
        double range = Rules.REAVER_RANGE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(focus, focus).inflate(range), e -> e != player && e.isAlive() && !(e instanceof ArmorStand) && !(e instanceof Player p && p.isCreative()))) {
            Vec3 center = target.getPosition(partial).add(0, target.getBbHeight() / 2, 0);
            if (center.distanceTo(focus) > range) continue;
            Vec3 delta = center.subtract(focus);
            Vec3 side = Canvas.perpendicular(delta.normalize());
            Vec3 previous = focus;
            for (int i = 1; i <= 8; i++) {
                double f = i / 8.0;
                Vec3 point = focus.add(delta.scale(f)).add(side.scale(Math.sin(f * Math.PI) * 0.35 * Math.sin((now + partial) * 0.25 + f * 8)));
                Canvas.tube(pose, glow, previous, point, 0.02, 0xFFE2A0, 0.85F);
                previous = point;
            }
        }
    }

    /** SQUID magnetometer: thin brackets on hostile things through walls, only for the wearer. */
    private static void drawSquid(PoseStack pose, MultiBufferSource.BufferSource buffers, Minecraft mc, float partial) {
        if (!SuitEvents.wearing(mc.player, EquipmentSlot.HEAD, ArmorSet.MEISSNER)) return;
        VertexConsumer xray = buffers.getBuffer(ArsenalRenderTypes.XRAY);
        Vec3 eye = mc.player.getEyePosition(partial);
        for (LivingEntity target : mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(Rules.SQUID_RANGE),
                e -> e instanceof Enemy && e.isAlive())) {
            Vec3 center = target.getPosition(partial).add(0, target.getBbHeight() / 2, 0);
            double distance = center.distanceTo(eye);
            if (distance > Rules.SQUID_RANGE) continue;
            float alpha = (float) (0.75 * (1 - distance / Rules.SQUID_RANGE) + 0.2);
            Canvas.worldRing(pose, xray, center, center.subtract(eye).normalize(), Math.max(target.getBbHeight(), target.getBbWidth()) * 0.62, 0.05, 0x7CE8FF, alpha);
        }
        buffers.endBatch(ArsenalRenderTypes.XRAY);
    }
}
