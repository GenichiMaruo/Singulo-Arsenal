import io.github.genichimaruo.singuloarsenal.core.Rules;

public final class RulesTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // Tanks must conserve energy at every boundary, including nonsense input.
        for (int capacity : new int[]{0, 240_000, Rules.LANCE_CAPACITY}) {
            for (int stored : new int[]{-1, 0, 1, capacity, Integer.MAX_VALUE}) {
                for (int request : new int[]{Integer.MIN_VALUE, -1, 0, 1, 500_000, Integer.MAX_VALUE}) {
                    int taken = Rules.accepted(stored, capacity, request);
                    check(taken >= 0 && taken <= Math.max(0, request), "accepted more than offered");
                    check(Rules.clamp(stored, capacity) + taken <= capacity, "overfilled tank");
                }
            }
        }
        check(Rules.chargeRequest(Rules.LANCE_CAPACITY) == Rules.LANCE_CAPACITY / 8, "large tanks charge by fraction");
        check(Rules.chargeRequest(1000) == 20_000, "small tanks keep a sane minimum");
        check(!Rules.canAccrete(Rules.COLLAPSAR_CAPACITY - Rules.PELLET_FE + 1, Rules.COLLAPSAR_CAPACITY), "pellet wasted overfilling");
        check(Rules.canAccrete(Rules.COLLAPSAR_CAPACITY - Rules.PELLET_FE, Rules.COLLAPSAR_CAPACITY), "exact room refused");

        // Flux pin.
        check(!Rules.shouldPin(true, true, false, -1, 100), "pinned on the ground");
        check(!Rules.shouldPin(false, false, false, -1, 100), "pinned without sneaking");
        check(!Rules.shouldPin(false, true, true, -1, 100), "pinned while flying");
        check(!Rules.shouldPin(false, true, false, 0.2, 100), "pin must not cancel a jump");
        check(!Rules.shouldPin(false, true, false, -1, 0), "pinned without energy");
        check(Rules.shouldPin(false, true, false, -1, 1), "pin refused");
        check(Rules.FLUX_CREEP < -0.03126, "creep must stay above the server's floating check or players get kicked");

        for (int total : new int[]{0, 1, 7, 960_000}) {
            for (int pieces = 1; pieces <= 4; pieces++) {
                int level = Rules.balanced(total, pieces);
                int remainder = total - level * pieces;
                check(remainder >= 0 && remainder < pieces, "balance lost energy");
            }
        }

        // Chandrasekhar limit: the excess may only be converted if it can be paid for.
        check(Rules.pressureFrom(8, 1_000_000) == 0, "hit at the limit is untouched");
        check(Rules.pressureFrom(8.5F, 1_000_000) == 1, "fraction over the limit rounds to a point");
        check(Rules.pressureFrom(30, 1_000_000) == 22, "excess conversion");
        check(Rules.pressureFrom(30, Rules.PRESSURE_FE_PER_HP * 5) == 5, "limited by energy");
        check(Rules.pressureFrom(30, 0) == 0, "free pressure");
        check(Rules.pressureFrom(Float.NaN, 1_000_000) == 0, "NaN pressure");
        check(Rules.pressureFrom(Float.POSITIVE_INFINITY, 1_000_000) == 0, "infinite pressure");
        check(Rules.slamDamage(3.9F) == 0, "small falls do not slam");
        check(Rules.slamDamage(1000) == 30, "slam is bounded");
        check(Rules.slamRadius(1000) <= 7, "slam radius is bounded");
        check(Rules.collapseDamage(Rules.PRESSURE_MAX) == 27, "collapse damage");

        // Pasta phases.
        check(Rules.phaseOf(0) == Rules.PHASE_GNOCCHI && Rules.phaseOf(33) == Rules.PHASE_GNOCCHI, "gnocchi band");
        check(Rules.phaseOf(34) == Rules.PHASE_SPAGHETTI && Rules.phaseOf(66) == Rules.PHASE_SPAGHETTI, "spaghetti band");
        check(Rules.phaseOf(67) == Rules.PHASE_LASAGNA && Rules.phaseOf(100) == Rules.PHASE_LASAGNA, "lasagna band");
        check(Rules.phaseOf(-5) == Rules.PHASE_GNOCCHI && Rules.phaseOf(5000) == Rules.PHASE_LASAGNA, "phase clamps");
        check(Rules.loadFromDamage(1000) == 15, "one hit cannot force lasagna");
        check(Rules.loadFromDamage(Float.NaN) == 0 && Rules.loadFromDamage(0) == 0, "bad load");

        // Horizon: never for survivable hits, /kill, an empty tank or during cooldown.
        check(!Rules.horizonCatches(5, 20, false, Rules.HORIZON_FE, true), "non-lethal hit caught");
        check(!Rules.horizonCatches(50, 20, true, Rules.HORIZON_FE, true), "catches /kill");
        check(!Rules.horizonCatches(50, 20, false, Rules.HORIZON_FE - 1, true), "free horizon");
        check(!Rules.horizonCatches(50, 20, false, Rules.HORIZON_FE, false), "cooldown ignored");
        check(Rules.horizonCatches(20, 20, false, Rules.HORIZON_FE, true), "exactly lethal not caught");
        check(Rules.COLLAPSAR_CAPACITY >= Rules.HORIZON_FE, "horizon must be affordable from one chestpiece");

        // Weapons.
        int current = 0;
        for (int i = 1; i <= 5; i++) current = Rules.currentAfterHit(current);
        check(current == Rules.QUENCH_MAX_CURRENT, "quench on the fifth hit");
        check(Rules.currentAfterHit(Rules.QUENCH_MAX_CURRENT) == Rules.QUENCH_MAX_CURRENT, "current clamps");
        check(Rules.stressAfterHit(Rules.STRESS_MAX) == Rules.STRESS_MAX, "stress clamps");
        check(Rules.stressMultiplier(0) == 1, "no stress, no bonus");
        check(Rules.shearDamage(5) == 66 && Rules.shearDamage(99) == 66, "shear bounded");
        check(Rules.shearDamage(0) == 16, "shear base");
        check(Rules.stretchFor(0, 0) == 1, "stretch rest");

        check(Rules.lanceRadius(0) > Rules.lanceRadius(10), "the jet flashes wide as it lights");
        check(Rules.lanceRadius(10) > 0 && Rules.lanceRadius(Rules.LANCE_SHOT_TICKS) == 0, "the jet pinches off at the end");
        check(Rules.lanceRadius(-1) == 0, "no jet before it is lit");
        check(Rules.lanceFocus(0) == 0 && Rules.lanceFocus(3) == 1, "the jet focuses at once");
        check(2.2F / Rules.LANCE_CHARGE_PITCH >= Rules.LANCE_CHARGE_TICKS / 20F, "the charge sound must not end before the spool-up");
        check(1.2F / Rules.LANCE_CHARGE_BODY_PITCH >= Rules.LANCE_CHARGE_TICKS / 20F, "the spool's burst must fall after the shot has cut it off");
        check(Rules.blastRadius(1) == Rules.LANCE_BLAST_RADIUS && Rules.blastRadius(2) > Rules.blastRadius(1), "blast radius");
        check(Rules.blastFalloff(0, 7) == 1 && Math.abs(Rules.blastFalloff(7, 7) - 0.4F) < 0.001F && Math.abs(Rules.blastFalloff(70, 7) - 0.4F) < 0.001F, "blast falloff");
        check(Rules.compactEnergy(999).equals("999") && Rules.compactEnergy(1500).equals("1.5k") && Rules.compactEnergy(16_000_000).equals("16M"), "compact energy");
        check(Rules.compactEnergy(2_000_000_000L).equals("2G") && Rules.compactEnergy(12_345).equals("12.3k") && Rules.compactEnergy(0).equals("0"), "compact energy units");
        check(Rules.shieldable(10, Rules.SHIELD_FE_PER_HP * 4L) == 4, "shield limited by energy");
        check(Rules.shieldable(10, Rules.SHIELD_FE_PER_HP * 40L) == 10, "shield limited by damage");
        check(Rules.shieldable(Float.NaN, 1_000_000) == 0 && Rules.shieldable(10, -5) == 0, "shield rejects nonsense");
        check(Rules.relaxing(-1, 50) == 1 && Rules.relaxing(50, 50) == 0, "relax ends");
        for (int age = 1; age <= 50; age++) check(Rules.relaxing(age, 50) <= Rules.relaxing(age - 1, 50), "relax must only fall");
        check(Rules.relaxing(25, 50) > 0.4F && Rules.relaxing(25, 50) < 0.6F, "relax is gradual");
        for (int i = 0; i <= 1000; i++) {
            float p = i / 1000F;
            for (float v : new float[]{Rules.reloadTilt(p), Rules.reloadVent(p), Rules.reloadMagazine(p), Rules.reloadReach(p), Rules.reloadCock(p)}) {
                check(v >= 0 && v <= 1, "reload curve out of range");
            }
        }
        for (float v : new float[]{Rules.reloadTilt(0), Rules.reloadVent(0), Rules.reloadMagazine(0), Rules.reloadReach(0), Rules.reloadCock(0),
                Rules.reloadTilt(1), Rules.reloadVent(1), Rules.reloadMagazine(1), Rules.reloadReach(1), Rules.reloadCock(1)}) {
            check(v == 0, "a reload must start and end at rest");
        }
        check(Rules.reloadMagazine(0.4F) == 1 && Rules.reloadMagazineGone(0.4F), "magazine is out mid-reload");
        check(!Rules.reloadMagazineGone(0) && !Rules.reloadMagazineGone(1), "magazine is present at rest");
        check(Rules.lanceKick(-1) == 0, "no recoil before firing");
        check(Rules.lanceKick(0) == 1 && Rules.lanceKick(Rules.LANCE_PULSE) < 1 && Rules.lanceKick(Rules.LANCE_PULSE * 2) == 0, "one kick per shot");
        check(Rules.moduleSlots(1) == 0 && Rules.moduleSlots(2) == 1 && Rules.moduleSlots(4) == 3 && Rules.moduleSlots(5) == 5, "socket tiers");
        check(Rules.collectRadius(0) == 0 && Rules.collectRadius(1) == 6 && Rules.collectRadius(2) == 9 && Rules.collectRadius(9) == 16, "collector reach");
        check(Rules.induced(1_000_000, Integer.MAX_VALUE, 1) == 20_000 && Rules.induced(1_000_000, Integer.MAX_VALUE, 3) == 60_000, "coils add up");
        check(Rules.induced(5_000, Integer.MAX_VALUE, 2) == 5_000 && Rules.induced(1_000_000, 300, 2) == 300 && Rules.induced(100, 50, 0) == 0, "induction never moves more than there is");
        check(Rules.capacityWith(1000, 0) == 1000 && Rules.capacityWith(1000, 2) == 2000, "capacitor bonus");
        check(Rules.capacityWith(Integer.MAX_VALUE, 3) == Integer.MAX_VALUE, "capacity overflow");
        check(Rules.cooled(160, 0) == 160 && Rules.cooled(160, 1) == 120 && Rules.cooled(1, 9) == 1, "coolant");
        check(Rules.amplified(0) == 1 && Rules.amplified(-1) == 1, "amplifier");
        check(Rules.overclocked(0) == 1 && Math.abs(Rules.overclocked(2) - 1.8F) < 0.001F && Rules.overclocked(-3) == 1, "overclock");
        check(Rules.LANCE_MAGAZINE * Rules.LANCE_SHOT_FE <= Rules.LANCE_CAPACITY, "a full tank must cover one magazine");
        System.out.println("Rules: " + checks + " checks passed");
    }
}
