package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.math.Axis;

/** The Module Integrator and bit drones. Centred on the origin, pixels, Y up; the machine fills one block. */
public final class MachineArt {
    private static final int FRAME = 0x3A3648, FRAME_LIGHT = 0x565068, DEEP = 0x262430, IVORY = 0xF2E4BC, COLD = 0x7CE8FF, WHITE = 0xF4FBFF;

    private MachineArt() {}

    /** {@code work} is 0..1 during a run and -1 at rest. */
    public static void integrator(Canvas c, float work, float t) {
        boolean running = work >= 0;
        // Plinth, with the service panel and its status light at the front.
        c.box(-8, -8, -8, 8, -5, 8, FRAME);
        c.box(-7, -5, -7, 7, -4, 7, FRAME_LIGHT);
        c.box(-3, -7, -8.25, 3, -6, -8, DEEP);
        c.lit(-2, -6.75, -8.3, 2, -6.25, -8.25, running ? WHITE : COLD);
        // Four posts that carry the upper ring, with the work bed open between them.
        for (int x = -1; x <= 1; x += 2) {
            for (int z = -1; z <= 1; z += 2) {
                c.box(x * 5.5, -4, z * 5.5, x * 7.5, 7, z * 7.5, DEEP);
                c.box(x * 5.5, 2, z * 7.5, x * 7.5, 3, z * 7.75, IVORY);
                c.box(x * 7.5, 2, z * 5.5, x * 7.75, 3, z * 7.5, IVORY);
            }
        }
        // Bed: a stepped pedestal the gear hovers over.
        c.box(-4, -4, -4, 4, -2.5, 4, IVORY);
        c.box(-3, -2.5, -3, 3, -1.5, 3, FRAME_LIGHT);
        c.litHoop(0, -1.45, 0, 1, 2.5, 0.25, running ? WHITE : COLD);
        c.halo(0, -1.4, 0, 1, 2.5, 1.25, COLD, running ? 0.7F : 0.3F);
        // Upper ring tying the posts together.
        c.box(-8, 7, -8, 8, 8, -5.5, FRAME);
        c.box(-8, 7, 5.5, 8, 8, 8, FRAME);
        c.box(-8, 7, -5.5, -5.5, 8, 5.5, FRAME);
        c.box(5.5, 7, -5.5, 8, 8, 5.5, FRAME);
        // Scan ring: travels up and down the piece during a run, parked at the top otherwise.
        double scan = running ? 1 + 3.5 * Math.sin(work * Math.PI * 6) : 4.5;
        c.litHoop(0, scan, 0, 1, 4.25, 0.25, running ? WHITE : 0x5FB9D0);
        if (running) c.halo(0, scan, 0, 1, 4.25, 1.5, COLD, 0.6F);
        // Two tool arms that swing in and weld while it runs, folded against the posts at rest.
        for (int side = -1; side <= 1; side += 2) {
            c.pose.pushPose();
            c.pose.translate(side * 6.5, 5.5, 0);
            double swing = running ? 40 + 16 * Math.sin(t * 0.35 + side) : 8;
            c.pose.mulPose(Axis.ZP.rotationDegrees((float) (side * -swing)));
            c.box(-0.75, -5, -0.75, 0.75, 0.5, 0.75, FRAME_LIGHT);
            c.box(-1, -0.5, -1, 1, 0.75, 1, IVORY);
            c.box(-0.5, -6.5, -0.5, 0.5, -5, 0.5, DEEP);
            if (running) c.lit(-0.25, -7, -0.25, 0.25, -6.5, 0.25, WHITE);
            c.pose.popPose();
        }
    }

    /** A bit drone, the shape of a Singulo android's: a slender stepped hull with small wings, nose toward -Z. Pixels, centred. */
    public static void bit(Canvas c, boolean firing) {
        c.box(-0.625, -0.625, -2, 0.625, 0.625, 2, FRAME);
        c.box(-0.375, -0.375, -3.25, 0.375, 0.375, -2, FRAME);
        c.box(-0.125, -0.125, -4.25, 0.125, 0.125, -3.25, IVORY);
        c.box(-1.125, -0.125, 0.25, 1.125, 0.125, 2, FRAME_LIGHT);
        c.box(-0.5, -0.5, 2, 0.5, 0.5, 2.5, DEEP);
        int core = firing ? WHITE : COLD;
        c.lit(-0.25, 0.625, -1.5, 0.25, 0.75, 1, core);
        c.lit(-0.25, -0.25, 2.5, 0.25, 0.25, 2.625, core);
        c.halo(0, 0, 2.75, 2, 0.5, 0.5, COLD, firing ? 0.8F : 0.4F);
    }
}
