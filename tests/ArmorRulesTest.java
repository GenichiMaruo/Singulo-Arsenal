import io.github.genichimaruo.singuloarsenal.armor.ArmorRules;

public final class ArmorRulesTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        // Energy must be conserved, even at boundaries or with malicious negative/huge requests.
        for (int capacity : new int[]{0, 100_000, 500_000}) {
            for (int stored : new int[]{-1, 0, 1, 99_999, 100_000, 500_000, Integer.MAX_VALUE}) {
                for (int request : new int[]{Integer.MIN_VALUE, -1, 0, 1, 2_000, Integer.MAX_VALUE}) {
                    int safe = ArmorRules.clamp(stored, capacity);
                    int accepted = ArmorRules.received(stored, capacity, request);
                    int taken = ArmorRules.extracted(stored, capacity, request);
                    check(accepted >= 0 && safe + accepted <= capacity, "charging overflow");
                    check(taken >= 0 && safe - taken >= 0, "negative reservoir");
                    check(accepted <= Math.max(0, request) && taken <= Math.max(0, request), "created energy");
                }
            }
        }
        check(ArmorRules.impactFromFall(5.99F) == 0, "tiny fall farm");
        check(ArmorRules.impactFromFall(6) == 1, "fall threshold");
        check(ArmorRules.impactFromFall(1000) == 10, "unbounded fall collection");
        check(ArmorRules.impactFromDamage(Float.NaN) == 0, "NaN impact");
        check(ArmorRules.impactFromDamage(Float.POSITIVE_INFINITY) == 0, "infinite impact");
        check(ArmorRules.impactFromDamage(1000) == 20, "unbounded damage collection");
        check(!ArmorRules.canDodge(false, false, 20, 2000, false), "air dodge");
        check(!ArmorRules.canDodge(true, true, 20, 2000, false), "overloaded dodge");
        check(!ArmorRules.canDodge(true, false, 19, 2000, false), "free impact dodge");
        check(!ArmorRules.canDodge(true, false, 20, 1999, false), "free FE dodge");
        check(!ArmorRules.canDodge(true, false, 20, 2000, true), "cooldown bypass");
        check(ArmorRules.canDodge(true, false, 20, 2000, false), "exact cost rejected");
        check(!ArmorRules.canRepair(1000, 801, true, 1), "repair in combat");
        check(ArmorRules.canRepair(1000, 800, true, 1), "combat boundary");
        check(!ArmorRules.canRepair(1000, 0, true, 0), "fuel-free repair");
        check(!ArmorRules.canRepair(1000, 0, false, 1), "waste fuel on pristine armor");
        for (int pool = 0; pool <= 16; pool++) {
            for (float damage : new float[]{0, 0.1F, 1, 15.5F, 16, 40}) {
                float absorbed = ArmorRules.absorbed(damage, pool);
                check(damage - absorbed >= 0 && absorbed <= pool, "shield created health");
                check(Math.abs((damage - absorbed) + absorbed - damage) < 0.001, "excess damage lost");
                check(ArmorRules.poolAfter(absorbed, pool) >= 0, "negative shield");
            }
        }
        check(40 - ArmorRules.absorbed(40, 16) == 24, "large hit immunity");
        check(ArmorRules.poolAfter(0.1F, 1) == 0, "fractional hits cost no shield");
        System.out.println("ArmorRules: " + checks + " checks passed");
    }
}
