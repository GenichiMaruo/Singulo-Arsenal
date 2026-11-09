package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.function.Consumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Draws everything the mod shows: plain coloured geometry, in three passes. A piece of art is one function that describes
 * solid plates, self-lit strips and additive halos together; {@link #draw} runs it once per pass, and each call only emits
 * in its own pass. That keeps art readable and never interleaves render types, which would invalidate the shared buffer.
 */
public final class Canvas {
    public static final ResourceLocation WHITE = SinguloArsenal.id("textures/misc/white.png");
    private static final RenderType SOLID = RenderType.entityCutoutNoCull(WHITE);
    private static final RenderType LIT = RenderType.entityTranslucentEmissive(WHITE);
    private static final int SOLID_PASS = 0, LIT_PASS = 1, GLOW_PASS = 2;

    public final PoseStack pose;
    /** Game time in ticks, for animation. */
    public final float t;
    private final int light;
    private final int overlay;
    private int pass;
    private VertexConsumer out;

    private Canvas(PoseStack pose, float t, int light, int overlay) {
        this.pose = pose;
        this.t = t;
        this.light = light;
        this.overlay = overlay;
    }

    public static void draw(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, Consumer<Canvas> art) {
        Canvas canvas = new Canvas(pose, t, light, overlay);
        canvas.pass = SOLID_PASS;
        canvas.out = buffers.getBuffer(SOLID);
        art.accept(canvas);
        canvas.pass = LIT_PASS;
        canvas.out = buffers.getBuffer(LIT);
        art.accept(canvas);
        canvas.pass = GLOW_PASS;
        canvas.out = buffers.getBuffer(ArsenalRenderTypes.GLOW);
        art.accept(canvas);
    }

    // ---- boxes ---------------------------------------------------------------------------------------------------

    /** A solid plate. Corners may be given in any order. */
    public void box(double x0, double y0, double z0, double x1, double y1, double z1, int rgb) {
        if (pass == SOLID_PASS) cuboid(x0, y0, z0, x1, y1, z1, rgb, light);
    }

    /** A self-lit strip: same geometry as a plate, ignoring world light. */
    public void lit(double x0, double y0, double z0, double x1, double y1, double z1, int rgb) {
        if (pass == LIT_PASS) cuboid(x0, y0, z0, x1, y1, z1, rgb, LightTexture.FULL_BRIGHT);
    }

    private void cuboid(double ax, double ay, double az, double bx, double by, double bz, int rgb, int lightmap) {
        float x0 = (float) Math.min(ax, bx), x1 = (float) Math.max(ax, bx);
        float y0 = (float) Math.min(ay, by), y1 = (float) Math.max(ay, by);
        float z0 = (float) Math.min(az, bz), z1 = (float) Math.max(az, bz);
        face(x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, 0, 0, -1, rgb, lightmap);
        face(x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, 0, 0, 1, rgb, lightmap);
        face(x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, -1, 0, 0, rgb, lightmap);
        face(x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, 1, 0, 0, rgb, lightmap);
        face(x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, 0, 1, 0, rgb, lightmap);
        face(x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1, 0, -1, 0, rgb, lightmap);
    }

    private void face(float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz,
                      float nx, float ny, float nz, int rgb, int lightmap) {
        vertex(ax, ay, az, 0, 0, nx, ny, nz, rgb, lightmap);
        vertex(bx, by, bz, 0, 1, nx, ny, nz, rgb, lightmap);
        vertex(cx, cy, cz, 1, 1, nx, ny, nz, rgb, lightmap);
        vertex(dx, dy, dz, 1, 0, nx, ny, nz, rgb, lightmap);
    }

    private void vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, int rgb, int lightmap) {
        out.addVertex(pose.last().pose(), x, y, z).setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, 255)
                .setUv(u, v).setOverlay(overlay).setLight(lightmap).setNormal(pose.last(), nx, ny, nz);
    }

    // ---- round things --------------------------------------------------------------------------------------------

    /** A solid hoop with a square section. Axis: 0 = X, 1 = Y, 2 = Z. */
    public void hoop(double cx, double cy, double cz, int axis, double radius, double half, int rgb) {
        if (pass == SOLID_PASS) torus(cx, cy, cz, axis, radius, half, rgb, light);
    }

    public void litHoop(double cx, double cy, double cz, int axis, double radius, double half, int rgb) {
        if (pass == LIT_PASS) torus(cx, cy, cz, axis, radius, half, rgb, LightTexture.FULL_BRIGHT);
    }

    private void torus(double cx, double cy, double cz, int axis, double radius, double half, int rgb, int lightmap) {
        int segments = 28;
        double in = radius - half, out2 = radius + half;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            quadOnRing(cx, cy, cz, axis, a, b, in, -half, in, half, rgb, lightmap, -1);
            quadOnRing(cx, cy, cz, axis, a, b, out2, half, out2, -half, rgb, lightmap, 1);
            quadOnRing(cx, cy, cz, axis, a, b, in, half, out2, half, rgb, lightmap, 0);
            quadOnRing(cx, cy, cz, axis, a, b, out2, -half, in, -half, rgb, lightmap, 0);
        }
    }

    private void quadOnRing(double cx, double cy, double cz, int axis, double a, double b, double r0, double h0, double r1, double h1, int rgb, int lightmap, int radialNormal) {
        float[] p0 = onRing(cx, cy, cz, axis, a, r0, h0), p1 = onRing(cx, cy, cz, axis, b, r0, h0);
        float[] p2 = onRing(cx, cy, cz, axis, b, r1, h1), p3 = onRing(cx, cy, cz, axis, a, r1, h1);
        float[] n = radialNormal != 0
                ? onRing(0, 0, 0, axis, (a + b) / 2, radialNormal, 0)
                : onRing(0, 0, 0, axis, 0, 0, Math.signum(h0));
        vertex(p0[0], p0[1], p0[2], 0, 0, n[0], n[1], n[2], rgb, lightmap);
        vertex(p1[0], p1[1], p1[2], 1, 0, n[0], n[1], n[2], rgb, lightmap);
        vertex(p2[0], p2[1], p2[2], 1, 1, n[0], n[1], n[2], rgb, lightmap);
        vertex(p3[0], p3[1], p3[2], 0, 1, n[0], n[1], n[2], rgb, lightmap);
    }

    private static float[] onRing(double cx, double cy, double cz, int axis, double angle, double radius, double along) {
        double u = Math.cos(angle) * radius, v = Math.sin(angle) * radius;
        return switch (axis) {
            case 0 -> new float[]{(float) (cx + along), (float) (cy + u), (float) (cz + v)};
            case 1 -> new float[]{(float) (cx + u), (float) (cy + along), (float) (cz + v)};
            default -> new float[]{(float) (cx + u), (float) (cy + v), (float) (cz + along)};
        };
    }

    /** A soft additive ring: brightest at {@code radius}, fading to nothing over {@code width} each way. */
    public void halo(double cx, double cy, double cz, int axis, double radius, double width, int rgb, float alpha) {
        if (pass != GLOW_PASS || alpha <= 0) return;
        haloRing(pose, out, cx, cy, cz, axis, radius, width, rgb, alpha);
    }

    public static void haloRing(PoseStack pose, VertexConsumer out, double cx, double cy, double cz, int axis, double radius, double width, int rgb, float alpha) {
        int segments = 48;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            for (int side = -1; side <= 1; side += 2) {
                double outer = Math.max(0, radius + side * width);
                glow(pose, out, onRing(cx, cy, cz, axis, a, radius, 0), rgb, alpha);
                glow(pose, out, onRing(cx, cy, cz, axis, b, radius, 0), rgb, alpha);
                glow(pose, out, onRing(cx, cy, cz, axis, b, outer, 0), rgb, 0);
                glow(pose, out, onRing(cx, cy, cz, axis, a, outer, 0), rgb, 0);
            }
        }
    }

    private static void glow(PoseStack pose, VertexConsumer out, float[] p, int rgb, float alpha) {
        out.addVertex(pose.last().pose(), p[0], p[1], p[2])
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, (int) (alpha * 255))));
    }

    /** A low-poly sphere, solid in the first pass. Used for event horizons, which are simply black. */
    public void sphere(double cx, double cy, double cz, double radius, int rgb) {
        if (pass != SOLID_PASS) return;
        int rings = 8, sectors = 12;
        for (int i = 0; i < rings; i++) {
            double a0 = Math.PI * i / rings, a1 = Math.PI * (i + 1) / rings;
            for (int j = 0; j < sectors; j++) {
                double b0 = Math.PI * 2 * j / sectors, b1 = Math.PI * 2 * (j + 1) / sectors;
                spherePoint(cx, cy, cz, radius, a0, b0, rgb);
                spherePoint(cx, cy, cz, radius, a1, b0, rgb);
                spherePoint(cx, cy, cz, radius, a1, b1, rgb);
                spherePoint(cx, cy, cz, radius, a0, b1, rgb);
            }
        }
    }

    private void spherePoint(double cx, double cy, double cz, double radius, double polar, double azimuth, int rgb) {
        float x = (float) (Math.sin(polar) * Math.cos(azimuth)), y = (float) Math.cos(polar), z = (float) (Math.sin(polar) * Math.sin(azimuth));
        vertex((float) (cx + x * radius), (float) (cy + y * radius), (float) (cz + z * radius), 0, 0, x, y, z, rgb, light);
    }

    // ---- world-space effects -------------------------------------------------------------------------------------

    /** An additive tube between two points, used for beams. Writes directly to a {@code GLOW}-type consumer. */
    public static void tube(PoseStack pose, VertexConsumer out, Vec3 start, Vec3 end, double radius, int rgb, float alpha) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 1.0E-6 || radius <= 0 || alpha <= 0) return;
        Vec3 axis = delta.normalize(), u = perpendicular(axis), v = axis.cross(u);
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6, b = (i + 1) * Math.PI / 6;
            Vec3 r0 = u.scale(Math.cos(a) * radius).add(v.scale(Math.sin(a) * radius));
            Vec3 r1 = u.scale(Math.cos(b) * radius).add(v.scale(Math.sin(b) * radius));
            point(pose, out, start.add(r0), rgb, alpha);
            point(pose, out, end.add(r0), rgb, alpha);
            point(pose, out, end.add(r1), rgb, alpha);
            point(pose, out, start.add(r1), rgb, alpha);
        }
    }

    /** A flat additive ring in world space, facing along {@code axis}. */
    public static void worldRing(PoseStack pose, VertexConsumer out, Vec3 center, Vec3 axis, double radius, double width, int rgb, float alpha) {
        if (alpha <= 0) return;
        Vec3 u = perpendicular(axis), v = axis.cross(u).normalize();
        int segments = 64;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            Vec3 p = u.scale(Math.cos(a)).add(v.scale(Math.sin(a))), q = u.scale(Math.cos(b)).add(v.scale(Math.sin(b)));
            point(pose, out, center.add(p.scale(Math.max(0, radius - width))), rgb, 0);
            point(pose, out, center.add(q.scale(Math.max(0, radius - width))), rgb, 0);
            point(pose, out, center.add(q.scale(radius)), rgb, alpha);
            point(pose, out, center.add(p.scale(radius)), rgb, alpha);
            point(pose, out, center.add(p.scale(radius)), rgb, alpha);
            point(pose, out, center.add(q.scale(radius)), rgb, alpha);
            point(pose, out, center.add(q.scale(radius + width)), rgb, 0);
            point(pose, out, center.add(p.scale(radius + width)), rgb, 0);
        }
    }

    private static void point(PoseStack pose, VertexConsumer out, Vec3 p, int rgb, float alpha) {
        out.addVertex(pose.last().pose(), (float) p.x, (float) p.y, (float) p.z)
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, (int) (alpha * 255))));
    }

    public static Vec3 perpendicular(Vec3 axis) {
        return axis.cross(Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
    }

    public static int noOverlay() {
        return OverlayTexture.NO_OVERLAY;
    }
}
