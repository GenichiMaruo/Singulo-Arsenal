package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.core.Rules;

/**
 * Weapons in item space, in pixels. Swords: Y up, blade along +Y, edge on +X. The lance: +Y toward the muzzle, +Z up, grips hanging
 * toward -Z. {@code use} runs 0..1 while the use key is held. Details stay on a half-pixel grid: finer than the suits, but no
 * hairlines, and every part is something a person would need: a grip to hold, a guard to stop a hand, a housing for what is inside.
 */
public final class WeaponArt {
    /** Where the lance's beam leaves it, on its +Y axis. */
    public static final double LANCE_MUZZLE = 23;

    private static final int VOID = 0x3A3648, VOID_EDGE = 0x565068, VOID_DEEP = 0x262430, IVORY = 0xF2E4BC, THREAD = 0xFFE2A0, HOT = 0xA8E4FF, WHITE_HOT = 0xF4FBFF;

    private WeaponArt() {}

    /** A grip with raised finger ridges, ending in a heavier pommel that keeps the hand from sliding off. */
    private static void grip(Canvas c, double y0, double y1, double half, int core, int ridge, int pommel) {
        c.box(-half, y0, -half, half, y1, half, core);
        for (double y = y0 + 1; y < y1 - 0.5; y += 1.5) c.box(-half - 0.25, y, -half - 0.25, half + 0.25, y + 0.5, half + 0.25, ridge);
        c.box(-half - 0.5, y0 - 1, -half - 0.5, half + 0.5, y0, half + 0.5, pommel);
    }

    // ---- Quench saber: a ceramic spine carrying a bare superconducting edge, cooled from a cryostat in the hilt -----------

    public static void quench(Canvas c, float use, float current, float reload) {
        int cold = current >= 0.8F ? 0xFFFFFF : 0x7CE8FF;
        // After a quench the conductor is normal metal again; superconductivity comes back from the cold end up.
        double conductor = reload >= 0 ? -1 + 12 * reload : 11;
        double collar = reload >= 0 ? 0.5 * Math.sin(reload * Math.PI) : 0;
        int ceramic = 0xF4F6FA, shade = 0xC2CDD9, dark = 0x3A4656;
        grip(c, -8, -3.5, 0.75, dark, shade, ceramic);
        // The pommel is the power cell; its strip shows how close the blade is to quenching.
        c.lit(-1, -8.75, -1.375, 1, -8.25, -1.25, cold);
        c.lit(-1, -8.75, 1.25, 1, -8.25, 1.375, cold);
        // Cryostat collar between hand and blade.
        c.box(-1.5, -3.5 - collar, -1.25, 1.5, -3 - collar, 1.25, shade);
        if (collar > 0) c.softBand(0, -3.5, -3, 1.3, 2.5, 0xCFF4FF, (float) collar);
        c.box(-3, -3, -1, 3, -2, 1, ceramic);
        c.box(-2, -2, -0.75, 2, -1.5, 0.75, shade);
        for (double z : new double[]{-1.125, 1}) c.lit(-1.5, -2.75, z, 1.5, -2.25, z + 0.125, cold);
        // Blade: a spine in coil sections, tapering in three steps.
        c.box(-1, -1.5, -0.5, 1, 11, 0.5, ceramic);
        c.box(-0.75, 11, -0.5, 0.75, 12, 0.5, ceramic);
        c.box(-0.5, 12, -0.25, 0.5, 13, 0.25, ceramic);
        for (int i = 0; i < 4; i++) c.box(-1.125, 1 + i * 2.75, -0.625, 1.125, 1.5 + i * 2.75, 0.625, dark);
        for (int side = -1; side <= 1; side += 2) {
            if (conductor > -1) c.lit(side, -1, -0.25, side * 1.5, conductor, 0.25, cold);
            if (conductor >= 11) c.lit(side * 0.75, 11, -0.25, side * 1.25, 12, 0.25, cold);
        }
        float alpha = 0.14F + 0.5F * current + use * 0.2F;
        for (int side = -1; side <= 1; side += 2) {
            c.softBand(side * 1.25, -1, conductor, 0.3, 1.25, cold, alpha);
            c.softBand(side * 1.25, -1, conductor, -0.3, 1.25, cold, alpha);
        }
    }

    // ---- Casimir blade: two mirrors on actuated rails, and the blade is the nothing between them ---------------------------

    public static void casimir(Canvas c, float use, boolean armed, float t) {
        float closing = armed ? 1 : use;
        double gap = 1.25 - closing * 1;
        int mirror = 0xDCE6F2, frame = 0x4A4E62, vacuum = armed ? 0xFFFFFF : 0xB8A8FF;
        grip(c, -9, -4, 0.75, 0x2C2E3C, 0x7A8096, mirror);
        // Actuator housing: this is what moves the mirrors when the use key is held.
        c.box(-3.5, -4, -1.25, 3.5, -2.5, 1.25, frame);
        c.box(-2.5, -4.5, -1, 2.5, -4, 1, mirror);
        for (double z : new double[]{-1.375, 1.25}) c.lit(-2.5, -3.5, z, 2.5, -3, z + 0.125, vacuum);
        // Guide rail the mirror carriers ride on, visible where the gap closes.
        c.box(-3, -2.5, -0.5, 3, -2, 0.5, 0x7A8096);
        for (int side = -1; side <= 1; side += 2) {
            double near = side * gap, far = side * (gap + 0.5), outer = side * (gap + 1);
            c.box(side * gap, -2.5, -1, outer, -1.5, 1, frame);
            c.box(near, -1.5, -1, far, 12, 1, mirror);
            c.box(far, -1.5, -0.5, outer, 11.5, 0.5, frame);
            c.box(near, 12, -0.5, far, 13.5, 0.5, mirror);
            // Clamps that hold each mirror flat against its backing.
            for (int i = 0; i < 3; i++) c.box(far, 1 + i * 4, -1, outer + side * 0.125, 2 + i * 4, 1, frame);
            double sheen = -1.5 + ((t * 0.18 + side * 3) % 11.5 + 11.5) % 11.5;
            for (double z : new double[]{-1.125, 1}) c.lit(near, sheen, z, far, Math.min(12, sheen + 2), z + 0.125, 0xFFFFFF);
        }
        float glow = (float) ((Math.sin(t * 0.9) * 0.1 + 0.9) * (0.45 + closing * 0.5));
        c.lit(-0.125, -1.5, -0.75, 0.125, 13.5, 0.75, vacuum);
        c.softBand(0, -1.5, 13.5, 0.8, gap + 0.25, vacuum, glow);
        c.softBand(0, -1.5, 13.5, -0.8, gap + 0.25, vacuum, glow);
    }

    // ---- Tidal reaver: a two-handed cleaver, bound in filament, its tidal focus housed in the spine ------------------------

    public static void reaver(Canvas c, float use, float reload, float t) {
        grip(c, -11, -3.5, 0.75, VOID_DEEP, IVORY, IVORY);
        c.box(-4, -3.5, -1.5, 4.5, -2.5, 1.5, VOID);
        c.box(-4, -2.5, -1.5, 4.5, -2, 1.5, IVORY);
        // Stepped cleaver, broad at the tip, cutting edge on +X, with a ground bevel toward it.
        double[][] steps = {{-2, 3, 2}, {3, 8, 3}, {8, 12.5, 4.5}, {12.5, 14.5, 3.5}};
        for (double[] step : steps) {
            if (step[0] == 3) {
                // This section is cut through for the chamber the hole sits in.
                c.box(-2, 3, -0.75, -1.25, 8, 0.75, VOID);
                c.box(1.25, 3, -0.75, step[2], 8, 0.75, VOID);
                c.box(-1.25, 3, -0.75, 1.25, 4.25, 0.75, VOID);
                c.box(-1.25, 7.75, -0.75, 1.25, 8, 0.75, VOID);
            } else {
                c.box(-2, step[0], -0.75, step[2], step[1], 0.75, VOID);
            }
            c.box(step[2] - 1, step[0] + 0.125, -0.875, step[2] - 0.125, step[1] - 0.125, 0.875, VOID_EDGE);
            c.lit(step[2], step[0], -0.25, step[2] + 0.5, step[1], 0.25, THREAD);
            c.softBand(step[2] + 0.25, step[0], step[1], 0.3, 1, THREAD, 0.3F + use * 0.4F);
            c.softBand(step[2] + 0.25, step[0], step[1], -0.3, 1, THREAD, 0.3F + use * 0.4F);
        }
        c.box(-2.5, -2, -1, -2, 14, 1, IVORY);
        c.box(-2, 14.5, -0.75, 1.5, 15, 0.75, VOID);
        // Filament bindings holding the focus housing to the blade.
        for (double y : new double[]{1, 10.5, 13}) {
            for (double z : new double[]{-1, 0.875}) c.lit(-2, y, z, 2, y + 0.5, z + 0.125, THREAD);
        }
        // A window through the middle of the blade on both faces, framed in ivory, with the hole held in the chamber behind the glass.
        for (int side = -1; side <= 1; side += 2) {
            double near = side * 0.75, far = side;
            c.box(-1.75, 3.75, near, -1.25, 8.25, far, IVORY);
            c.box(1.25, 3.75, near, 1.75, 8.25, far, IVORY);
            c.box(-1.25, 7.75, near, 1.25, 8.25, far, IVORY);
            c.box(-1.25, 3.75, near, 1.25, 4.25, far, IVORY);
            // The pane's edges run inside the frame, so none of them shares a plane with it.
            c.glass(-1.375, 4.125, side * 0.8125, 1.375, 7.875, side * 0.9375, 0xFFE2A0, 0.12F);
        }
        c.pose.pushPose();
        c.pose.translate(0, 6, 0);
        c.faceCamera();
        c.pose.mulPose(Axis.ZP.rotationDegrees(-16));
        // A shear spends the focus; it re-forms from nothing during the cooldown.
        float formed = reload >= 0 ? Math.min(1, reload * 1.15F) : 1;
        c.blackHole((0.55 + use * 0.25) * formed, 2, (0.75F + use * 0.25F) * formed);
        c.pose.popPose();
    }

    // ---- Jet lance: a shouldered gun. A magnetic mirror seals the rear pole, so everything leaves through the barrel ---------

    /** {@code handleSide} is the X side of the charging handle, always toward the support hand so the hand never has to cross the gun. */
    public static void lance(Canvas c, float ignition, float reload, float t, int handleSide) {
        boolean reloading = reload >= 0;
        int status = ignition > 0 ? WHITE_HOT : reloading ? 0xFFB061 : 0x6A88A8;
        float vent = reloading ? Rules.reloadVent(reload) : 0;
        float cooling = reloading ? (1 - reload) * (1 - reload) : 0;
        boolean spinning = ignition > 0 || cooling > 0.3F;
        // Eight-sided, so the rings read as machined parts at this scale.
        c.coarse(8);
        // Short skeleton stock and butt pad, so the recoil of a one-sided jet has somewhere to go.
        c.box(-0.75, -6.5, 1.5, 0.75, -4, 2.5, VOID);
        c.box(-0.75, -6.5, -3, 0.75, -4, -2, VOID);
        c.box(-1.5, -8, -5, 1.5, -6.5, 4, VOID_EDGE);
        c.box(-1.25, -8.5, -4.5, 1.25, -8, 3.5, VOID_DEEP);
        c.box(-1.25, -6.25, 2.5, 1.25, -4.5, 3.5, IVORY);

        // Rear pole: a stack of magnetic mirror coils that closes this end of the hole.
        c.box(-2.5, -4, -2.5, 2.5, -2.5, 2.5, VOID_DEEP);
        c.hoop(0, -3.75, 0, 1, 3, 0.5, VOID_EDGE);
        // The facet between the shoulder and the hole is left out, or this ring would hide it from whoever holds the gun.
        int sightFacet = handleSide < 0 ? 2 : 0;
        c.hoop(0, -2.75, 0, 1, 3, 0.5, IVORY, sightFacet, sightFacet);
        c.litHoop(0, -4.125, 0, 1, 1.5, 0.25, status);

        // Containment chamber, open at the sides so the hole can be seen: a top and a bottom plate and four posts.
        // The top plate lifts on its posts to vent the chamber after a burst.
        // It is a frame round a window running to the rear edge: seen from the shoulder, the hole lies just behind the plate's middle.
        double lid = 2.5 + vent * 1.5;
        c.box(-3.5, 5.25, lid, 3.5, 7, lid + 1, VOID);
        for (int side = -1; side <= 1; side += 2) c.box(side * 2.25, -2.5, lid, side * 3.5, 5.25, lid + 1, VOID);
        // The pane runs into the frame on three sides and stops short of the open end, so none of its edges lies in a plane of the frame.
        c.glass(-2.5, -2.375, lid + 0.25, 2.5, 5.5, lid + 0.75, 0x9CC8FF, 0.16F);
        if (vent > 0) {
            for (int side = -1; side <= 1; side += 2) c.box(side * 3, 0, 2.5, side * 3.5, 4, 2.5 + vent * 1.5, VOID_EDGE);
            c.softBand(0, -1, 6, 3.2, 4, 0xFFB061, vent * 0.45F);
            c.softBand(0, -1, 6, 3.9, 4, 0xFFFFFF, vent * 0.2F);
        }
        c.box(-3.5, -2.5, -3.5, 3.5, 7, -2.5, VOID);
        for (double y : new double[]{-2.5, 6}) {
            for (int side = -1; side <= 1; side += 2) c.box(side * 2.5, y, -2.5, side * 3.5, y + 1, 2.5, VOID_EDGE);
        }
        // Open across the top and the rear of the top, along the line of sight through the window.
        c.hoop(0, 2.25, 0, 0, 2.75, 0.25, IVORY, 1, 2);
        c.pose.pushPose();
        c.pose.translate(0, 2.25, 0);
        c.faceCamera();
        c.pose.mulPose(Axis.ZP.rotationDegrees(-14));
        c.blackHole(1.5, 1.8 + ignition * 0.5, 0.8F + ignition * 0.2F);
        c.pose.popPose();

        // Throat, barrel and three collimation rings stepping down toward the muzzle, as on the Jet Forge.
        c.box(-2.5, 7, -2.5, 2.5, 8.5, 2.5, VOID_EDGE);
        c.box(-1, 8.5, -1, 1, 21, 1, VOID);
        for (int side = -1; side <= 1; side += 2) c.box(side * 0.5 - 0.25, 8.5, side * 2 - 0.25, side * 0.5 + 0.25, 17.5, side * 2 + 0.25, VOID_EDGE);
        double[][] rings = {{11, 3}, {15, 2.5}, {19, 2}};
        for (int i = 0; i < rings.length; i++) {
            double y = rings[i][0], radius = rings[i][1];
            c.box(-1.5, y - 1, -1.5, 1.5, y + 1, 1.5, VOID_EDGE);
            c.hoop(0, y, 0, 1, radius, 0.5, IVORY);
            // Guide vanes that turn with the field: they only move while the ring is energised or still running down.
            c.pose.pushPose();
            c.pose.translate(0, y, 0);
            if (spinning) c.pose.mulPose(Axis.YP.rotationDegrees((i % 2 == 0 ? 1 : -1) * t * 11));
            for (int k = 0; k < 3; k++) {
                c.pose.mulPose(Axis.YP.rotationDegrees(120));
                // Thicker than the lit band they cross, so their faces never share its plane.
                c.box(radius - 1, -0.375, -0.5, radius + 1, 0.375, 0.5, VOID_EDGE);
            }
            c.pose.popPose();
            boolean energised = ignition > (i + 1) / 4F;
            // After firing the rings are still hot, and cool from orange to nothing.
            int ring = energised ? WHITE_HOT : cooling > 0.04F ? 0xFF8A3C : 0x4A5A70;
            // A lit band round the outside of the ring.
            c.litHoop(0, y, 0, 1, radius + 0.5, 0.25, ring);
            if (energised) c.halo(0, y, 0, 1, radius, 1, HOT, 0.55F);
            else if (cooling > 0.04F) c.halo(0, y, 0, 1, radius, 1, 0xFF8A3C, cooling * 0.5F);
        }
        // Heat fins between the rings.
        for (double y : new double[]{12.75, 13.75, 16.75, 17.75}) {
            for (int side = -1; side <= 1; side += 2) c.box(side, y, -0.75, side * 2, y + 0.5, 0.75, VOID_DEEP);
        }
        // Muzzle crown with the aperture the jet leaves through.
        c.box(-1.5, 21, -1.5, 1.5, 23, 1.5, VOID_EDGE);
        c.litHoop(0, 23.125, 0, 1, 1.0, 0.25, status);
        c.box(-0.5, 23, -0.5, 0.5, 23.125, 0.5, VOID_DEEP);

        // Sight rail ahead of the window, and an optic with a lit reticle on it.
        c.box(-0.5, 5.25, 3.5, 0.5, 9, 4, VOID_EDGE);
        c.box(-1.25, 6, 4, -0.75, 8.5, 6, VOID);
        c.box(0.75, 6, 4, 1.25, 8.5, 6, VOID);
        c.box(-1.25, 6, 6, 1.25, 8.5, 6.5, VOID);
        c.lit(-0.25, 7, 4.75, 0.25, 7.05, 5.25, 0x7CFFB0);

        // Rear pistol grip, raked back the way a wrist sits, with a trigger inside a guard.
        c.pose.pushPose();
        c.pose.translate(0, -0.5, -3.5);
        c.pose.mulPose(Axis.XP.rotationDegrees(14));
        c.box(-1, -1.25, -5, 1, 1.25, 0, VOID_DEEP);
        for (int i = 0; i < 3; i++) c.box(-1.25, 1.25, -1.5 - i * 1.5, 1.25, 1.75, -1 - i * 1.5, IVORY);
        c.box(-1.25, -1.5, -5.5, 1.25, 1.5, -5, VOID_EDGE);
        c.pose.popPose();
        c.box(-0.5, 1.5, -5.5, 0.5, 5.5, -5, VOID_EDGE);
        c.box(-0.5, 5, -5, 0.5, 5.5, -3.5, VOID_EDGE);
        c.box(-0.25, 2.5, -5, 0.25, 3, -3.5, IVORY);

        // Accretion feed: the magazine that mass pellets are drawn from, with a charge window.
        c.box(-2, 5, -3.625, 2, 9.5, -3, VOID_EDGE);
        if (!reloading || !Rules.reloadMagazineGone(reload)) {
            float out = reloading ? Rules.reloadMagazine(reload) : 0;
            c.pose.pushPose();
            c.pose.translate(0, 7.25, -3.5 - out * 6);
            c.pose.mulPose(Axis.XP.rotationDegrees(out * 16));
            c.box(-1.5, -1.75, -4, 1.5, 1.75, 0, VOID);
            c.box(-1.75, -1.75, -4.25, 1.75, 1.75, -4, VOID_EDGE);
            for (int side = -1; side <= 1; side += 2) c.lit(side * 1.5, -0.75, -3.5, side * 1.625, 0.75, -0.5, status);
            c.pose.popPose();
        }

        // Charging handle on the support-hand side of the receiver: racked once to seat the new feed.
        double handle = 5 - (reloading ? Rules.reloadCock(reload) * 4 : 0);
        c.box(handleSide * 3.5, 0, 0.5, handleSide * 4, 6, 1.5, VOID_EDGE);
        c.box(handleSide * 4, handle, 0.25, handleSide * 5, handle + 1, 1.75, IVORY);

        // Forward grip for the support hand.
        c.pose.pushPose();
        c.pose.translate(0, 12.5, -1.5);
        c.pose.mulPose(Axis.XP.rotationDegrees(-10));
        c.box(-0.75, -0.75, -5, 0.75, 0.75, 0, VOID_DEEP);
        for (int i = 0; i < 3; i++) c.box(-1, 0.75, -1.25 - i * 1.25, 1, 1.25, -0.75 - i * 1.25, IVORY);
        c.box(-1, -1, -5.5, 1, 1, -5, VOID_EDGE);
        c.pose.popPose();
    }
}
