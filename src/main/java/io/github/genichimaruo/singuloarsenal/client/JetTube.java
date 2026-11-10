package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import io.github.genichimaruo.singuloarsenal.client.art.ArsenalRenderTypes;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import java.io.IOException;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RegisterShadersEvent;

/**
 * The same jet as the Accretion Jet Forge: Singulo's own {@code jet_beam} shader, on three nested tubes (halo, plasma, core) with the
 * Forge's widths and colours. Each vertex carries how far across the tube it is as seen from the camera, so it looks round from any side.
 */
final class JetTube extends RenderType {
    private static ShaderInstance shader;
    private static final RenderType BEAM = create("singulo_arsenal_jet", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS,
            16384, false, false, CompositeState.builder()
                    .setShaderState(new ShaderStateShard(() -> shader))
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL)
                    .createCompositeState(false));
    private static final int SIDES = 12;

    private JetTube() {
        super("unused", DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
    }

    static void registerShaders(RegisterShadersEvent event) {
        try {
            // In 1.20.1 ShaderInstance.apply() overrides RenderType's blend state.
            // Our descriptor reuses Singulo's programs and explicitly enables additive blending.
            event.registerShader(new ShaderInstance(event.getResourceProvider(), new ResourceLocation("singulo_arsenal", "jet_beam"),
                    DefaultVertexFormat.POSITION_TEX_COLOR), s -> shader = s);
        } catch (IOException e) {
            LogUtils.getLogger().error("Could not load Singulo's jet shader; jets fall back to plain tubes", e);
        }
    }

    static RenderType type() {
        return shader == null ? ArsenalRenderTypes.GLOW : BEAM;
    }

    /**
     * @param camera   camera position in the pose's space
     * @param radius   full radius of the jet
     * @param focus    0 for a broad turbulent jet, 1 for a tight white beam
     * @param flareIn  blocks over which the jet widens from a narrow start, so it visibly comes out of an aperture
     */
    static void draw(PoseStack pose, MultiBufferSource buffers, Vec3 camera, Vec3 from, Vec3 to, float radius, float focus, float alpha, float flareIn) {
        double length = from.distanceTo(to);
        if (length < 1.0E-3 || radius <= 0 || alpha <= 0) return;
        if (shader == null) {
            VertexConsumer out = buffers.getBuffer(ArsenalRenderTypes.GLOW);
            Canvas.tube(pose, out, from, to, radius, 0x8CD2FF, alpha * 0.4F);
            Canvas.tube(pose, out, from, to, radius * 0.4, 0xFFF8F0, alpha);
            return;
        }
        VertexConsumer out = buffers.getBuffer(BEAM);
        int segments = Mth.clamp((int) (length * 2), 6, 48);
        for (int layer = 0; layer < 3; layer++) {
            int style = layer == 0 ? 2 : layer == 1 ? 0 : 1;
            float width = switch (layer) {
                case 0 -> radius * (2.4F - 0.6F * focus);
                case 1 -> radius * (1.05F - 0.25F * focus);
                default -> radius * (0.28F + 0.22F * focus);
            };
            int color = switch (layer) {
                case 0 -> lerp(0x6A70FF, 0x8CD2FF, focus);
                case 1 -> lerp(0x7EA0FF, 0xBFE6FF, focus);
                default -> lerp(0xFFC48C, 0xFFF8F0, focus);
            };
            float strength = alpha * switch (layer) {
                case 0 -> 0.3F + 0.25F * focus;
                case 1 -> 0.85F - 0.45F * focus;
                default -> 0.35F + 0.85F * focus;
            };
            tube(pose, out, camera, from, to, length, segments, width, strength, color, style, flareIn);
        }
    }

    private static void tube(PoseStack pose, VertexConsumer out, Vec3 camera, Vec3 from, Vec3 to, double length, int segments, float width, float strength,
                             int color, int style, float flareIn) {
        Vec3 axis = to.subtract(from).normalize();
        Vec3 e1 = Canvas.perpendicular(axis), e2 = axis.cross(e1).normalize();
        Vec3 side = axis.cross(from.lerp(to, 0.5).subtract(camera));
        side = side.lengthSqr() < 1.0E-8 ? e1 : side.normalize();
        float base = 4F * style;
        for (int i = 0; i < segments; i++) {
            double s0 = length * i / segments, s1 = length * (i + 1) / segments;
            double w0 = width * flare(s0, flareIn), w1 = width * flare(s1, flareIn);
            float a0 = strength * 0.55F * fade(s0, length), a1 = strength * 0.55F * fade(s1, length);
            Vec3 c0 = from.add(axis.scale(s0)), c1 = from.add(axis.scale(s1));
            for (int j = 0; j < SIDES; j++) {
                double t0 = j * Math.PI * 2 / SIDES, t1 = (j + 1) * Math.PI * 2 / SIDES;
                Vec3 r0 = e1.scale(Math.cos(t0)).add(e2.scale(Math.sin(t0))), r1 = e1.scale(Math.cos(t1)).add(e2.scale(Math.sin(t1)));
                vertex(pose, out, c0.add(r0.scale(w0)), (float) s0, base + (float) r0.dot(side), color, a0);
                vertex(pose, out, c0.add(r1.scale(w0)), (float) s0, base + (float) r1.dot(side), color, a0);
                vertex(pose, out, c1.add(r1.scale(w1)), (float) s1, base + (float) r1.dot(side), color, a1);
                vertex(pose, out, c1.add(r0.scale(w1)), (float) s1, base + (float) r0.dot(side), color, a1);
            }
        }
    }

    private static double flare(double s, float flareIn) {
        if (flareIn <= 0) return 1;
        double k = Math.min(1, s / flareIn);
        return 0.3 + 0.7 * k * (2 - k);
    }

    private static float fade(double s, double length) {
        return (float) Math.min(1, (length - s) / Math.min(1.5, length * 0.3) + 0.15);
    }

    private static void vertex(PoseStack pose, VertexConsumer out, Vec3 p, float u, float v, int rgb, float alpha) {
        out.vertex(pose.last().pose(), (float) p.x, (float) p.y, (float) p.z).uv(u, v)
                .color((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Mth.clamp((int) (alpha * 255), 0, 255)).endVertex();
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) Mth.lerp(t, (a >> 16) & 255, (b >> 16) & 255);
        int g = (int) Mth.lerp(t, (a >> 8) & 255, (b >> 8) & 255);
        int bl = (int) Mth.lerp(t, a & 255, b & 255);
        return (r << 16) | (g << 8) | bl;
    }
}
