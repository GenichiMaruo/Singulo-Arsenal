package io.github.genichimaruo.singuloarsenal.core;

/**
 * Every balance number in the mod and the pure arithmetic behind it. Nothing here touches Minecraft, so
 * {@code tools/check.py} compiles this file alone and regression-tests it without starting the game.
 */
public final class Rules {
    private Rules() {}

    // ---- Energy ------------------------------------------------------------------------------------------------

    public static final int MEISSNER_CAPACITY = 240_000;
    public static final int DEGENERACY_CAPACITY = 2_000_000;
    public static final int COLLAPSAR_CAPACITY = 16_000_000;
    public static final int QUENCH_CAPACITY = 240_000;
    public static final int CASIMIR_CAPACITY = 1_000_000;
    public static final int REAVER_CAPACITY = 8_000_000;
    public static final int LANCE_CAPACITY = 16_000_000;

    /** A sneak-use on a power source requests one eighth of the tank, so large tanks do not take hundreds of clicks. */
    public static int chargeRequest(int capacity) {
        return Math.max(20_000, capacity / 8);
    }

    public static int clamp(int value, int capacity) {
        return Math.max(0, Math.min(value, capacity));
    }

    public static int accepted(int stored, int capacity, int requested) {
        return Math.min(Math.max(0, requested), capacity - clamp(stored, capacity));
    }

    /** Collapsar-class gear accretes mass pellets at this yield. It is one-way: the FE can never be drawn back out. */
    public static final int PELLET_FE = 500_000;

    public static boolean canAccrete(int stored, int capacity) {
        return capacity - clamp(stored, capacity) >= PELLET_FE;
    }

    // ---- Meissner suit: a superconductor expels fields and holds its position in a pinned flux lattice -------------

    public static final int DEFLECT_FE = 1_500;
    public static final int DEFLECT_COOLDOWN = 4;
    public static final int LENZ_FE = 800;
    public static final double LENZ_PUSH = 0.9;
    public static final float MEISSNER_LEG_SPEED = 0.07F;
    /** Paid in half-second lumps so a hovering player is not re-sent their boots every tick. */
    public static final int PIN_FE_PER_10_TICKS = 300;
    /** Flux creep: the pin always sags. It also keeps the descent above vanilla's anti-flight floating threshold. */
    public static final double FLUX_CREEP = -0.05;
    public static final int SQUID_RANGE = 24;

    public static boolean shouldPin(boolean grounded, boolean sneaking, boolean restricted, double verticalSpeed, int energy) {
        return !grounded && sneaking && !restricted && verticalSpeed < FLUX_CREEP && energy > 0;
    }

    /** Share a persistent current across pieces: returns the level every piece settles to; the remainder stays put. */
    public static int balanced(int total, int pieces) {
        return pieces <= 0 ? 0 : Math.max(0, total) / pieces;
    }

    // ---- Degeneracy plate: pressure holds until the Chandrasekhar limit, and then everything goes at once ---------

    /** Final damage per hit above this is converted into pressure instead of reaching the wearer. */
    public static final float CHANDRASEKHAR_LIMIT = 8F;
    public static final int PRESSURE_FE_PER_HP = 4_000;
    public static final int PRESSURE_MAX = 60;
    public static final int COLLAPSE_LOCKOUT = 600;
    public static final double COLLAPSE_RADIUS = 6;
    public static final float COLLAPSE_DAMAGE = 12;
    public static final float COLLAPSE_DAMAGE_PER_PRESSURE = 0.25F;
    public static final int VENT_MINIMUM = 15;
    public static final float BRACE_FACTOR = 0.7F;
    public static final int SLAM_FE_PER_BLOCK = 500;
    public static final int FILTER_FE = 2_000;

    /** How much of this hit exceeds the limit, in whole points of pressure the suit can afford. */
    public static int pressureFrom(float finalDamage, int energy) {
        if (!Float.isFinite(finalDamage) || finalDamage <= CHANDRASEKHAR_LIMIT) return 0;
        int wanted = (int) Math.ceil(finalDamage - CHANDRASEKHAR_LIMIT);
        return Math.min(wanted, Math.max(0, energy) / PRESSURE_FE_PER_HP);
    }

    public static float slamDamage(float blocks) {
        return Float.isFinite(blocks) && blocks >= 4 ? Math.min(30, blocks) : 0;
    }

    public static double slamRadius(float blocks) {
        return Math.min(7, 2 + Math.max(0, blocks) / 4);
    }

    public static float collapseDamage(int pressure) {
        return COLLAPSE_DAMAGE + Math.max(0, pressure) * COLLAPSE_DAMAGE_PER_PRESSURE;
    }

    // ---- Collapsar armor: nuclear-pasta lining with the same three phases as Singulo's accumulator -----------------

    public static final int LOAD_MAX = 100;
    public static final int PHASE_GNOCCHI = 0;
    public static final int PHASE_SPAGHETTI = 1;
    public static final int PHASE_LASAGNA = 2;
    public static final float GNOCCHI_SPEED = 0.15F;
    public static final float SPAGHETTI_REACH = 1F;
    public static final float SPAGHETTI_KNOCKBACK = 0.5F;
    public static final float LASAGNA_SPEED = -0.10F;
    public static final float LASAGNA_FACTOR = 0.7F;
    public static final int JET_BURST_FE = 200_000;
    public static final int JET_BURST_LOAD = 25;
    public static final int JET_BURST_COOLDOWN = 30;
    public static final double JET_BURST_SPEED = 2.1;
    public static final double JET_BURST_REAR_RANGE = 10;
    public static final float JET_BURST_DAMAGE = 24;
    public static final int CLEANSE_FE = 10_000;
    public static final int HORIZON_FE = 8_000_000;
    public static final int HORIZON_TICKS = 60;
    public static final int HORIZON_COOLDOWN = 12_000;
    public static final float HORIZON_HEALTH_FRACTION = 0.4F;
    public static final double HORIZON_RADIUS = 9;
    public static final float HORIZON_DAMAGE = 60;

    public static int phaseOf(int load) {
        int value = clamp(load, LOAD_MAX);
        return value <= 33 ? PHASE_GNOCCHI : value <= 66 ? PHASE_SPAGHETTI : PHASE_LASAGNA;
    }

    /** Two points per point of health actually lost, at most fifteen from one hit: no single blow forces lasagna. */
    public static int loadFromDamage(float damage) {
        return Float.isFinite(damage) && damage > 0 ? Math.min(15, (int) Math.ceil(damage * 2)) : 0;
    }

    public static boolean horizonCatches(float finalDamage, float health, boolean bypassesEverything, int energy, boolean ready) {
        return Float.isFinite(finalDamage) && finalDamage >= health && !bypassesEverything && ready && energy >= HORIZON_FE;
    }

    // ---- Quench saber ------------------------------------------------------------------------------------------

    public static final int QUENCH_MAX_CURRENT = 5;
    public static final int QUENCH_DECAY_TICKS = 120;
    public static final int QUENCH_FE = 8_000;
    public static final float QUENCH_DAMAGE = 10;
    public static final double QUENCH_RADIUS = 4.5;
    public static final int QUENCH_COOLDOWN = 60;

    /** Current after a landed hit; returns {@code QUENCH_MAX_CURRENT} exactly when this hit quenches. */
    public static int currentAfterHit(int current) {
        return Math.min(QUENCH_MAX_CURRENT, clamp(current, QUENCH_MAX_CURRENT) + 1);
    }

    // ---- Casimir blade -----------------------------------------------------------------------------------------

    public static final float CASIMIR_ARMOR_KEPT = 0.65F;
    public static final float CASIMIR_ARMED_BONUS = 1.5F;
    public static final int CASIMIR_CLOSE_TICKS = 16;
    public static final int CASIMIR_ARMED_TICKS = 120;
    public static final int CASIMIR_COOLDOWN = 160;
    public static final int CASIMIR_FE = 40_000;

    // ---- Tidal reaver ------------------------------------------------------------------------------------------

    public static final int STRESS_MAX = 5;
    public static final int STRESS_TICKS = 120;
    public static final float STRESS_DAMAGE_STEP = 0.08F;
    public static final int REAVER_MIN_HOLD = 10;
    public static final int REAVER_MAX_HOLD = 60;
    public static final int REAVER_COOLDOWN = 100;
    public static final int REAVER_FE_PER_10_TICKS = 150_000;
    public static final double REAVER_RANGE = 12;
    public static final double REAVER_FOCUS = 5.5;
    public static final double REAVER_PULL = 0.34;
    /** Bodies stop this far from the focus: nothing is pulled through the hole itself. */
    public static final double REAVER_STOP = 1.2;
    public static final double REAVER_HEAVY_PULL = 0.08;
    public static final float REAVER_HEAVY_HEALTH = 200;
    public static final float SHEAR_BASE = 16;
    public static final float SHEAR_PER_STRESS = 10;

    public static int stressAfterHit(int stress) {
        return Math.min(STRESS_MAX, clamp(stress, STRESS_MAX) + 1);
    }

    public static float stressMultiplier(int stress) {
        return 1 + clamp(stress, STRESS_MAX) * STRESS_DAMAGE_STEP;
    }

    public static float shearDamage(int stress) {
        return SHEAR_BASE + clamp(stress, STRESS_MAX) * SHEAR_PER_STRESS;
    }

    /** Render-only: how far a body is drawn out. Compressed sideways so the volume stays constant. */
    public static float stretchFor(int stress, float shearFade) {
        return 1 + clamp(stress, STRESS_MAX) * 0.09F + Math.max(0, shearFade) * 1.1F;
    }

    // ---- Bipolar jet lance -------------------------------------------------------------------------------------

    public static final int LANCE_IGNITE = 10;
    public static final int LANCE_COLLIMATE = 30;
    public static final int LANCE_MAX_HOLD = 150;
    public static final int LANCE_PULSE = 5;
    public static final float LANCE_DAMAGE = 14;
    public static final float LANCE_ARMOR_KEPT = 0.4F;
    public static final double LANCE_RANGE = 80;
    /** Added to target hitboxes along the beam, so what looks hit is hit. */
    public static final double LANCE_HIT_WIDTH = 0.45;
    public static final int LANCE_FE_PER_10_TICKS = 250_000;
    public static final int LANCE_COOLDOWN = 160;
    public static final int LANCE_SHORT_COOLDOWN = 20;

    public static final int LANCE_IDLE = 0;
    public static final int LANCE_IGNITING = 1;
    public static final int LANCE_COLLIMATING = 2;
    public static final int LANCE_FIRING = 3;

    public static int lanceStage(int heldTicks) {
        if (heldTicks <= 0) return LANCE_IDLE;
        if (heldTicks < LANCE_IGNITE) return LANCE_IGNITING;
        if (heldTicks < LANCE_COLLIMATE) return LANCE_COLLIMATING;
        return LANCE_FIRING;
    }

    // ---- Modules and the Module Integrator ---------------------------------------------------------------------------------

    public static final int INTEGRATOR_CAPACITY = 400_000;
    public static final int INTEGRATE_TICKS = 100;
    public static final int INTEGRATE_FE_PER_TICK = 2_000;
    public static final float CAPACITOR_BONUS = 0.5F;
    public static final double PLATING_ARMOR = 2;
    public static final double PLATING_TOUGHNESS = 1;
    public static final double THRUSTER_JUMP = 0.1;
    public static final double THRUSTER_STEP = 0.5;
    public static final float COOLANT_FACTOR = 0.75F;
    public static final float AMPLIFIER_BONUS = 0.15F;
    public static final int BITS_PER_MODULE = 2;
    public static final double BIT_RANGE = 16;
    public static final float BIT_DAMAGE = 5;
    public static final int BIT_FE = 1_500;

    /** Sockets grow with what the gear cost to make: one at stage 2, two at stage 4, three at stage 5. */
    public static int moduleSlots(int stage) {
        return stage >= 5 ? 3 : stage >= 4 ? 2 : stage >= 2 ? 1 : 0;
    }

    public static int capacityWith(int base, int capacitors) {
        return (int) Math.min(Integer.MAX_VALUE, (long) base + (long) (base * (double) CAPACITOR_BONUS) * Math.max(0, capacitors));
    }

    public static int cooled(int ticks, int coolants) {
        return Math.max(1, Math.round(ticks * (float) Math.pow(COOLANT_FACTOR, Math.max(0, coolants))));
    }

    public static float amplified(int amplifiers) {
        return 1 + AMPLIFIER_BONUS * Math.max(0, amplifiers);
    }

    // ---- Reload choreography: each curve is 0 at rest and runs over reload progress 0..1 --------------------------------------

    private static float ramp(float p, float from, float to) {
        float k = Math.max(0, Math.min(1, (p - from) / (to - from)));
        return k * k * (3 - 2 * k);
    }

    /** Weapon turned over to reach the magazine. */
    public static float reloadTilt(float p) {
        return ramp(p, 0.06F, 0.22F) * (1 - ramp(p, 0.84F, 0.98F));
    }

    /** Heat vents raised, right after firing. */
    public static float reloadVent(float p) {
        return ramp(p, 0, 0.08F) * (1 - ramp(p, 0.76F, 0.86F));
    }

    /** How far the magazine has left its well. */
    public static float reloadMagazine(float p) {
        return p < 0.48F ? ramp(p, 0.14F, 0.3F) : 1 - ramp(p, 0.48F, 0.68F);
    }

    /** Between dropping the old magazine and bringing up the new one there is none attached. */
    public static boolean reloadMagazineGone(float p) {
        return p > 0.3F && p < 0.48F;
    }

    /** Support hand down at the magazine well. */
    public static float reloadReach(float p) {
        return ramp(p, 0.1F, 0.24F) * (1 - ramp(p, 0.66F, 0.76F));
    }

    /** Charging handle pulled back and released. */
    public static float reloadCock(float p) {
        return ramp(p, 0.72F, 0.8F) * (1 - ramp(p, 0.82F, 0.9F));
    }

    /** Recoil of one damage pulse, 1 as it fires and easing to 0 before the next. */
    public static float lanceKick(float held) {
        if (held < LANCE_COLLIMATE) return 0;
        float phase = ((held - LANCE_COLLIMATE) % LANCE_PULSE) / LANCE_PULSE;
        return (1 - phase) * (1 - phase);
    }

    /** The beam is narrowed by three rings, stepping down exactly as the Accretion Jet Forge's does. */
    public static float lanceRadius(int heldTicks) {
        if (heldTicks < LANCE_IGNITE) return 0.72F;
        if (heldTicks < LANCE_IGNITE + 7) return 0.66F;
        if (heldTicks < LANCE_IGNITE + 14) return 0.52F;
        if (heldTicks < LANCE_COLLIMATE) return 0.42F;
        return 0.34F;
    }
}
