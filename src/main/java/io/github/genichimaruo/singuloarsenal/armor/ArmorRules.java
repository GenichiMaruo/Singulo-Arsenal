package io.github.genichimaruo.singuloarsenal.armor;

/** Server-side balance rules, independent of Minecraft so they can be regression tested offline. */
public final class ArmorRules {
    public static final int IMPACT_CAPACITY = 100;
    public static final int DODGE_IMPACT = 20;
    public static final int DODGE_FE = 2_000;
    public static final int DODGE_COOLDOWN = 30;
    public static final int OVERLOAD_TICKS = 100;
    public static final int REPAIR_UNITS = 256;
    public static final int COMBAT_DELAY = 200;
    public static final int BARRIER_START_FE = 20_000;
    public static final int BARRIER_TICK_FE = 100;
    public static final int BARRIER_POOL = 16;
    public static final int BARRIER_COOLDOWN = 1_200;

    private ArmorRules() {}

    public static int clamp(int value, int capacity) {
        return Math.max(0, Math.min(value, capacity));
    }

    public static int received(int stored, int capacity, int requested) {
        return Math.min(Math.max(0, requested), capacity - clamp(stored, capacity));
    }

    public static int extracted(int stored, int capacity, int requested) {
        return Math.min(Math.max(0, requested), clamp(stored, capacity));
    }

    /** At most 20 impact per hit; environmental/self damage is filtered before calling this. */
    public static int impactFromDamage(float damage) {
        return Float.isFinite(damage) && damage > 0 ? Math.min(20, (int) Math.ceil(damage * 2)) : 0;
    }

    /** Falls below six blocks never generate impact; falls cannot refill the entire reservoir. */
    public static int impactFromFall(float distance) {
        return Float.isFinite(distance) && distance >= 6 ? Math.min(10, (int) (distance - 5)) : 0;
    }

    public static float absorbed(float damage, int pool) {
        return Float.isFinite(damage) && damage > 0 ? Math.min(damage, Math.max(0, pool)) : 0;
    }

    public static int poolAfter(float absorbed, int pool) {
        return Math.max(0, pool - (int) Math.ceil(absorbed));
    }

    public static boolean canDodge(boolean grounded, boolean disabled, int impact, int energy, boolean cooling) {
        return grounded && !disabled && !cooling && impact >= DODGE_IMPACT && energy >= DODGE_FE;
    }

    public static boolean canRepair(long now, long lastCombat, boolean damaged, int reserve) {
        return damaged && reserve > 0 && now - lastCombat >= COMBAT_DELAY;
    }
}
