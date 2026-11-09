package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.math.Axis;

/** Weapons in item space: pixels, Y up, blade or shaft along +Y, centred on the origin. {@code use} runs 0..1 while the use key is held. */
public final class WeaponArt {
    private WeaponArt() {}

    // ---- Quench saber: a thin ceramic spine carrying a bare superconducting edge ------------------------------------

    public static void quench(Canvas c, float use, float current) {
        int cold = current >= 0.8F ? 0xFFFFFF : 0x7CE8FF;
        c.box(-0.45, -8, -0.5, 0.45, -3.2, 0.5, 0x26303C);
        c.box(-0.7, -8.6, -0.7, 0.7, -8, 0.7, 0xB9C5D1);
        c.hoop(0, -6.2, 0, 1, 1.05, 0.17, 0xB9C5D1);
        c.hoop(0, -4.8, 0, 1, 1.05, 0.17, 0xB9C5D1);
        c.litHoop(0, -5.5, 0, 1, 1.05, 0.1, cold);
        c.box(-2.6, -3.2, -0.9, 2.6, -2.1, 0.9, 0xE7ECF1);
        c.box(-2.0, -2.1, -0.65, 2.0, -1.5, 0.65, 0xB9C5D1);
        c.lit(-1.5, -2.9, -0.98, 1.5, -2.4, -0.88, cold);
        c.box(-0.75, -2.1, -0.4, 0.75, 11.4, 0.4, 0xE7ECF1);
        c.box(-0.4, 11.4, -0.3, 0.4, 13.2, 0.3, 0xE7ECF1);
        c.lit(0.75, -1.8, -0.15, 1.5, 11.4, 0.15, cold);
        c.lit(0.4, 11.4, -0.12, 1.0, 13.0, 0.12, cold);
        c.lit(-1.5, -1.8, -0.15, -0.75, 11.4, 0.15, cold);
        c.lit(-1.0, 11.4, -0.12, -0.4, 13.0, 0.12, cold);
        for (int i = 0; i < 4; i++) c.box(-0.76, 1 + i * 2.6, -0.42, 0.76, 1.4 + i * 2.6, 0.42, 0x26303C);
        float alpha = 0.18F + 0.5F * current + use * 0.2F;
        c.halo(0, -5.5, 0, 1, 1.05, 0.9, 0x7CE8FF, alpha);
        for (int i = 0; i < 3; i++) c.halo(0, 2 + i * 4, 0, 1, 1.7, 0.7, 0x7CE8FF, alpha * 0.5F);
    }

    // ---- Casimir blade: two parallel mirrors, and the blade is the nothing between them -------------------------------

    public static void casimir(Canvas c, float use, boolean armed, float t) {
        float closing = armed ? 1 : use;
        float gap = 1.15F - closing * 0.85F;
        int mirror = 0xC8D6E4, frame = 0x2C2F3F, vacuum = armed ? 0xFFFFFF : 0xB8A8FF;
        c.box(-0.5, -8.4, -0.55, 0.5, -3.6, 0.55, frame);
        c.box(-0.8, -8.9, -0.8, 0.8, -8.4, 0.8, mirror);
        for (int i = 0; i < 3; i++) c.box(-0.65, -7.8 + i * 1.5, -0.65, 0.65, -7.4 + i * 1.5, 0.65, 0x5A6072);
        c.box(-3.2, -3.6, -1.2, 3.2, -2.5, 1.2, frame);
        c.lit(-2.6, -3.3, -1.28, 2.6, -2.8, -1.18, vacuum);
        c.box(-gap - 0.75, -2.5, -0.9, gap + 0.75, -1.6, 0.9, 0x5A6072);
        for (int side = -1; side <= 1; side += 2) {
            double near = side * gap, far = side * (gap + 0.42);
            c.box(near, -1.6, -1.0, far, 12.0, 1.0, mirror);
            c.box(far, -1.6, -1.05, side * (gap + 0.6), 12.0, 1.05, frame);
            c.box(near, 12.0, -0.65, far, 13.6, 0.65, mirror);
        }
        double flicker = Math.sin(t * 0.9) * 0.1 + 0.9;
        c.lit(-0.05, -1.2, -0.8, 0.05, 13.2, 0.8, vacuum);
        for (int i = 0; i < 6; i++) {
            double y = -1 + ((t * 0.35 + i * 2.3) % 14);
            c.halo(0, y, 0, 0, 0.6 + gap * 0.5, 0.45, vacuum, (float) (flicker * (0.22 + closing * 0.4)));
        }
    }

    // ---- Tidal reaver: a cleaver of lightless metal bound in spun filament, with a tiny tidal focus in the spine -------

    private static final int VOID = 0x0E0D14, VOID_EDGE = 0x24212E, IVORY = 0xE7D8B0, THREAD = 0xFFE2A0;

    public static void reaver(Canvas c, float use, float t) {
        // Grip, wound with filament.
        c.box(-0.6, -10, -0.65, 0.6, -3.4, 0.65, VOID_EDGE);
        c.box(-0.9, -10.6, -0.9, 0.9, -10, 0.9, IVORY);
        for (int i = 0; i < 5; i++) c.lit(-0.68, -9.4 + i * 1.25, -0.72, 0.68, -9.15 + i * 1.25, 0.72, THREAD);
        // Guard.
        c.box(-3.6, -3.4, -1.3, 4.4, -2.3, 1.3, VOID);
        c.box(-3.6, -2.45, -1.35, 4.4, -2.2, 1.35, IVORY);
        // The blade: a stepped cleaver, broad at the tip, cutting edge on +X.
        double[][] steps = {{-2.3, 3.0, 2.2}, {3.0, 8.0, 3.2}, {8.0, 12.5, 4.4}, {12.5, 14.6, 3.4}};
        for (double[] step : steps) {
            c.box(-1.9, step[0], -0.55, step[2], step[1], 0.55, VOID);
            c.lit(step[2], step[0], -0.16, step[2] + 0.7, step[1], 0.16, THREAD);
        }
        c.box(-2.4, -2.3, -0.8, -1.9, 14.0, 0.8, IVORY);
        c.box(-1.9, 14.6, -0.55, 1.6, 15.2, 0.55, VOID);
        // Filament bindings running across the flat.
        for (int i = 0; i < 5; i++) {
            double y = -0.5 + i * 3.0 + 0.3 * Math.sin(t * 0.08 + i);
            c.lit(-1.9, y, -0.62, 2.8 + i * 0.3, y + 0.28, -0.56, THREAD);
            c.lit(-1.9, y, 0.56, 2.8 + i * 0.3, y + 0.28, 0.62, THREAD);
        }
        // The focus, seated in the spine.
        c.pose.pushPose();
        c.pose.translate(-2.4, 6.8, 0);
        c.sphere(0, 0, 0, 1.45 + use * 0.5, 0x000000);
        c.litHoop(0, 0, 0, 2, 1.8 + use * 0.5, 0.12, 0xFFF8EA);
        c.halo(0, 0, 0, 2, 1.8 + use * 0.5, 0.9, THREAD, 0.55F + use * 0.4F);
        c.pose.mulPose(Axis.YP.rotation(t * (0.14F + use * 0.5F)));
        c.pose.mulPose(Axis.XP.rotationDegrees(70));
        c.halo(0, 0, 0, 2, 3.2, 1.4, THREAD, 0.3F + use * 0.5F);
        c.pose.popPose();
    }

    // ---- Bipolar jet lance: a contained black hole on a staff, with a collimator on each pole --------------------------

    private static final int HOT = 0xFFD28A, WHITE_HOT = 0xFFF8EA;

    public static void lance(Canvas c, float ignition, float t) {
        for (int pole = -1; pole <= 1; pole += 2) {
            // Shaft from the core out to the emitter.
            c.box(-0.55, pole * 2.6, -0.55, 0.55, pole * 14.0, 0.55, VOID);
            c.box(-0.72, pole * 2.6, -0.2, 0.72, pole * 13.0, 0.2, VOID_EDGE);
            c.box(-0.8, pole * 13.0, -0.8, 0.8, pole * 14.2, 0.8, IVORY);
            c.box(-1.1, pole * 14.2, -1.1, 1.1, pole * 15.6, 1.1, VOID_EDGE);
            c.lit(-0.55, pole * 15.6, -0.55, 0.55, pole * 15.75, 0.55, ignition > 0 ? WHITE_HOT : HOT);
            // Three collimation rings stepping down toward the emitter, as on the Jet Forge.
            double[][] rings = {{5.6, 2.9}, {8.4, 2.2}, {11.0, 1.6}};
            for (int i = 0; i < rings.length; i++) {
                double y = pole * rings[i][0];
                c.hoop(0, y, 0, 1, rings[i][1], 0.3, IVORY);
                for (int k = 0; k < 4; k++) {
                    double a = k * Math.PI / 2 + Math.PI / 4;
                    double px = Math.cos(a) * rings[i][1] * 0.5, pz = Math.sin(a) * rings[i][1] * 0.5;
                    c.box(px - 0.15, y - 0.14, pz - 0.15, px + 0.15, y + 0.14, pz + 0.15, VOID_EDGE);
                }
                boolean energised = ignition > (i + 1) / 4F;
                c.litHoop(0, y, 0, 1, rings[i][1], 0.14, energised ? WHITE_HOT : 0x8A6A3A);
                if (energised) c.halo(0, y, 0, 1, rings[i][1], 1.0, HOT, 0.6F);
            }
        }
        // The cradle and the hole.
        c.box(-2.8, -1.5, -0.7, -2.2, 1.5, 0.7, IVORY);
        c.box(2.2, -1.5, -0.7, 2.8, 1.5, 0.7, IVORY);
        c.hoop(0, 0, 0, 2, 3.4, 0.36, VOID_EDGE);
        c.litHoop(0, 0, -0.36, 2, 3.4, 0.08, HOT);
        c.litHoop(0, 0, 0.36, 2, 3.4, 0.08, HOT);
        c.sphere(0, 0, 0, 1.9, 0x000000);
        c.litHoop(0, 0, 0, 2, 2.15, 0.1, WHITE_HOT);
        c.halo(0, 0, 0, 2, 2.15, 1.0, HOT, 0.6F + ignition * 0.3F);
        c.pose.pushPose();
        c.pose.mulPose(Axis.YP.rotation(t * (0.2F + ignition * 0.9F)));
        c.halo(0, 0, 0, 1, 3.0 + ignition, 1.6, HOT, 0.35F + ignition * 0.45F);
        c.halo(0, 0, 0, 1, 4.6 + ignition * 1.4, 1.1, 0xFF9A4A, 0.22F + ignition * 0.3F);
        c.pose.popPose();
    }
}
