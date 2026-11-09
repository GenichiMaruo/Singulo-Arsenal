package io.github.genichimaruo.singuloarsenal.client.art;

import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.core.Rules;

/**
 * The three suits as actual geometry. Coordinates are the vanilla player model's: pixels, Y down, each part relative to its
 * own pivot. The silhouettes differ on purpose:
 * Meissner is smooth white ceramic with hoops of cold current floating free of it (nothing touches a superconductor's field);
 * Degeneracy is plates stacked under load, seams glowing hotter as pressure rises;
 * Collapsar is lightless metal around a contained black hole, lined with nuclear pasta that visibly changes phase.
 */
public final class SuitArt {
    /** What the wearer's state contributes to the drawing. */
    public record State(float load, int phase, boolean active) {
        public static final State IDLE = new State(0, Rules.PHASE_GNOCCHI, false);
    }

    public enum Piece { HELMET, TORSO, ARM_R, ARM_L, WAIST, LEG_R, LEG_L, BOOT_R, BOOT_L }

    private SuitArt() {}

    public static void draw(Canvas c, ArmorSet set, Piece piece, State state) {
        switch (set) {
            case MEISSNER -> meissner(c, piece, state);
            case DEGENERACY -> degeneracy(c, piece, state);
            case COLLAPSAR -> collapsar(c, piece, state);
        }
    }

    // ---- Meissner ------------------------------------------------------------------------------------------------

    private static final int CERAMIC = 0xE7ECF1, CERAMIC_SHADE = 0xB9C5D1, SEAM = 0x27313D, COLD = 0x7CE8FF, COLD_DEEP = 0x2E9BC4;

    private static void meissner(Canvas c, Piece piece, State s) {
        float breathe = 0.5F + 0.5F * (float) Math.sin(c.t * 0.08);
        switch (piece) {
            case HELMET -> {
                c.box(-4.7, -8.6, -4.7, 4.7, -3.0, 4.7, CERAMIC);
                c.box(-3.9, -9.2, -3.9, 3.9, -8.6, 3.9, CERAMIC_SHADE);
                c.box(-4.7, -3.0, -4.7, 4.7, -0.3, 4.7, CERAMIC_SHADE);
                c.box(-4.0, -6.5, -5.05, 4.0, -3.2, -4.6, SEAM);
                c.lit(-3.6, -5.9, -5.15, 3.6, -3.8, -5.0, COLD);
                for (int x = -3; x <= 2; x += 2) c.box(x, -2.1, -5.0, x + 0.9, -0.8, -4.6, SEAM);
                c.box(-4.9, -8.0, -1.0, -4.6, -4.0, 1.0, SEAM);
                c.box(4.6, -8.0, -1.0, 4.9, -4.0, 1.0, SEAM);
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 4.6, -6.3, -1.7, side * 5.8, -2.8, 1.7, CERAMIC_SHADE);
                    c.lit(side * 5.7, -5.3, -0.8, side * 5.95, -3.6, 0.8, COLD);
                }
                float y = -10.4F - breathe * 0.3F;
                c.litHoop(0, y, 0, 1, 5.5, 0.18, COLD);
                c.halo(0, y, 0, 1, 5.5, 1.3, COLD, 0.3F);
            }
            case TORSO -> {
                c.box(-4.7, -0.2, -2.9, 4.7, 7.0, 2.9, CERAMIC);
                c.box(-4.3, 7.0, -2.6, 4.3, 10.6, 2.6, CERAMIC_SHADE);
                c.box(-2.9, 0.6, -3.1, 2.9, 6.4, -2.9, SEAM);
                c.hoop(0, 3.5, -3.15, 2, 2.2, 0.32, CERAMIC_SHADE);
                c.litHoop(0, 3.5, -3.35, 2, 2.2, 0.14, COLD);
                c.lit(-0.5, 3.0, -3.3, 0.5, 4.0, -3.15, COLD);
                c.halo(0, 3.5, -3.5, 2, 2.2, 1.3, COLD, 0.25F + breathe * 0.2F);
                for (int side = -1; side <= 1; side += 2) c.box(side * 3.8, 0.2, -3.05, side * 4.3, 6.4, -2.85, SEAM);
                c.box(-4.0, 8.2, -2.85, 4.0, 8.7, -2.55, SEAM);
                // Cryostat pack: the cold has to come from somewhere.
                c.box(-3.3, 0.6, 2.9, 3.3, 8.2, 4.7, CERAMIC_SHADE);
                c.box(-2.6, 1.3, 4.7, 2.6, 7.5, 5.0, SEAM);
                for (int side = -1; side <= 1; side += 2) c.lit(side * 1.3 - 0.4, 1.8, 4.85, side * 1.3 + 0.4, 7.0, 5.1, COLD);
            }
            case ARM_R -> meissnerArm(c, -1, breathe);
            case ARM_L -> meissnerArm(c, 1, breathe);
            case WAIST -> {
                c.box(-4.5, 9.8, -2.7, 4.5, 12.2, 2.7, SEAM);
                c.box(-1.7, 10.1, -3.0, 1.7, 11.9, -2.6, CERAMIC_SHADE);
                c.lit(-1.0, 10.6, -3.1, 1.0, 11.4, -2.95, COLD);
            }
            case LEG_R -> meissnerLeg(c, -1);
            case LEG_L -> meissnerLeg(c, 1);
            case BOOT_R -> meissnerBoot(c, s);
            case BOOT_L -> meissnerBoot(c, s);
        }
    }

    private static void meissnerArm(Canvas c, int side, float breathe) {
        double x0 = side < 0 ? -3.0 : -1.0, x1 = side < 0 ? 1.0 : 3.0, mid = (x0 + x1) / 2;
        c.box(x0 - 0.7, -2.6, -2.8, x1 + 0.7, 2.2, 2.8, CERAMIC);
        c.box(x0 - 0.4, 2.2, -2.4, x1 + 0.4, 5.0, 2.4, SEAM);
        c.box(x0 - 0.6, 5.0, -2.6, x1 + 0.6, 10.3, 2.6, CERAMIC);
        c.box(x0 - 0.75, 6.0, -1.0, x0 - 0.55, 9.0, 1.0, CERAMIC_SHADE);
        double y = 7.6 + breathe * 0.4;
        c.litHoop(mid, y, 0, 1, 4.0, 0.14, COLD);
        c.halo(mid, y, 0, 1, 4.0, 1.0, COLD, 0.24F);
    }

    private static void meissnerLeg(Canvas c, int side) {
        c.box(-2.5, -0.1, -2.6, 2.5, 5.2, 2.6, CERAMIC);
        c.box(-2.25, 5.2, -2.3, 2.25, 7.4, 2.3, SEAM);
        c.box(-2.7, 4.7, -3.0, 2.7, 6.9, -2.3, CERAMIC_SHADE);
        c.lit(-1.2, 5.4, -3.1, 1.2, 6.1, -2.95, COLD);
        c.box(side * 2.5, 1.0, -1.0, side * 2.75, 4.2, 1.0, CERAMIC_SHADE);
    }

    private static void meissnerBoot(Canvas c, State s) {
        c.box(-2.65, 7.4, -2.75, 2.65, 11.0, 2.75, CERAMIC);
        c.box(-2.8, 11.0, -3.6, 2.8, 12.3, 2.9, SEAM);
        c.box(-2.0, 8.8, -3.0, 2.0, 10.1, -2.7, CERAMIC_SHADE);
        c.lit(-1.2, 12.3, -2.0, 1.2, 12.45, 1.4, COLD);
        // A levitation pad that opens out while the flux lattice is pinned.
        double y = 13.1 + (s.active ? 0.3 : 0);
        c.litHoop(0, y, -0.3, 1, s.active ? 3.4 : 2.6, 0.13, COLD);
        c.halo(0, y, -0.3, 1, s.active ? 3.4 : 2.6, 1.2, COLD, s.active ? 0.6F : 0.18F);
    }

    // ---- Degeneracy ----------------------------------------------------------------------------------------------

    private static final int PLATE = 0x3E3936, PLATE_LIGHT = 0x59524A, PLATE_DARK = 0x262220, RIVET = 0x8A7A62;

    private static int hot(State s, float t) {
        float wave = 0.85F + 0.15F * (float) Math.sin(t * 0.25);
        float heat = Math.min(1, s.load * wave);
        int r = (int) (205 + 50 * heat), g = (int) (112 + 100 * heat), b = (int) (28 + 130 * heat * heat);
        return (r << 16) | (g << 8) | b;
    }

    /** A column of plates held apart by pressure. As load rises the gaps close and the seam light spreads. */
    private static void stack(Canvas c, double x0, double y0, double z0, double x1, double y1, double z1, int plates, int hot, float load) {
        double pitch = (y1 - y0) / plates, gap = 0.34 - 0.2 * load;
        for (int i = 0; i < plates; i++) {
            double inset = (i % 2) * 0.28, top = y0 + i * pitch;
            int color = i % 2 == 0 ? PLATE : PLATE_LIGHT;
            c.box(x0 + inset, top, z0 + inset, x1 - inset, top + pitch - gap, z1 - inset, color);
            if (i < plates - 1) c.lit(x0 + 0.5, top + pitch - gap, z0 + 0.5, x1 - 0.5, top + pitch, z1 - 0.5, hot);
        }
    }

    private static void degeneracy(Canvas c, Piece piece, State s) {
        int hot = hot(s, c.t);
        float halo = 0.12F + s.load * 0.55F;
        switch (piece) {
            case HELMET -> {
                stack(c, -5.0, -9.0, -5.0, 5.0, -3.2, 5.0, 3, hot, s.load);
                c.box(-5.2, -3.2, -5.2, 5.2, -0.2, 5.2, PLATE_DARK);
                c.box(-4.4, -6.4, -5.45, 4.4, -3.0, -5.0, PLATE_DARK);
                c.lit(-3.8, -5.3, -5.55, 3.8, -4.5, -5.4, hot);
                c.box(-1.0, -10.2, -3.0, 1.0, -9.0, 3.0, PLATE_LIGHT);
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 5.0, -7.5, -2.2, side * 6.2, -1.2, 2.2, PLATE);
                    c.box(side * 2.0, -2.2, -5.5, side * 3.2, -0.8, -5.2, RIVET);
                }
            }
            case TORSO -> {
                stack(c, -5.0, -0.4, -3.4, 5.0, 10.2, 3.4, 5, hot, s.load);
                c.box(-2.0, 0.4, -3.75, 2.0, 7.6, -3.3, PLATE_DARK);
                c.lit(-0.8, 1.0, -3.85, 0.8, 7.0, -3.7, hot);
                c.halo(0, 4, -4.0, 2, 2.0 + s.load * 1.4, 1.5, hot, halo);
                // Dorsal manifold: the gauge of what the plates are holding back.
                c.box(-3.0, 0.2, 3.4, 3.0, 9.2, 5.2, PLATE_DARK);
                for (int i = 0; i < 4; i++) c.lit(-2.2, 1.0 + i * 2, 5.15, 2.2, 1.0 + i * 2 + 0.9, 5.35, i < s.load * 4 ? hot : 0x4A2A14);
            }
            case ARM_R -> degeneracyArm(c, -1, hot, s);
            case ARM_L -> degeneracyArm(c, 1, hot, s);
            case WAIST -> {
                c.box(-4.8, 9.8, -3.0, 4.8, 12.4, 3.0, PLATE_DARK);
                for (int x = -3; x <= 3; x += 2) c.box(x - 0.4, 10.3, -3.3, x + 0.4, 11.9, -3.0, RIVET);
            }
            case LEG_R -> degeneracyLeg(c, hot, s);
            case LEG_L -> degeneracyLeg(c, hot, s);
            case BOOT_R -> degeneracyBoot(c, hot);
            case BOOT_L -> degeneracyBoot(c, hot);
        }
    }

    private static void degeneracyArm(Canvas c, int side, int hot, State s) {
        double x0 = side < 0 ? -3.0 : -1.0, x1 = side < 0 ? 1.0 : 3.0;
        // Pauldron: the widest thing on the suit.
        stack(c, x0 - 2.0 * (side < 0 ? 1 : 0.2), -3.8, -3.6, x1 + 2.0 * (side > 0 ? 1 : 0.2), 2.4, 3.6, 3, hot, s.load);
        c.box(x0 - 0.5, 2.4, -2.5, x1 + 0.5, 5.0, 2.5, PLATE_DARK);
        stack(c, x0 - 0.8, 5.0, -2.9, x1 + 0.8, 10.4, 2.9, 3, hot, s.load);
    }

    private static void degeneracyLeg(Canvas c, int hot, State s) {
        stack(c, -2.8, -0.2, -3.0, 2.8, 5.6, 3.0, 3, hot, s.load);
        c.box(-2.4, 5.6, -2.6, 2.4, 7.6, 2.6, PLATE_DARK);
        c.box(-3.0, 5.0, -3.5, 3.0, 7.4, -2.6, PLATE_LIGHT);
        c.lit(-1.5, 5.8, -3.6, 1.5, 6.6, -3.45, hot);
    }

    private static void degeneracyBoot(Canvas c, int hot) {
        c.box(-3.0, 7.4, -3.0, 3.0, 10.4, 3.0, PLATE);
        c.box(-3.3, 10.4, -3.9, 3.3, 12.5, 3.2, PLATE_DARK);
        c.box(-2.6, 8.4, -3.3, 2.6, 9.6, -3.0, PLATE_LIGHT);
        c.lit(-2.2, 12.5, -3.0, 2.2, 12.65, 2.4, hot);
    }

    // ---- Collapsar -----------------------------------------------------------------------------------------------

    private static final int VOID = 0x0E0D14, VOID_EDGE = 0x24212E, IVORY = 0xE7D8B0, JET = 0xFFD28A, WHITE_HOT = 0xFFF8EA;

    private static int phaseColor(int phase) {
        return switch (phase) {
            case Rules.PHASE_GNOCCHI -> 0xFFB061;
            case Rules.PHASE_SPAGHETTI -> 0xFFE2A0;
            default -> 0xFFFFFF;
        };
    }

    /** The lining seen through the chest: separate grains, then strands, then continuous sheets, as the accumulator's pasta. */
    private static void pasta(Canvas c, double x0, double y0, double x1, double y1, double z, int phase) {
        int color = phaseColor(phase);
        switch (phase) {
            case Rules.PHASE_GNOCCHI -> {
                for (double y = y0 + 0.3; y < y1 - 0.5; y += 1.25) {
                    for (double x = x0 + 0.3; x < x1 - 0.5; x += 1.25) c.lit(x, y, z, x + 0.62, y + 0.62, z - 0.14, color);
                }
            }
            case Rules.PHASE_SPAGHETTI -> {
                for (double x = x0 + 0.3; x < x1 - 0.3; x += 1.25) {
                    double wobble = Math.sin((x + c.t * 0.05) * 1.7) * 0.25;
                    c.lit(x + wobble, y0 + 0.2, z, x + wobble + 0.42, y1 - 0.2, z - 0.14, color);
                }
            }
            default -> {
                for (double y = y0 + 0.2; y < y1 - 0.2; y += 1.6) c.lit(x0 + 0.2, y, z, x1 - 0.2, y + 0.95, z - 0.14, color);
            }
        }
    }

    private static void collapsar(Canvas c, Piece piece, State s) {
        int glow = phaseColor(s.phase);
        float spin = c.t * 0.11F;
        switch (piece) {
            case HELMET -> {
                c.box(-4.8, -9.0, -4.8, 4.8, -3.6, 4.8, VOID);
                c.box(-3.9, -9.6, -3.9, 3.9, -9.0, 3.9, VOID_EDGE);
                c.box(-4.8, -3.6, -4.8, 4.8, -0.2, 4.8, VOID_EDGE);
                c.box(-4.2, -5.6, -5.2, 4.2, -3.9, -4.7, IVORY);
                c.box(-4.0, -5.4, -5.3, 4.0, -4.1, -5.1, VOID);
                c.lit(-3.5, -5.0, -5.45, 3.5, -4.5, -5.25, WHITE_HOT);
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 4.8, -8.0, -1.4, side * 5.4, -2.0, 1.4, IVORY);
                    c.box(side * 1.4, -9.0, -5.0, side * 0.9, -5.6, -4.75, IVORY);
                }
                // Observer's crown: a thin ring that never quite touches.
                double y = -11.0 + 0.25 * Math.sin(c.t * 0.07);
                c.litHoop(0, y, 0, 1, 4.6, 0.12, IVORY);
                c.halo(0, y, 0, 1, 4.6, 1.1, JET, 0.35F);
            }
            case TORSO -> {
                c.box(-4.9, -0.3, -3.0, 4.9, 7.4, 3.0, VOID);
                c.box(-4.4, 7.4, -2.7, 4.4, 10.4, 2.7, VOID_EDGE);
                c.box(-3.4, 0.4, -3.3, 3.4, 6.8, -2.95, VOID_EDGE);
                pasta(c, -3.2, 0.6, 3.2, 6.6, -3.32, s.phase);
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 4.0, -0.1, -3.2, side * 4.6, 7.2, -2.95, IVORY);
                    c.box(side * 1.0, 7.4, -2.95, side * 1.4, 10.2, -2.65, IVORY);
                }
                c.box(-4.2, 7.0, -3.2, 4.2, 7.4, -2.95, IVORY);
                // The contained hole rides between the shoulder blades, its disk turning edge-on to the spine.
                c.box(-1.8, 1.4, 3.0, 1.8, 5.4, 4.2, VOID_EDGE);
                c.pose.pushPose();
                c.pose.translate(0, 3.4, 6.6);
                c.sphere(0, 0, 0, 1.25, 0x000000);
                c.litHoop(0, 0, 0, 2, 1.55, 0.11, WHITE_HOT);
                c.halo(0, 0, 0, 2, 1.55, 0.9, glow, 0.7F);
                c.pose.mulPose(com.mojang.math.Axis.ZP.rotation(spin));
                c.pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(62));
                c.litHoop(0, 0, 0, 2, 3.8, 0.16, JET);
                c.halo(0, 0, 0, 2, 3.0, 1.9, JET, 0.4F);
                c.halo(0, 0, 0, 2, 4.6, 1.3, 0xFF9A4A, 0.26F);
                c.pose.popPose();
                // Polar nozzles either side of it, where Jet Burst leaves the suit.
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 3.0, 0.2, 3.0, side * 4.5, 6.6, 5.0, VOID_EDGE);
                    c.box(side * 3.0, 6.6, 3.2, side * 4.5, 7.5, 4.8, IVORY);
                    c.lit(side * 3.2, 7.5, 3.4, side * 4.3, 7.65, 4.6, glow);
                }
            }
            case ARM_R -> collapsarArm(c, -1, glow);
            case ARM_L -> collapsarArm(c, 1, glow);
            case WAIST -> {
                c.box(-4.6, 9.8, -2.9, 4.6, 12.3, 2.9, VOID_EDGE);
                c.box(-1.9, 10.2, -3.2, 1.9, 11.9, -2.85, IVORY);
                c.sphere(0, 11.05, -3.2, 0.62, 0x000000);
                c.halo(0, 11.05, -3.4, 2, 0.95, 0.5, glow, 0.6F);
            }
            case LEG_R -> collapsarLeg(c, -1, glow);
            case LEG_L -> collapsarLeg(c, 1, glow);
            case BOOT_R -> collapsarBoot(c, glow);
            case BOOT_L -> collapsarBoot(c, glow);
        }
    }

    private static void collapsarArm(Canvas c, int side, int glow) {
        double x0 = side < 0 ? -3.0 : -1.0, x1 = side < 0 ? 1.0 : 3.0, mid = (x0 + x1) / 2, outer = side < 0 ? x0 : x1;
        c.box(x0 - 0.9, -3.0, -3.0, x1 + 0.9, 2.2, 3.0, VOID);
        c.box(outer + side * 0.9, -2.6, -2.4, outer + side * 1.3, 1.8, 2.4, IVORY);
        c.box(x0 - 0.4, 2.2, -2.4, x1 + 0.4, 4.8, 2.4, VOID_EDGE);
        c.box(x0 - 0.75, 4.8, -2.75, x1 + 0.75, 10.4, 2.75, VOID);
        c.box(x0 - 0.8, 9.4, -2.85, x1 + 0.8, 9.9, 2.85, IVORY);
        c.lit(outer + side * 0.75, 5.6, -0.5, outer + side * 0.95, 8.8, 0.5, glow);
        c.litHoop(mid, -0.4, 0, 0, 2.9, 0.1, IVORY);
    }

    private static void collapsarLeg(Canvas c, int side, int glow) {
        c.box(-2.7, -0.2, -2.8, 2.7, 5.4, 2.8, VOID);
        c.box(-2.3, 5.4, -2.4, 2.3, 7.4, 2.4, VOID_EDGE);
        c.box(-2.9, 4.8, -3.3, 2.9, 7.0, -2.5, VOID);
        c.box(-1.4, 5.3, -3.45, 1.4, 6.5, -3.25, IVORY);
        c.lit(side * 2.7, 0.6, -0.4, side * 2.9, 4.6, 0.4, glow);
    }

    private static void collapsarBoot(Canvas c, int glow) {
        c.box(-2.85, 7.4, -2.95, 2.85, 10.4, 2.95, VOID);
        c.box(-3.0, 10.4, -3.9, 3.0, 12.3, 3.1, VOID_EDGE);
        c.box(-3.05, 10.3, -3.95, 3.05, 10.75, 3.15, IVORY);
        c.box(-1.6, 8.0, -3.25, 1.6, 9.8, -2.9, IVORY);
        c.lit(-2.2, 12.3, -3.0, 2.2, 12.45, 2.4, glow);
        c.halo(0, 12.7, -0.3, 1, 2.2, 1.2, glow, 0.5F);
    }
}
