package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.math.Axis;

/**
 * Mining tools in item space, in pixels: Y up, handle along -Y, the working end toward +Y. Like the weapons, details stay on a half-pixel
 * grid and every part is something the tool needs. Faces that sit on one another are stepped by at least an eighth of a pixel.
 */
public final class ToolArt {
    private static final int CERAMIC = 0xF4F6FA, SHADE = 0xC2CDD9, DARK = 0x3A4656, COLD = 0x7CE8FF;
    private static final int VOID = 0x3A3648, VOID_EDGE = 0x565068, VOID_DEEP = 0x262430, IVORY = 0xF2E4BC;
    private static final int PLATE = 0x59524A, PLATE_LIGHT = 0x7A7065, AMBER = 0xE08A2E;

    private ToolArt() {}

    // ---- SQUID pick: a ceramic pick with the sensing loops in its socket -------------------------------------------------

    public static void squidPick(Canvas c) {
        WeaponArt.grip(c, -9, -3, 0.75, DARK, SHADE, CERAMIC);
        c.box(-0.625, -3, -0.625, 0.625, 8.5, 0.625, CERAMIC);
        for (double y : new double[]{0, 3.5}) c.box(-0.75, y, -0.75, 0.75, y + 0.5, 0.75, DARK);
        // The socket carries a pickup loop on each face.
        c.box(-1.75, 8.5, -1.25, 1.75, 12, 1.25, SHADE);
        c.box(-1.25, 12, -0.875, 1.25, 12.75, 0.875, DARK);
        c.coarse(8);
        for (int side = -1; side <= 1; side += 2) c.litHoop(0, 10.25, side * 1.4375, 2, 0.75, 0.125, COLD);
        c.halo(0, 10.25, 0, 2, 0.75, 0.75, COLD, 0.25F);
        c.coarse(0);
        // The head falls away to both points in steps, hardest at the tips.
        for (int side = -1; side <= 1; side += 2) {
            c.box(side * 1.75, 9, -0.75, side * 4, 11.5, 0.75, CERAMIC);
            c.box(side * 4, 8.5, -0.625, side * 6, 10.5, 0.625, CERAMIC);
            c.box(side * 6, 7.5, -0.5, side * 7.5, 9.5, 0.5, SHADE);
            c.box(side * 7.5, 6.5, -0.25, side * 8.5, 8.5, 0.25, DARK);
            c.lit(side * 2, 11.5, -0.25, side * 3.75, 11.625, 0.25, COLD);
        }
    }

    // ---- Degeneracy breaker: a short-hafted maul whose head is stacked plate, hot along its seams ----------------------------

    public static void degeneracyBreaker(Canvas c) {
        WeaponArt.grip(c, -11, -3, 1, VOID_DEEP, PLATE_LIGHT, PLATE);
        c.box(-1, -3, -1, 1, 6.5, 1, PLATE);
        c.box(-1.5, 4, -1.5, 1.5, 6.5, 1.5, PLATE_LIGHT);
        // Head: a core with plates pressed onto both striking faces.
        c.box(-4.5, 6.5, -3, 4.5, 14.5, 3, PLATE);
        for (int side = -1; side <= 1; side += 2) {
            c.box(side * 4.5, 7, -2.75, side * 6, 14, 2.75, PLATE_LIGHT);
            c.box(side * 6, 7.5, -2.25, side * 7, 13.5, 2.25, IVORY);
            for (double z : new double[]{-3.125, 3}) c.lit(side * 2.25, 8.75, z, side * 2.75, 12.25, z + 0.125, AMBER);
        }
        // Binding straps round the head.
        for (double y : new double[]{7.5, 12.5}) c.box(-4.625, y, -3.25, 4.625, y + 1, 3.25, VOID_DEEP);
        c.box(-3, 14.5, -2, 3, 15.5, 2, PLATE_LIGHT);
    }

    // ---- Shear saw: a motor and a guide bar, with the chain running round it ------------------------------------------------

    public static void shearSaw(Canvas c, boolean running, float t) {
        WeaponArt.grip(c, -10, -3.5, 0.75, VOID_DEEP, IVORY, VOID_EDGE);
        // Motor housing with cooling fins.
        c.box(-2.75, -3.5, -2, 2.75, 3, 2, VOID);
        for (int i = 0; i < 3; i++) c.box(-3, -2.5 + i * 1.75, -2.25, 3, -2 + i * 1.75, 2.25, VOID_EDGE);
        for (double z : new double[]{-2.375, 2.25}) c.lit(-0.25, -3, z, 0.25, 2.5, z + 0.125, running ? 0xFFFFFF : COLD);
        // Guide bar, narrowing to the nose.
        c.box(-1.5, 3, -0.375, 1.5, 15, 0.375, IVORY);
        c.box(-1, 15, -0.375, 1, 16, 0.375, IVORY);
        c.box(-1.75, 3, -0.625, 1.75, 4.5, 0.625, VOID_EDGE);
        // The chain: teeth up one edge and down the other, moving only under power.
        double travel = running ? t * 0.9 : 0;
        double run = 9;
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 6; i++) {
                // Wrapped along the edge, so there are always six teeth and none pops in or out.
                double y = 4.625 + (((i * 1.5 + side * travel) % run) + run) % run;
                c.box(side * 1.5, y, -0.25, side * 2.25, y + 0.75, 0.25, VOID_DEEP);
            }
        }
    }

    // ---- Accretion drill: a contained hole behind the window, and a stepped bit in front of it -------------------------------

    public static void accretionDrill(Canvas c, boolean running, float t) {
        WeaponArt.grip(c, -11, -5, 0.875, VOID_DEEP, IVORY, VOID_EDGE);
        // The housing is a frame with a chamber through it, glazed on both faces.
        c.box(-3.25, -5, -3, 3.25, -3.75, 3, VOID);
        c.box(-3.25, 2.25, -3, 3.25, 3.5, 3, VOID);
        for (int side = -1; side <= 1; side += 2) {
            c.box(side * 2, -3.75, -3, side * 3.25, 2.25, 3, VOID);
            c.glass(-2.125, -3.875, side * 2.5, 2.125, 2.375, side * 2.75, 0xFFC36B, 0.12F);
            for (double y : new double[]{-4.75, 2.5}) c.lit(-2.5, y, side * 3, 2.5, y + 0.5, side * 3.125, running ? 0xFFFFFF : 0xFFC36B);
        }
        c.pose.pushPose();
        c.pose.translate(0, -0.75, 0);
        c.faceCamera();
        c.pose.mulPose(Axis.ZP.rotationDegrees(-14));
        c.blackHole(0.8, 2, running ? 1F : 0.8F);
        c.pose.popPose();
        // Collar, then a bit that steps down to its point and turns under power.
        c.coarse(8);
        c.hoop(0, 4, 0, 1, 2.75, 0.5, IVORY);
        c.litHoop(0, 4, 0, 1, 3.25, 0.25, running ? 0xFFFFFF : 0xFFC36B);
        c.coarse(0);
        c.pose.pushPose();
        if (running) c.pose.mulPose(Axis.YP.rotationDegrees(t * 40));
        c.box(-2, 3.5, -2, 2, 7, 2, VOID_EDGE);
        c.box(-1.5, 7, -1.5, 1.5, 10.5, 1.5, VOID_EDGE);
        c.box(-1, 10.5, -1, 1, 14, 1, VOID_EDGE);
        c.box(-0.5, 14, -0.5, 0.5, 17, 0.5, IVORY);
        // Flights winding up the bit, a quarter turn per step.
        double[][] flights = {{4.625, 2.5}, {5.875, 2.75}, {8, 2.25}, {10, 2}, {12, 1.5}, {14, 1}};
        for (int i = 0; i < flights.length; i++) {
            c.pose.pushPose();
            c.pose.mulPose(Axis.YP.rotationDegrees(i * 90));
            double y = flights[i][0], reach = flights[i][1];
            c.box(0.5, y, -0.375, reach, y + 1, 0.375, IVORY);
            c.pose.popPose();
        }
        c.pose.popPose();
    }
}
