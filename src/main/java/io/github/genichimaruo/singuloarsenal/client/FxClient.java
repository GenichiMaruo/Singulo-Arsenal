package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.client.art.ArsenalRenderTypes;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import io.github.genichimaruo.singuloarsenal.net.ScanPayload;
import io.github.genichimaruo.singuloarsenal.weapon.JetCannonItem;
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
        ScanPayload.setClientHandler(scan -> {
            var mc = Minecraft.getInstance();
            if (mc.level != null) {
                scanned = scan.ores();
                scannedUntil = mc.level.getGameTime() + Rules.SCAN_TICKS;
            }
        });
        NeoForge.EVENT_BUS.addListener(FxClient::tick);
        NeoForge.EVENT_BUS.addListener(FxClient::world);
        // Ahead of Singulo's own handler, which draws every lens registered so far and then forgets them.
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGHEST, FxClient::lenses);
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
            case FxPayload.MELTDOWN -> 26;
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
        FORMATIONS.clear();
        scanned = List.of();
        scene = next;
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (scene != mc.level) clear(mc.level);
        if (mc.level == null) return;
        long now = mc.level.getGameTime();
        ACTIVE.removeIf(fx -> now - fx.born > lifetime(fx.data.kind()));
        tickFormations(mc.level);
        STRETCHES.values().removeIf(s -> s.heldUntil + Rules.STRETCH_RELAX_TICKS < now && now - s.shearBorn > Rules.STRETCH_RELAX_TICKS);
    }

    // ---- spaghettification ---------------------------------------------------------------------------------------

    private static void stretchBegin(RenderLivingEvent.Pre<?, ?> event) {
        Stretch stretch = STRETCHES.get(event.getEntity().getId());
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        if (stretch == null || scene == null) return;
        float time = scene.getGameTime() + event.getPartialTick();
        // Both kinds of stretch let go gradually: a body does not snap back to its own shape.
        float fade = stretch.shearBorn == 0 ? 0 : Rules.relaxing(time - stretch.shearBorn, Rules.STRETCH_RELAX_TICKS);
        float stress = stretch.held * Rules.relaxing(time - stretch.heldUntil, Rules.STRETCH_RELAX_TICKS);
        float length = Rules.stretchFor(stress, fade * (0.6F + 0.1F * stretch.shear));
        float width = 1 / (float) Math.sqrt(length);
        pose.scale(width, length, width);
    }

    private static void stretchEnd(RenderLivingEvent.Post<?, ?> event) {
        event.getPoseStack().popPose();
    }

    // ---- gravitational lensing -----------------------------------------------------------------------------------------

    /** True while the world is being drawn: the paper doll in the inventory also draws held weapons, but not anywhere in the world. */
    private static boolean inLevel;

    private static void lenses(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) inLevel = true;
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        inLevel = false;
        var mc = Minecraft.getInstance();
        if (mc.level == null) return;
        float partial = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        for (Player player : mc.level.players()) {
            float use = focusOf(player, partial);
            if (use < 0) continue;
            float shadow = (4 + use * 5) / 16;
            Lensing.hole(TidalReaverItem.focus(player, partial), shadow, shadow * 4.5F);
        }
    }

    /** The Roche focus held by this player, from 0 to 1, or -1 if none is open. */
    private static float focusOf(Player player, float partial) {
        return TidalReaverItem.focusProgress(player.getMainHandItem(), player.level().getGameTime(), partial);
    }

    /** A small bend of the view round the hole sealed inside a weapon someone else is holding. The pose and point are the art's own, in model pixels. */
    static void heldHole(PoseStack pose, ItemDisplayContext context, float x, float y, float z, float pixels) {
        if (context.firstPerson() || !inLevel) return;
        Vector3f at = pose.last().pose().transformPosition(x, y, z, new Vector3f());
        float orb = pixels * pose.last().pose().getScale(new Vector3f()).y;
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        Lensing.distortion(camera.add(at.x, at.y, at.z), orb * 1.4F, 0.8F, orb * 4.3F);
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
            drawFx(pose, glow, fx.data, age, 1 - age / life, camera);
        }
        for (Player player : mc.level.players()) {
            float jet = JetCannonItem.jetAge(player.getMainHandItem(), now, partial);
            if (jet >= 0) drawImpact(pose, glow, player, partial, jet, camera);
            if (focusOf(player, partial) >= 0) drawThreads(pose, glow, mc.level, player, partial, now);
        }
        buffers.endBatch(ArsenalRenderTypes.GLOW);
        drawWorldJets(pose, buffers, mc, partial, camera);
        drawSquid(pose, buffers, mc, partial);
        drawScan(pose, buffers, mc, partial);
        pose.popPose();
        frame++;
    }

    private static void debris(PoseStack pose, VertexConsumer glow, Vec3 at, int count, double speed, float age, float fade, int rgb, double half) {
        java.util.Random random = new java.util.Random(Double.doubleToLongBits(at.x * 31 + at.y * 17 + at.z));
        for (int i = 0; i < count; i++) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            double pace = speed * (0.45 + 0.55 * random.nextDouble());
            Vec3 point = at.add(direction.scale(pace * age)).add(0, -0.012 * age * age, 0);
            Canvas.glowCube(pose, glow, point, half * (0.6 + 0.6 * random.nextDouble()) * fade, rgb, Math.min(1, fade * 1.6F));
        }
    }

    /** Effects are volumes: balls, fronts, bolts and debris that look the same from every side. */
    private static void drawFx(PoseStack pose, VertexConsumer glow, FxPayload fx, float age, float fade, Vec3 camera) {
        Vec3 at = fx.a();
        float strength = fade * fade;
        switch (fx.kind()) {
            case FxPayload.RIPPLE -> {
                int rgb = fx.value() >= 2 ? 0xFFB347 : 0x7CE8FF;
                Canvas.glowShell(pose, glow, at, 0.3 + age * 0.09, rgb, strength * 1.2F, camera);
                Canvas.glowBall(pose, glow, at, 0.16 + age * 0.03, 0xFFFFFF, strength * 0.5F, camera);
            }
            case FxPayload.COLLAPSE, FxPayload.SLAM -> {
                double radius = 0.4 + (fx.value() - 0.4) * (1 - Math.pow(fade, 2));
                Canvas.glowShell(pose, glow, at, radius, 0xFFB347, strength * 1.3F, camera);
                Canvas.glowBall(pose, glow, at, radius * 0.5, 0xFFF0C8, strength * 0.7F, camera);
                debris(pose, glow, at, 16, 0.3, age, fade, 0xFFC36B, 0.09);
            }
            case FxPayload.QUENCH -> {
                double radius = 0.3 + fx.value() * (1 - fade);
                Canvas.glowShell(pose, glow, at, radius, 0x7CE8FF, strength * 1.2F, camera);
                Canvas.glowBall(pose, glow, at, 0.9 * fade, 0xE8FBFF, strength, camera);
                arcs(pose, glow, at, radius, (int) age / 2, strength);
            }
            case FxPayload.HORIZON -> {
                double radius = fx.value() * (1 - fade * fade);
                Canvas.glowShell(pose, glow, at, radius, 0xFFD28A, strength * 1.5F, camera);
                Canvas.glowShell(pose, glow, at, radius * 0.6, 0xFFFFFF, fade * 0.8F, camera);
                debris(pose, glow, at, 24, 0.4, age, fade, 0xFFE2A0, 0.1);
            }
            case FxPayload.JET_BURST -> Canvas.glowShell(pose, glow, at, 0.5 + age * 0.3, 0xBFE6FF, strength * 1.2F, camera);
            case FxPayload.BIT_SHOT -> {
                if (Minecraft.getInstance().level.getEntity(fx.entity()) instanceof Player owner) {
                    int count = bitCount(owner);
                    if (count > 0) {
                        Vec3 from = bitPosition(owner, (int) fx.value(), count, Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(true));
                        Canvas.tube(pose, glow, from, at, 0.05 * fade, 0x7CE8FF, strength * 0.6F);
                        Canvas.tube(pose, glow, from, at, 0.02 * fade, 0xFFFFFF, strength);
                        Canvas.glowBall(pose, glow, at, 0.14 + age * 0.05, 0x7CE8FF, strength, camera);
                    }
                }
            }
            case FxPayload.MELTDOWN -> meltdown(pose, glow, at, fx.value(), age, fade, camera);
            case FxPayload.RUPTURE -> {
                Canvas.glowShell(pose, glow, at, 0.2 + age * 0.14, 0xB8A8FF, strength * 1.2F, camera);
                Canvas.tube(pose, glow, at.subtract(fx.b().scale(1.5)), at.add(fx.b().scale(1.5)), 0.05, 0xFFFFFF, strength);
            }
            case FxPayload.SHEAR -> {
                // Everything is drawn in to the focus, then the focus lets go.
                double front = fx.value() * fade * fade;
                Canvas.glowShell(pose, glow, at, front, 0xFFE2A0, strength * 1.3F, camera);
                java.util.Random random = new java.util.Random(Double.doubleToLongBits(at.x + at.z));
                for (int i = 0; i < 18; i++) {
                    Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
                    Canvas.tube(pose, glow, at.add(direction.scale(front)), at.add(direction.scale(front * 0.45)), 0.05 * fade, 0xFFF0C8, strength);
                }
                Canvas.glowBall(pose, glow, at, 0.3 + 1.6 * (1 - fade), 0xFFFFFF, (1 - fade) * fade * 2.2F, camera);
            }
            default -> {}
        }
    }

    private static void arcs(PoseStack pose, VertexConsumer glow, Vec3 at, double radius, int frame, float strength) {
        java.util.Random random = new java.util.Random(frame * 7919L + Double.doubleToLongBits(at.x));
        for (int arc = 0; arc < 8; arc++) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            Vec3 previous = at;
            for (int step = 1; step <= 5; step++) {
                Vec3 jitter = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).scale(0.12 * radius);
                Vec3 next = at.add(direction.scale(radius * step / 5)).add(step == 5 ? Vec3.ZERO : jitter);
                Canvas.tube(pose, glow, previous, next, 0.035, 0xCFF4FF, strength);
                previous = next;
            }
        }
    }

    /** A real fireball: a white core, billowing lobes of orange that cool as they spread, a pressure front, and thrown debris. */
    private static void meltdown(PoseStack pose, VertexConsumer glow, Vec3 at, float radius, float age, float fade, Vec3 camera) {
        float grown = 1 - fade * fade * fade;
        // Additive light piles up fast, so each part is kept dim: the colour comes from the lobes, and only the core reaches white.
        Canvas.glowShell(pose, glow, at, radius * 1.25 * grown, 0xFF9A5A, fade * fade * 0.9F, camera);
        Canvas.glowBall(pose, glow, at, radius * 0.4 * grown, 0xFFF4DC, fade * 0.8F, camera);
        java.util.Random random = new java.util.Random(Double.doubleToLongBits(at.x * 3 + at.y * 5 + at.z * 7));
        for (int lobe = 0; lobe < 16; lobe++) {
            Vec3 direction = new Vec3(random.nextGaussian(), random.nextGaussian(), random.nextGaussian()).normalize();
            double out = radius * (0.35 + 0.45 * random.nextDouble()) * grown;
            double size = radius * (0.28 + 0.2 * random.nextDouble()) * (0.5 + grown);
            int rgb = fade > 0.55F ? 0xFF8A3C : 0xC8461A;
            Canvas.glowBall(pose, glow, at.add(direction.scale(out)), size, rgb, fade * 0.3F, camera);
        }
        debris(pose, glow, at, 40, radius * 0.07, age, fade, 0xFFB061, 0.16);
    }

    private static boolean firstPerson(Player player) {
        return player == Minecraft.getInstance().player && Minecraft.getInstance().options.getCameraType().isFirstPerson();
    }

    /** Where the jet lands: the same eye-ray the server damages along, so the beam always meets what it is hitting. */
    static net.minecraft.world.phys.BlockHitResult aim(Player player, float partial) {
        Vec3 eye = player.getEyePosition(partial);
        return player.level().clip(new ClipContext(eye, eye.add(player.getViewVector(partial).scale(Rules.CANNON_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }

    /**
     * Turns the cannon on its grip so the barrel points exactly at what the player is aiming at. The beam then leaves the muzzle
     * along the barrel's own axis, instead of merely starting at the muzzle. Run before anything else moves the art.
     */
    static void aimCannon(Player owner, PoseStack pose, ItemDisplayContext context, float partial) {
        // A shadow pass uses the light camera, not the main view used by this aim conversion.
        if (ShaderCompat.shadowPass()) return;
        var mc = Minecraft.getInstance();
        // The inventory's paper doll is drawn in GUI space, where world directions mean nothing.
        if (!context.firstPerson() && mc.screen instanceof net.minecraft.client.gui.screens.inventory.EffectRenderingInventoryScreen) return;
        Vec3 eye = owner.getEyePosition(partial);
        Vec3 look = owner.getViewVector(partial);
        boolean firing = JetCannonItem.jetAge(owner.getMainHandItem(), owner.level().getGameTime(), partial) >= 0;
        // Never closer than a few blocks, or a wall at arm's length would wrench the gun sideways.
        double distance = firing ? Math.max(3, eye.distanceTo(aim(owner, partial).getLocation())) : 40;
        Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
        Vec3 target = eye.add(look.scale(distance)).subtract(camera);
        Matrix4f matrix = pose.last().pose();
        Vector3f origin = matrix.transformPosition(0, 0, 0, new Vector3f());
        Vector3f direction = RenderSpace.worldToPose().transformPosition(
                (float) target.x, (float) target.y, (float) target.z, new Vector3f()).sub(origin).normalize();
        new Matrix4f(matrix).invert().transformDirection(direction).normalize();
        pose.mulPose(new Quaternionf().rotationTo(0, 1, 0, direction.x, direction.y, direction.z));
    }

    /**
     * Called from the item renderer with the pose of the cannon actually on screen. In third person the muzzle is remembered for the
     * world pass. First-person hands are drawn later with their own projection and a cleared depth buffer, so the jet is drawn here.
     */
    static void cannonRendered(Player owner, PoseStack pose, MultiBufferSource buffers, ItemDisplayContext context, float partial, float age) {
        // Never let a light-space muzzle overwrite the world-pass cache.
        if (ShaderCompat.shadowPass()) return;
        Vector3f tip = new Matrix4f(pose.last().pose()).transformPosition(0, (float) WeaponArt.CANNON_MUZZLE, 0, new Vector3f());
        var mc = Minecraft.getInstance();
        if (!context.firstPerson()) {
            Vec3 camera = mc.gameRenderer.getMainCamera().getPosition();
            RenderSpace.worldToPose().invert().transformPosition(tip);
            MUZZLES.put(owner.getId(), new Muzzle(camera.add(tip.x, tip.y, tip.z), frame));
            return;
        }
        if (age < 0) return;
        // Draw the entire beam in camera-relative world space, then use the same
        // conversion as the muzzle/aim. Shader passes need not use vanilla's basis.
        Matrix4f worldToPose = RenderSpace.worldToPose();
        new Matrix4f(worldToPose).invert().transformPosition(tip);
        PoseStack beam = new PoseStack();
        beam.mulPose(worldToPose);
        Vec3 target = aim(owner, partial).getLocation().subtract(mc.gameRenderer.getMainCamera().getPosition());
        drawJet(beam, buffers, Vec3.ZERO, new Vec3(tip.x, tip.y, tip.z), target, age, 0.55F);
    }

    /** One jet: at full length from its first tick, flashing wide as it lights and pinching off at the end. */
    private static void drawJet(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 from, Vec3 to, float age, float scale) {
        float radius = Rules.cannonRadius(age) * scale * (1 + 0.1F * (float) Math.sin(age * 1.3));
        JetTube.draw(pose, buffers, camera, from, to, radius, Rules.cannonFocus(age), 1, 1.1F);
    }

    /** Jets of everyone drawn in the world. The local first-person jet is drawn with the hand instead. */
    private static void drawWorldJets(PoseStack pose, MultiBufferSource.BufferSource buffers, Minecraft mc, float partial, Vec3 camera) {
        for (Player player : mc.level.players()) {
            float age = JetCannonItem.jetAge(player.getMainHandItem(), mc.level.getGameTime(), partial);
            if (age < 0 || (firstPerson(player) && !mc.options.hideGui)) continue;
            Muzzle known = MUZZLES.get(player.getId());
            Vec3 from;
            if (known != null && known.frame == frame) {
                from = known.position;
            } else {
                Vec3 look = player.getViewVector(partial);
                from = player.getEyePosition(partial).add(look.scale(1.5)).add(0, -0.4, 0);
            }
            drawJet(pose, buffers, camera, from, aim(player, partial).getLocation(), age, 0.85F);
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

    private static void drawImpact(PoseStack pose, VertexConsumer glow, Player player, float partial, float held, Vec3 camera) {
        var hit = aim(player, partial);
        if (hit.getType() != HitResult.Type.BLOCK) return;
        Vec3 at = hit.getLocation();
        float pulse = 0.85F + 0.15F * (float) Math.sin(held * 1.3);
        float tail = Math.min(1, (Rules.CANNON_SHOT_TICKS - held) / 8);
        Canvas.glowBall(pose, glow, at, 0.9 * pulse, 0x8CD2FF, 0.8F * tail, camera);
        Canvas.glowBall(pose, glow, at, 0.45 * pulse, 0xFFFFFF, tail, camera);
        // Sparks restart a few times a second, thrown off the surface.
        float cycle = held % 6;
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        debris(pose, glow, at.add(normal.scale(0.2)).add(normal.scale(0.01 * (int) (held / 6))), 8, 0.14, cycle, tail * (1 - cycle / 6), 0xFFE2A0, 0.05);
    }

    private static int bitCount(Player player) {
        return Modules.count(player.getItemBySlot(EquipmentSlot.CHEST), ModuleType.BIT_DRONE) * Rules.BITS_PER_MODULE;
    }

    /** Where a player's bits are: each eases toward its place in the fan, and the fan turns a little behind the body. Client-side only. */
    private static final class Formation {
        float yaw;
        final Vec3[] now, before;

        Formation(int count, float yaw) {
            this.yaw = yaw;
            now = new Vec3[count];
            before = new Vec3[count];
        }
    }

    private static final Map<Integer, Formation> FORMATIONS = new HashMap<>();

    /** A bit's place: a fan behind and above the wearer, the middle highest, each drifting on its own slow cycle, as a Singulo android's bits do. */
    private static Vec3 bitGoal(Player player, int index, int count, float yaw, float age) {
        double spread = count <= 1 ? 0 : (index - (count - 1) / 2.0) * (Rules.BIT_FAN_DEGREES / (count - 1));
        double angle = Math.toRadians(yaw + 180 + spread);
        double radius = 1.15 + 0.15 * Math.sin(age * 0.07 + index);
        double height = player.getEyeHeight() + 0.13 + 0.3 * Math.cos(Math.toRadians(spread) * 1.4) + 0.12 * Math.sin(age * 0.11 + index * 1.7);
        return new Vec3(-Math.sin(angle) * radius, height, Math.cos(angle) * radius);
    }

    private static void tickFormations(ClientLevel level) {
        FORMATIONS.keySet().removeIf(id -> !(level.getEntity(id) instanceof Player player) || bitCount(player) == 0);
        for (Player player : level.players()) {
            int count = bitCount(player);
            if (count == 0) continue;
            Formation formation = FORMATIONS.get(player.getId());
            if (formation == null || formation.now.length != count) {
                formation = new Formation(count, player.yBodyRot);
                FORMATIONS.put(player.getId(), formation);
            }
            formation.yaw = net.minecraft.util.Mth.rotLerp(0.14F, formation.yaw, player.yBodyRot);
            for (int i = 0; i < count; i++) {
                Vec3 goal = player.position().add(bitGoal(player, i, count, formation.yaw, player.tickCount));
                // Far from its place means a teleport: arrive there, rather than streak across the world.
                if (formation.now[i] == null || formation.now[i].distanceToSqr(player.position()) > 64) formation.now[i] = goal;
                formation.before[i] = formation.now[i];
                formation.now[i] = formation.now[i].lerp(goal, 0.24);
            }
        }
    }

    private static Vec3 bitPosition(Player player, int index, int count, float partial) {
        Formation formation = FORMATIONS.get(player.getId());
        if (formation != null && index < formation.now.length && formation.now[index] != null) return formation.before[index].lerp(formation.now[index], partial);
        return player.getPosition(partial).add(bitGoal(player, index, count, player.getPreciseBodyRotation(partial), player.tickCount + partial));
    }

    private static void drawBits(PoseStack pose, MultiBufferSource.BufferSource buffers, Minecraft mc, float partial) {
        boolean any = false;
        float t = mc.level.getGameTime() + partial;
        for (Player player : mc.level.players()) {
            int count = bitCount(player);
            // A wearer's own bits stay out of their view altogether: a fast turn would otherwise swing the fan across it.
            if (count == 0 || player.isInvisible() || firstPerson(player)) continue;
            Formation formation = FORMATIONS.get(player.getId());
            float yaw = formation != null ? formation.yaw : player.getPreciseBodyRotation(partial);
            any = true;
            for (int i = 0; i < count; i++) {
                Vec3 at = bitPosition(player, i, count, partial);
                pose.pushPose();
                pose.translate(at.x, at.y, at.z);
                // Nose along the wearer's facing, raised a little. The hull's front is -Z.
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180 - yaw));
                pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(25));
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
        for (Player player : mc.level.players()) any |= focusOf(player, partial) >= 0;
        if (!any) return;
        for (Player player : mc.level.players()) {
            float use = focusOf(player, partial);
            if (use < 0) continue;
            Vec3 focus = TidalReaverItem.focus(player, partial);
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

    private static List<net.minecraft.core.BlockPos> scanned = List.of();
    private static long scannedUntil;

    /** What the SQUID pick found: a block of light on each ore, seen through the rock, fading as the reading goes stale. */
    private static void drawScan(PoseStack pose, MultiBufferSource.BufferSource buffers, Minecraft mc, float partial) {
        float left = scannedUntil - mc.level.getGameTime() - partial;
        if (left <= 0 || scanned.isEmpty()) return;
        float alpha = 0.1F * Math.min(1, left / 40);
        VertexConsumer xray = buffers.getBuffer(ArsenalRenderTypes.XRAY);
        for (var pos : scanned) {
            if (!mc.level.getBlockState(pos).isAir()) Canvas.glowCube(pose, xray, Vec3.atCenterOf(pos), 0.5, 0x7CE8FF, alpha);
        }
        buffers.endBatch(ArsenalRenderTypes.XRAY);
    }

    /** SQUID magnetometer: thin brackets on hostile things through walls, only for the wearer. */
    /** Twelve thin edges round a body: a marker with depth, unlike a flat circle. */
    private static void bracket(PoseStack pose, VertexConsumer out, AABB box, float alpha) {
        double[] xs = {box.minX, box.maxX}, ys = {box.minY, box.maxY}, zs = {box.minZ, box.maxZ};
        for (double y : ys) for (double z : zs) Canvas.tube(pose, out, new Vec3(xs[0], y, z), new Vec3(xs[1], y, z), 0.018, 0x7CE8FF, alpha);
        for (double x : xs) for (double z : zs) Canvas.tube(pose, out, new Vec3(x, ys[0], z), new Vec3(x, ys[1], z), 0.018, 0x7CE8FF, alpha);
        for (double x : xs) for (double y : ys) Canvas.tube(pose, out, new Vec3(x, y, zs[0]), new Vec3(x, y, zs[1]), 0.018, 0x7CE8FF, alpha);
    }

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
            bracket(pose, xray, target.getBoundingBox().move(target.getPosition(partial).subtract(target.position())).inflate(0.1), alpha);
        }
        buffers.endBatch(ArsenalRenderTypes.XRAY);
    }
}
