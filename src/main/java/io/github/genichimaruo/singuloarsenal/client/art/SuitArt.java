package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import net.minecraft.resources.ResourceLocation;

/**
 * The three suits as geometry on the vanilla player model's pixel grid: every visible width and height is a whole number of
 * model pixels, and each plate carries a material texture at one texel per pixel, so the armor reads at the same scale as a
 * Minecraft skin. The black holes are the one deliberate exception and are drawn realistically. {@code box} is a bevelled plate, {@code flat} a marking painted on a surface (offset {@link #P} so it has no
 * visible side), {@code lit} the same but self-lit.
 * <p>
 * Meissner is white ceramic with rings of cold current floating free of it; Degeneracy is plates stacked under load, seams
 * glowing hotter as pressure rises; Collapsar is dark metal around a contained black hole, lined with nuclear pasta that
 * visibly changes phase.
 */
public final class SuitArt {
    /** What the wearer's state contributes to the drawing. */
    public record State(float load, int phase, boolean active) {
        public static final State IDLE = new State(0, Rules.PHASE_GNOCCHI, false);
    }

    public enum Piece { HELMET, TORSO, ARM_R, ARM_L, WAIST, LEG_R, LEG_L, BOOT_R, BOOT_L }

    /** How far a painted marking stands off its surface. */
    private static final double P = 0.04;

    private SuitArt() {}

    public static ResourceLocation material(ArmorSet set) {
        return SinguloArsenal.id("textures/misc/material_" + set.id + ".png");
    }

    public static void draw(Canvas c, ArmorSet set, Piece piece, State state) {
        switch (set) {
            case MEISSNER -> meissner(c, piece, state);
            case DEGENERACY -> degeneracy(c, piece, state);
            case COLLAPSAR -> collapsar(c, piece, state);
        }
    }

    /** The arm's box in model space: 4 wide, offset toward the outside of the body. */
    private static double armMin(int side) { return side < 0 ? -3 : -1; }
    private static double armMax(int side) { return side < 0 ? 1 : 3; }

    // ---- Meissner ------------------------------------------------------------------------------------------------

    private static final int CERAMIC = 0xF4F6FA, CERAMIC_SHADE = 0xC2CDD9, SEAM = 0x465365, COLD = 0x7CE8FF;

    private static void meissner(Canvas c, Piece piece, State s) {
        switch (piece) {
            case HELMET -> {
                c.box(-5, -9, -5, 5, -2, 5, CERAMIC);
                c.box(-4, -10, -4, 4, -9, 4, CERAMIC_SHADE);
                c.box(-5, -2, -5, 5, 0, 5, CERAMIC_SHADE);
                c.flat(-4, -7, -5 - P, 4, -3, -5, SEAM);
                c.lit(-3, -6, -5 - 2 * P, 3, -4, -5 - P, COLD);
                for (int x = -3; x <= 1; x += 2) c.flat(x, -2, -5 - P, x + 1, -1, -5, SEAM);
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 5, -6, -2, side * 6, -2, 2, CERAMIC_SHADE);
                    c.lit(side * 6, -5, -1, side * 6 + side * P, -3, 1, COLD);
                }
                c.voxelRing(0, -11.5, 0, 1, 6, COLD, true);
            }
            case TORSO -> {
                c.box(-5, -1, -3, 5, 7, 3, CERAMIC);
                c.box(-4, 7, -3, 4, 10, 3, CERAMIC_SHADE);
                c.flat(-3, 0, -3 - P, 3, 6, -3, SEAM);
                for (int side = -1; side <= 1; side += 2) c.flat(side * 4, 0, -3 - P, side * 4 + side, 6, -3, CERAMIC_SHADE);
                c.flat(-4, 8, -3 - P, 4, 9, -3, SEAM);
                // Field coil over the sternum, drawn in whole pixels.
                c.lit(-2, 1, -3 - 2 * P, 2, 2, -3 - P, COLD);
                c.lit(-2, 4, -3 - 2 * P, 2, 5, -3 - P, COLD);
                c.lit(-2, 2, -3 - 2 * P, -1, 4, -3 - P, COLD);
                c.lit(1, 2, -3 - 2 * P, 2, 4, -3 - P, COLD);
                // Cryostat pack: the cold has to come from somewhere.
                c.box(-3, 0, 3, 3, 8, 5, CERAMIC_SHADE);
                c.flat(-2, 1, 5, 2, 7, 5 + P, SEAM);
                for (int side = -1; side <= 1; side += 2) c.lit(side - 0.5, 2, 5 + P, side + 0.5, 6, 5 + 2 * P, COLD);
            }
            case ARM_R -> meissnerArm(c, -1);
            case ARM_L -> meissnerArm(c, 1);
            case WAIST -> {
                c.box(-5, 10, -3, 5, 12, 3, SEAM);
                c.flat(-2, 10, -3 - P, 2, 12, -3, CERAMIC_SHADE);
                c.lit(-1, 10.5, -3 - 2 * P, 1, 11.5, -3 - P, COLD);
            }
            case LEG_R -> meissnerLeg(c, -1);
            case LEG_L -> meissnerLeg(c, 1);
            case BOOT_R, BOOT_L -> meissnerBoot(c, s);
        }
    }

    private static void meissnerArm(Canvas c, int side) {
        double x0 = armMin(side), x1 = armMax(side), mid = (x0 + x1) / 2;
        c.box(x0 - 1, -3, -3, x1 + 1, 2, 3, CERAMIC);
        c.box(x0 - 0.5, 2, -2.5, x1 + 0.5, 5, 2.5, SEAM);
        c.box(x0 - 1, 5, -3, x1 + 1, 11, 3, CERAMIC);
        c.flat(x0, 7, -3 - P, x1, 8, -3, CERAMIC_SHADE);
        c.voxelRing(mid, 8.5, 0, 1, 5, COLD, true);
    }

    private static void meissnerLeg(Canvas c, int side) {
        c.box(-2.5, 0, -2.5, 2.5, 5, 2.5, CERAMIC);
        c.box(-2.5, 5, -2.5, 2.5, 8, 2.5, SEAM);
        c.box(-2.5, 5, -3.5, 2.5, 7, -2.5, CERAMIC_SHADE);
        c.lit(-1.5, 5.5, -3.5 - P, 1.5, 6.5, -3.5, COLD);
        c.flat(side * 2.5, 1, -1, side * 2.5 + side * P, 4, 1, CERAMIC_SHADE);
    }

    private static void meissnerBoot(Canvas c, State s) {
        c.box(-3, 8, -3, 3, 11, 3, CERAMIC);
        c.box(-3, 11, -4, 3, 12, 3, SEAM);
        c.flat(-2, 9, -3 - P, 2, 10, -3, CERAMIC_SHADE);
        c.lit(-1, 12, -2, 1, 12 + P, 1, COLD);
        // A levitation ring that opens out while the flux lattice is pinned.
        c.voxelRing(0, 13.5, 0, 1, s.active ? 4 : 3, COLD, true);
    }

    // ---- Degeneracy ----------------------------------------------------------------------------------------------

    private static final int PLATE = 0x7A7065, PLATE_LIGHT = 0x9C8F7E, PLATE_DARK = 0x4A423B, RIVET = 0xC2AE8C;

    private static int hot(State s, float t) {
        float wave = 0.85F + 0.15F * (float) Math.sin(t * 0.25);
        float heat = Math.min(1, s.load * wave);
        int r = (int) (205 + 50 * heat), g = (int) (112 + 100 * heat), b = (int) (28 + 130 * heat * heat);
        return (r << 16) | (g << 8) | b;
    }

    /** A column of plates, each {@code pitch} pixels: alternately inset, with a one-pixel seam of light between them. */
    private static void stack(Canvas c, double x0, double y0, double z0, double x1, double z1, int plates, int pitch, int hot) {
        for (int i = 0; i < plates; i++) {
            double inset = i % 2, top = y0 + i * pitch, bottom = top + pitch - 1;
            c.box(x0 + inset, top, z0 + inset, x1 - inset, bottom, z1 - inset, i % 2 == 0 ? PLATE : PLATE_LIGHT);
            if (i < plates - 1) {
                c.box(x0 + 1, bottom, z0 + 1, x1 - 1, bottom + 1, z1 - 1, PLATE_DARK);
                c.lit(x0 + 2, bottom, z0 + 1 - P, x1 - 2, bottom + 1, z0 + 1, hot);
            }
        }
    }

    private static void degeneracy(Canvas c, Piece piece, State s) {
        int hot = hot(s, c.t);
        switch (piece) {
            case HELMET -> {
                stack(c, -5, -9, -5, 5, 5, 2, 3, hot);
                c.box(-5, -3, -5, 5, 0, 5, PLATE_DARK);
                c.flat(-4, -6, -5 - P, 4, -3, -5, PLATE_DARK);
                c.lit(-3, -5, -5 - 2 * P, 3, -4, -5 - P, hot);
                c.box(-1, -10, -3, 1, -9, 3, PLATE_LIGHT);
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 5, -7, -2, side * 6, -1, 2, PLATE);
                    c.flat(side * 3, -2, -5 - P, side * 3 + side, -1, -5, RIVET);
                }
            }
            case TORSO -> {
                stack(c, -5, -1, -3, 5, 3, 4, 3, hot);
                c.box(-2, 0, -4, 2, 8, -3, PLATE_DARK);
                c.lit(-1, 1, -4 - P, 1, 7, -4, hot);
                // Dorsal manifold: the gauge of what the plates are holding back.
                c.box(-3, 0, 3, 3, 9, 5, PLATE_DARK);
                for (int i = 0; i < 4; i++) c.lit(-2, 1 + i * 2, 5, 2, 2 + i * 2, 5 + P, i < s.load * 4 ? hot : 0x5A3418);
            }
            case ARM_R -> degeneracyArm(c, -1, hot);
            case ARM_L -> degeneracyArm(c, 1, hot);
            case WAIST -> {
                c.box(-5, 10, -3, 5, 12, 3, PLATE_DARK);
                for (int x = -3; x <= 2; x += 5) c.flat(x, 10, -3 - P, x + 1, 12, -3, RIVET);
                c.flat(-1, 10, -3 - P, 1, 12, -3, RIVET);
            }
            case LEG_R, LEG_L -> degeneracyLeg(c, hot);
            case BOOT_R, BOOT_L -> degeneracyBoot(c, hot);
        }
    }

    private static void degeneracyArm(Canvas c, int side, int hot) {
        double x0 = armMin(side), x1 = armMax(side);
        // Pauldron: the widest thing on the suit.
        stack(c, x0 - (side < 0 ? 2 : 1), -4, -4, x1 + (side > 0 ? 2 : 1), 4, 2, 3, hot);
        c.box(x0 - 0.5, 2, -2.5, x1 + 0.5, 5, 2.5, PLATE_DARK);
        stack(c, x0 - 1, 5, -3, x1 + 1, 3, 2, 3, hot);
    }

    private static void degeneracyLeg(Canvas c, int hot) {
        stack(c, -3, 0, -3, 3, 3, 2, 3, hot);
        c.box(-2.5, 6, -2.5, 2.5, 8, 2.5, PLATE_DARK);
        c.box(-3, 5, -4, 3, 7, -3, PLATE_LIGHT);
        c.lit(-2, 5.5, -4 - P, 2, 6.5, -4, hot);
    }

    private static void degeneracyBoot(Canvas c, int hot) {
        c.box(-3, 8, -3, 3, 11, 3, PLATE);
        c.box(-3, 11, -4, 3, 12, 3, PLATE_DARK);
        c.flat(-2, 9, -3 - P, 2, 10, -3, PLATE_LIGHT);
        c.lit(-2, 12, -3, 2, 12 + P, 2, hot);
    }

    // ---- Collapsar -----------------------------------------------------------------------------------------------

    private static final int VOID = 0x3A3648, VOID_EDGE = 0x565068, IVORY = 0xEFE0B6, JET = 0xFFD28A;

    private static int phaseColor(int phase) {
        return switch (phase) {
            case Rules.PHASE_GNOCCHI -> 0xFFB061;
            case Rules.PHASE_SPAGHETTI -> 0xFFE2A0;
            default -> 0xFFFFFF;
        };
    }

    /** The lining seen through the chest, in whole pixels: separate grains, then strands, then continuous sheets. */
    private static void pasta(Canvas c, int x0, int y0, int x1, int y1, double z, int phase) {
        int color = phaseColor(phase);
        switch (phase) {
            case Rules.PHASE_GNOCCHI -> {
                for (int y = y0; y < y1; y += 2) for (int x = x0; x < x1; x += 2) c.lit(x, y, z - P, x + 1, y + 1, z, color);
            }
            case Rules.PHASE_SPAGHETTI -> {
                for (int x = x0; x < x1; x += 2) c.lit(x, y0, z - P, x + 1, y1, z, color);
            }
            default -> {
                for (int y = y0; y < y1; y += 2) c.lit(x0, y, z - P, x1, y + 1, z, color);
            }
        }
    }

    private static void collapsar(Canvas c, Piece piece, State s) {
        int glow = phaseColor(s.phase);
        switch (piece) {
            case HELMET -> {
                c.box(-5, -9, -5, 5, -3, 5, VOID);
                c.box(-4, -10, -4, 4, -9, 4, VOID_EDGE);
                c.box(-5, -3, -5, 5, 0, 5, VOID_EDGE);
                c.flat(-4, -6, -5 - P, 4, -3, -5, IVORY);
                c.flat(-3, -5, -5 - 2 * P, 3, -4, -5 - P, 0x14121C);
                c.lit(-3, -5, -5 - 3 * P, 3, -4, -5 - 2 * P, 0xFFF8EA);
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 5, -8, -1, side * 6, -2, 1, IVORY);
                    c.flat(side * 2 - (side < 0 ? 0 : 1), -9, -5 - P, side * 2 + (side < 0 ? 1 : 0), -6, -5, IVORY);
                }
                c.voxelRing(0, -11.5, 0, 1, 5, IVORY, true);
            }
            case TORSO -> {
                c.box(-5, -1, -3, 5, 8, 3, VOID);
                c.box(-4, 8, -3, 4, 10, 3, VOID_EDGE);
                c.flat(-3, 4, -3 - P, 3, 8, -3, 0x14121C);
                pasta(c, -3, 4, 3, 8, -3 - P, s.phase);
                for (int side = -1; side <= 1; side += 2) c.flat(side * 4, -1, -3 - P, side * 4 + side, 8, -3, IVORY);
                c.flat(-4, 8, -3 - P, 4, 9, -3, IVORY);
                // A small contained hole at the sternum, framed in ivory above its pasta lining: an even-width plus, centred on the body.
                c.box(-3, -1, -4, 3, 4, -3, IVORY);
                c.pose.pushPose();
                c.pose.translate(0, 1.5, -5.4);
                c.faceCamera();
                c.blackHole(1.25, 2.2, 0.95F);
                c.pose.popPose();
                // The main hole rides between the shoulder blades, its disk turning edge-on to the spine.
                c.box(-2, 1, 3, 2, 5, 4, VOID_EDGE);
                c.pose.pushPose();
                c.pose.translate(0, 3, 7.5);
                c.faceCamera();
                c.pose.mulPose(Axis.ZP.rotationDegrees(-16));
                c.blackHole(2.3, 3.4, 1);
                c.pose.popPose();
                // Polar nozzles either side of it, where Jet Burst leaves the suit.
                for (int side = -1; side <= 1; side += 2) {
                    c.box(side * 3, 0, 3, side * 5, 6, 5, VOID_EDGE);
                    c.box(side * 3, 6, 3, side * 5, 7, 5, IVORY);
                    c.lit(side * 3 + side * 0.5, 7, 3.5, side * 5 - side * 0.5, 7 + P, 4.5, glow);
                }
            }
            case ARM_R -> collapsarArm(c, -1, glow);
            case ARM_L -> collapsarArm(c, 1, glow);
            case WAIST -> {
                c.box(-5, 10, -3, 5, 12, 3, VOID_EDGE);
                c.flat(-2, 10, -3 - P, 2, 12, -3, IVORY);
                c.lit(-1, 10.5, -3 - 2 * P, 1, 11.5, -3 - P, glow);
            }
            case LEG_R -> collapsarLeg(c, -1, glow);
            case LEG_L -> collapsarLeg(c, 1, glow);
            case BOOT_R, BOOT_L -> collapsarBoot(c, glow);
        }
    }

    private static void collapsarArm(Canvas c, int side, int glow) {
        double x0 = armMin(side), x1 = armMax(side), outer = side < 0 ? x0 - 1 : x1 + 1;
        c.box(x0 - 1, -3, -3, x1 + 1, 2, 3, VOID);
        c.box(outer, -2, -2, outer + side, 1, 2, IVORY);
        c.box(x0 - 0.5, 2, -2.5, x1 + 0.5, 5, 2.5, VOID_EDGE);
        c.box(x0 - 1, 5, -3, x1 + 1, 11, 3, VOID);
        c.flat(x0 - 1, 10, -3 - P, x1 + 1, 11, -3, IVORY);
        c.lit(outer, 6, -1, outer + side * P, 9, 1, glow);
    }

    private static void collapsarLeg(Canvas c, int side, int glow) {
        c.box(-3, 0, -3, 3, 5, 3, VOID);
        c.box(-2.5, 5, -2.5, 2.5, 8, 2.5, VOID_EDGE);
        c.box(-3, 5, -4, 3, 7, -3, VOID);
        c.flat(-2, 5, -4 - P, 2, 7, -4, IVORY);
        c.lit(side * 3, 1, -1, side * 3 + side * P, 4, 1, glow);
    }

    private static void collapsarBoot(Canvas c, int glow) {
        c.box(-3, 8, -3, 3, 11, 3, VOID);
        c.box(-3, 11, -4, 3, 12, 3, VOID_EDGE);
        c.flat(-3, 11, -4 - P, 3, 11.5, -4, IVORY);
        c.flat(-2, 9, -3 - P, 2, 10, -3, IVORY);
        c.lit(-2, 12, -3, 2, 12 + P, 2, glow);
    }
}
