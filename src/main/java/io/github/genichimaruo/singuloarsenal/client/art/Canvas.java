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
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

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
    /** Width of the tiling material PNGs, in texels. One texel covers one model pixel. */
    private static final int MATERIAL_SIZE = 32;
    private static final int FINE_SIZE = 64;
    private static final ResourceLocation FINE = SinguloArsenal.id("textures/misc/material_fine.png");

    public final PoseStack pose;
    /** Game time in ticks, for animation. */
    public final float t;
    private final int light;
    private final int overlay;
    private int pass;
    private VertexConsumer out;

    private final boolean textured;
    /** Bevel width in model pixels. */
    private final float bevel;
    /** Texels of the material per model pixel. */
    private final int density;
    private final int materialSize;
    /** Fine materials also shade each plate top to bottom, which large weapon surfaces need to avoid looking flat. */
    private final boolean gradient;
    /** Whether the pose is camera-relative, so {@link #faceCamera} can find the camera. False in GUI rendering. */
    private final boolean billboard;

    private Canvas(PoseStack pose, float t, int light, int overlay, boolean textured, float bevel, int density, int materialSize, boolean gradient, boolean billboard) {
        this.pose = pose;
        this.t = t;
        this.light = light;
        this.overlay = overlay;
        this.textured = textured;
        this.bevel = bevel;
        this.density = density;
        this.materialSize = materialSize;
        this.gradient = gradient;
        this.billboard = billboard;
    }

    /** Flat-coloured drawing in a camera-relative pose. */
    public static void draw(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, Consumer<Canvas> art) {
        run(new Canvas(pose, t, light, overlay, false, 0, 1, 32, false, true), buffers, SOLID, art);
    }

    /** Suits: one texel per model pixel and a one-pixel bevel. All plate edges must lie on the model's pixel grid. */
    public static void drawMaterial(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, ResourceLocation material, boolean billboard, Consumer<Canvas> art) {
        run(new Canvas(pose, t, light, overlay, true, 1, 1, MATERIAL_SIZE, false, billboard), buffers, RenderType.entityCutoutNoCull(material), art);
    }

    /** Weapons: two texels per model pixel, a half-pixel bevel and a vertical gradient, because weapons are seen up close. */
    public static void drawFine(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, boolean billboard, Consumer<Canvas> art) {
        run(new Canvas(pose, t, light, overlay, true, 0.5F, 2, FINE_SIZE, true, billboard), buffers, RenderType.entityCutoutNoCull(FINE), art);
    }

    private static void run(Canvas canvas, MultiBufferSource buffers, RenderType solid, Consumer<Canvas> art) {
        canvas.pass = SOLID_PASS;
        canvas.out = buffers.getBuffer(solid);
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
        if (pass != SOLID_PASS) return;
        if (textured) material(x0, y0, z0, x1, y1, z1, rgb, true);
        else cuboid(x0, y0, z0, x1, y1, z1, rgb, light);
    }

    /** A plate with the material but no bevel: for markings painted onto a surface and for single voxels. */
    public void flat(double x0, double y0, double z0, double x1, double y1, double z1, int rgb) {
        if (pass == SOLID_PASS) material(x0, y0, z0, x1, y1, z1, rgb, false);
    }

    /** A hollow circle of one-pixel voxels, so round parts stay on the same pixel scale as everything else. */
    public void voxelRing(double cx, double cy, double cz, int axis, int radius, int rgb, boolean selfLit) {
        java.util.Set<Long> done = new java.util.HashSet<>();
        for (int i = 0; i < radius * 16; i++) {
            double angle = i * Math.PI * 2 / (radius * 16);
            int u = (int) Math.round(Math.cos(angle) * radius), v = (int) Math.round(Math.sin(angle) * radius);
            if (!done.add(u * 1000L + v)) continue;
            double[] p = switch (axis) {
                case 0 -> new double[]{cx, cy + u, cz + v};
                case 1 -> new double[]{cx + u, cy, cz + v};
                default -> new double[]{cx + u, cy + v, cz};
            };
            voxel(p[0], p[1], p[2], rgb, selfLit);
        }
    }

    /** A ball of whole-pixel voxels. */
    public void voxelBall(double cx, double cy, double cz, double radius, int rgb) {
        int r = (int) Math.ceil(radius);
        for (int x = -r; x <= r; x++) for (int y = -r; y <= r; y++) for (int z = -r; z <= r; z++) {
            if (x * x + y * y + z * z <= radius * radius) voxel(cx + x, cy + y, cz + z, rgb, false);
        }
    }

    private void voxel(double x, double y, double z, int rgb, boolean selfLit) {
        if (selfLit) lit(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5, rgb);
        else flat(x - 0.5, y - 0.5, z - 0.5, x + 0.5, y + 0.5, z + 0.5, rgb);
    }

    private void material(double ax, double ay, double az, double bx, double by, double bz, int rgb, boolean bevel) {
        float x0 = (float) Math.min(ax, bx), x1 = (float) Math.max(ax, bx);
        float y0 = (float) Math.min(ay, by), y1 = (float) Math.max(ay, by);
        float z0 = (float) Math.min(az, bz), z1 = (float) Math.max(az, bz);
        // Each plate starts somewhere different in the tile, on a whole texel, so neighbours do not repeat.
        int seed = Math.floorMod(Math.round(x0 * 2) * 7 + Math.round(y0 * 2) * 13 + Math.round(z0 * 2) * 5 + Math.round((x1 - x0) * 3), materialSize);
        float w = x1 - x0, h = y1 - y0, d = z1 - z0;
        panel(x0, y0, z0, w, 0, 0, 0, h, 0, 0, 0, -1, seed, rgb, bevel);
        panel(x0, y0, z1, w, 0, 0, 0, h, 0, 0, 0, 1, seed + 11, rgb, bevel);
        panel(x0, y0, z0, 0, 0, d, 0, h, 0, -1, 0, 0, seed + 5, rgb, bevel);
        panel(x1, y0, z0, 0, 0, d, 0, h, 0, 1, 0, 0, seed + 17, rgb, bevel);
        panel(x0, y1, z0, w, 0, 0, 0, 0, d, 0, 1, 0, seed + 23, rgb, bevel);
        panel(x0, y0, z0, w, 0, 0, 0, 0, d, 0, -1, 0, seed + 29, rgb, bevel);
    }

    /** One face, from origin along U and V. With a bevel: a lit top edge, a shadowed bottom edge and darker sides. */
    private void panel(float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz, float nx, float ny, float nz, int seed, int rgb, boolean withBevel) {
        float width = Math.abs(ux + uy + uz), height = Math.abs(vx + vy + vz), e = bevel;
        int top = gradient ? shade(rgb, 1.12F) : rgb, bottom = gradient ? shade(rgb, 0.76F) : rgb;
        if (!withBevel || width < 3 * e || height < 3 * e) {
            strip(ox, oy, oz, ux, uy, uz, vx, vy, vz, nx, ny, nz, seed, 0, width, 0, height, width, height, top, bottom);
            return;
        }
        strip(ox, oy, oz, ux, uy, uz, vx, vy, vz, nx, ny, nz, seed, e, width - e, e, height - e, width, height, top, bottom);
        int high = shade(rgb, 1.28F), low = shade(rgb, 0.58F);
        strip(ox, oy, oz, ux, uy, uz, vx, vy, vz, nx, ny, nz, seed, 0, width, 0, e, width, height, high, high);
        strip(ox, oy, oz, ux, uy, uz, vx, vy, vz, nx, ny, nz, seed, 0, width, height - e, height, width, height, low, low);
        strip(ox, oy, oz, ux, uy, uz, vx, vy, vz, nx, ny, nz, seed, 0, e, e, height - e, width, height, shade(top, 0.86F), shade(bottom, 0.86F));
        strip(ox, oy, oz, ux, uy, uz, vx, vy, vz, nx, ny, nz, seed, width - e, width, e, height - e, width, height, shade(top, 0.78F), shade(bottom, 0.78F));
    }

    private void strip(float ox, float oy, float oz, float ux, float uy, float uz, float vx, float vy, float vz, float nx, float ny, float nz,
                       int seed, float u0, float u1, float v0, float v1, float width, float height, int rgbTop, int rgbBottom) {
        float a = u0 / width, b = u1 / width, c = v0 / height, d = v1 / height;
        float size = materialSize;
        float s0 = (seed + u0 * density) / size, s1 = (seed + u1 * density) / size, t0 = (seed + v0 * density) / size, t1 = (seed + v1 * density) / size;
        materialVertex(ox + ux * a + vx * c, oy + uy * a + vy * c, oz + uz * a + vz * c, s0, t0, nx, ny, nz, rgbTop);
        materialVertex(ox + ux * a + vx * d, oy + uy * a + vy * d, oz + uz * a + vz * d, s0, t1, nx, ny, nz, rgbBottom);
        materialVertex(ox + ux * b + vx * d, oy + uy * b + vy * d, oz + uz * b + vz * d, s1, t1, nx, ny, nz, rgbBottom);
        materialVertex(ox + ux * b + vx * c, oy + uy * b + vy * c, oz + uz * b + vz * c, s1, t0, nx, ny, nz, rgbTop);
    }

    private void materialVertex(float x, float y, float z, float u, float v, float nx, float ny, float nz, int rgb) {
        vertex(x, y, z, u, v, nx, ny, nz, rgb, light);
    }

    private static int shade(int rgb, float factor) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * factor));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * factor));
        int b = Math.min(255, (int) ((rgb & 255) * factor));
        return (r << 16) | (g << 8) | b;
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
        int rings = 16, sectors = 24;
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

    /** Turns the pose so +Z points at the camera and +Y stays as close to the current up as possible. */
    public void faceCamera() {
        if (!billboard) return;
        Vector3f camera = new Matrix4f(pose.last().pose()).invert().transformPosition(new Vector3f());
        if (camera.lengthSquared() < 1.0E-6F) return;
        Vector3f z = camera.normalize(new Vector3f());
        Vector3f x = new Vector3f(0, 1, 0).cross(z);
        if (x.lengthSquared() < 1.0E-4F) x.set(1, 0, 0);
        x.normalize();
        Vector3f y = new Vector3f(z).cross(x);
        pose.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(x, y, z)));
    }

    /** A soft additive band along Y at the given X and Z, fading to nothing at +-width: the glow of an energised edge. */
    public void softBand(double x, double y0, double y1, double z, double width, int rgb, float alpha) {
        if (pass != GLOW_PASS || alpha <= 0) return;
        for (int side = -1; side <= 1; side += 2) {
            glow(pose, out, new float[]{(float) x, (float) y0, (float) z}, rgb, alpha);
            glow(pose, out, new float[]{(float) x, (float) y1, (float) z}, rgb, alpha);
            glow(pose, out, new float[]{(float) (x + side * width), (float) y1, (float) z}, rgb, 0);
            glow(pose, out, new float[]{(float) (x + side * width), (float) y0, (float) z}, rgb, 0);
        }
    }

    /**
     * A black hole as it would look: a shadow, a thin photon ring, the far side of the disk bent up and over by lensing, and the
     * near side crossing in front. The disk is brighter on the side turning toward the viewer. Draw it in a pose that faces the
     * camera (+Z toward the viewer); it is depth-tested, so bodies still hide it.
     */
    public void blackHole(double radius, double diskReach, float intensity) {
        sphere(0, 0, 0, radius, 0x000000);
        if (pass != GLOW_PASS) return;
        double phase = t * 0.14;
        int segments = 72;
        for (int i = 0; i < segments; i++) {
            double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
            // Lensed image of the far disk: strongest over the top, thin underneath.
            double lens = 0.42 + 0.58 * Math.max(0, Math.sin((a + b) / 2));
            ringQuad(a, b, radius * 1.06, radius * (1.5 + 0.5 * lens), 0, 0xFFE8BC, intensity * (float) (0.85 * lens), 0xFF9A4A, 0);
            // Photon ring: thin and nearly white all the way round.
            ringQuad(a, b, radius * 0.98, radius * 1.12, 0, 0xFFFFFF, intensity, 0xFFF4DC, intensity * 0.4F);
        }
        // The disk seen nearly edge-on. Its near half is in front of the shadow, its far half behind it.
        double flatten = 0.24;
        int bands = 5;
        for (int band = 0; band < bands; band++) {
            double r0 = radius * (1.3 + (diskReach - 1.3) * band / bands), r1 = radius * (1.3 + (diskReach - 1.3) * (band + 1) / bands);
            float f0 = band / (float) bands, f1 = (band + 1) / (float) bands;
            for (int i = 0; i < segments; i++) {
                double a = i * Math.PI * 2 / segments, b = (i + 1) * Math.PI * 2 / segments;
                float a0 = diskAlpha(a, f0, phase, r0) * intensity, a1 = diskAlpha(a, f1, phase, r1) * intensity;
                float b0 = diskAlpha(b, f0, phase, r0) * intensity, b1 = diskAlpha(b, f1, phase, r1) * intensity;
                double z0 = Math.sin(a) < 0 ? radius * 1.15 : -radius * 1.15, z1 = Math.sin(b) < 0 ? radius * 1.15 : -radius * 1.15;
                if (z0 != z1) continue;
                glow(pose, out, new float[]{(float) (Math.cos(a) * r0), (float) (Math.sin(a) * r0 * flatten), (float) z0}, diskColor(f0), a0);
                glow(pose, out, new float[]{(float) (Math.cos(b) * r0), (float) (Math.sin(b) * r0 * flatten), (float) z0}, diskColor(f0), b0);
                glow(pose, out, new float[]{(float) (Math.cos(b) * r1), (float) (Math.sin(b) * r1 * flatten), (float) z0}, diskColor(f1), b1);
                glow(pose, out, new float[]{(float) (Math.cos(a) * r1), (float) (Math.sin(a) * r1 * flatten), (float) z0}, diskColor(f1), a1);
            }
        }
    }

    private void ringQuad(double a, double b, double r0, double r1, double z, int rgb0, float alpha0, int rgb1, float alpha1) {
        glow(pose, out, onRing(0, 0, z, 2, a, r0, 0), rgb0, alpha0);
        glow(pose, out, onRing(0, 0, z, 2, b, r0, 0), rgb0, alpha0);
        glow(pose, out, onRing(0, 0, z, 2, b, r1, 0), rgb1, alpha1);
        glow(pose, out, onRing(0, 0, z, 2, a, r1, 0), rgb1, alpha1);
    }

    private static float diskAlpha(double angle, float radial, double phase, double r) {
        double doppler = 0.34 + 0.66 * (0.5 - 0.5 * Math.cos(angle));
        double streaks = 0.72 + 0.28 * Math.sin(angle * 9 + r * 2.2 - phase * 3);
        return (float) ((1 - radial) * (1 - radial) * doppler * streaks * 1.25);
    }

    private static int diskColor(float radial) {
        int r = 255, g = (int) (244 - 130 * radial), b = (int) (214 - 170 * radial);
        return (r << 16) | (g << 8) | b;
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
