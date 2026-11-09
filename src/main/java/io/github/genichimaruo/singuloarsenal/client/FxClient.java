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
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Everything drawn in the world rather than on a body: impact rings, jets, the tidal focus, the SQUID overlay, spaghettification. */
final class FxClient {
    private record Fx(FxPayload data, long born) {}
    private record Stretch(float held, long heldUntil, long shearBorn, float shear) {}

    private static final List<Fx> ACTIVE = new ArrayList<>();
    private static final Map<Integer, Stretch> STRETCHES = new HashMap<>();
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
        VertexConsumer glow = buffers.getBuffer(ArsenalRenderTypes.GLOW);
        long now = mc.level.getGameTime();
        for (Fx fx : ACTIVE) {
            float age = now - fx.born + partial;
            float life = lifetime(fx.data.kind());
            if (age < 0 || age > life) continue;
            drawFx(pose, glow, fx.data, age, 1 - age / life);
        }
        for (Player player : mc.level.players()) {
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof JetLanceItem) drawJets(pose, glow, player, partial);
            if (player.isUsingItem() && player.getUseItem().getItem() instanceof TidalReaverItem) drawThreads(pose, glow, mc.level, player, partial, now);
        }
        buffers.endBatch(ArsenalRenderTypes.GLOW);
        drawSquid(pose, buffers, mc, partial);
        pose.popPose();
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
                Vec3 end = at.lerp(fx.b(), Math.min(1, age / 3));
                Canvas.tube(pose, glow, at, end, 0.55 * fade, 0xFF9A4A, strength * 0.45F);
                Canvas.tube(pose, glow, at, end, 0.26 * fade, 0xFFD28A, strength * 0.7F);
                Canvas.tube(pose, glow, at, end, 0.1 * fade, 0xFFF8EA, strength);
                Vec3 thrust = at.subtract(fx.b()).normalize();
                Canvas.worldRing(pose, glow, at, thrust, 0.5 + age * 0.3, 0.2, 0xFFE2A0, strength);
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

    private static Vec3 muzzle(Player player, float partial) {
        Vec3 look = player.getViewVector(partial);
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        boolean firstPerson = firstPerson(player);
        // In first person a beam starting at the eye would fill the screen, so it starts where the lance's tip is seen.
        return player.getEyePosition(partial).add(look.scale(firstPerson ? 2.4 : 0.5)).add(side.scale(firstPerson ? 0.34 : 0.15)).add(0, firstPerson ? -0.26 : -0.3, 0);
    }

    private static boolean firstPerson(Player player) {
        return player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    private static void drawJets(PoseStack pose, VertexConsumer glow, Player player, float partial) {
        float held = JetLanceItem.held(player.getUseItem(), player) + partial;
        Vec3 look = player.getViewVector(partial).normalize();
        Vec3 from = muzzle(player, partial);
        var level = player.level();
        Vec3 frontEnd = level.clip(new ClipContext(from, from.add(look.scale(Rules.LANCE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        var rearHit = level.clip(new ClipContext(from, from.subtract(look.scale(Rules.LANCE_REAR_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 rearEnd = rearHit.getLocation();
        float scale = firstPerson(player) ? 0.34F : 0.7F;
        float radius = Rules.lanceRadius((int) held) * scale;
        if (held < Rules.LANCE_IGNITE) {
            float grow = held / Rules.LANCE_IGNITE;
            Canvas.tube(pose, glow, from, from.add(look.scale(grow * 2)), radius * grow, 0xFF9A4A, 0.5F);
            Canvas.tube(pose, glow, from, from.subtract(look.scale(grow * 1.4)), radius * grow, 0xFF9A4A, 0.4F);
            return;
        }
        float pulse = 0.85F + 0.15F * (float) Math.sin(held * 1.3);
        float collimating = held < Rules.LANCE_COLLIMATE ? 1 : 0;
        for (var pair : new Vec3[][]{{from, frontEnd}, {from, rearEnd}}) {
            float flare = 1 + 0.6F * collimating;
            Canvas.tube(pose, glow, pair[0], pair[1], radius * 1.9 * flare, 0xFF7A2E, 0.1F * pulse);
            Canvas.tube(pose, glow, pair[0], pair[1], radius * 1.3 * flare, 0xFFB45E, 0.42F * pulse);
            Canvas.tube(pose, glow, pair[0], pair[1], radius * 0.5, 0xFFFBF0, pulse);
        }
        // Three rings ahead of the muzzle close down in turn as the beam is collimated.
        int rings = held < Rules.LANCE_IGNITE + 7 ? 1 : held < Rules.LANCE_IGNITE + 14 ? 2 : 3;
        for (int i = 0; i < rings; i++) {
            double distance = 0.7 + i * 0.55;
            double ring = (3 - i) * 0.17 * scale * (1 + 0.1 * Math.sin(held * 0.7 + i));
            Canvas.worldRing(pose, glow, from.add(look.scale(distance)), look, ring, 0.05, 0xFFE2A0, 0.85F);
            Canvas.worldRing(pose, glow, from.subtract(look.scale(distance)), look, ring, 0.05, 0xFFE2A0, 0.7F);
        }
        if (held >= Rules.LANCE_COLLIMATE && frontEnd.distanceToSqr(from) < Rules.LANCE_RANGE * Rules.LANCE_RANGE - 1) {
            Canvas.worldRing(pose, glow, frontEnd.subtract(look.scale(0.08)), look, 0.55 * pulse, 0.25, 0xFFD28A, 0.8F);
        }
        if (held >= Rules.LANCE_COLLIMATE && rearHit.getType() == HitResult.Type.BLOCK) {
            Canvas.worldRing(pose, glow, rearEnd.add(look.scale(0.08)), look, 0.4 * pulse, 0.2, 0xFFD28A, 0.7F);
        }
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
                c.sphere(0, 0, 0, 4 + use * 5, 0x000000);
                c.litHoop(0, 0, 0, 1, 5 + use * 6, 0.25, 0xFFF8EA);
                c.halo(0, 0, 0, 1, 7 + use * 8, 3, 0xFFD28A, 0.6F);
                c.halo(0, 0, 0, 1, 13 + use * 10, 4, 0xFF9A4A, 0.35F);
            });
            pose.popPose();
        }
        buffers.endBatch(RenderType.entityCutoutNoCull(Canvas.WHITE));
        buffers.endBatch(RenderType.entityTranslucentEmissive(Canvas.WHITE));
    }

    private static void drawThreads(PoseStack pose, VertexConsumer glow, ClientLevel level, Player player, float partial, long now) {
        Vec3 focus = TidalReaverItem.focus(player, partial);
        double range = Rules.REAVER_RANGE;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(focus, focus).inflate(range), e -> e != player && e.isAlive() && !(e instanceof ArmorStand))) {
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
