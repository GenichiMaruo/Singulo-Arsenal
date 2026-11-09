package io.github.genichimaruo.singuloarsenal.core;

import net.minecraft.world.item.ItemStack;

/** Namespaced stack NBT is saved and synchronized by Minecraft 1.20.1. */
public final class ArsenalData {
    public static final String ENERGY = "energy", LOAD = "load", CURRENT = "current";
    public static final String LOCKED_UNTIL = "locked_until", HORIZON_UNTIL = "horizon_until", HORIZON_READY = "horizon_ready";
    public static final String ARMED_UNTIL = "armed_until", LAST_HIT = "last_hit", RELOAD_UNTIL = "reload_until", RELOAD_TICKS = "reload_ticks";
    public static final String MODULES = "modules";
    private ArsenalData() {}
    public static int get(ItemStack stack, String key) {
        var tag = stack.getTagElement("singulo_arsenal");
        return tag == null ? 0 : Math.max(0, tag.getInt(key));
    }
    public static long time(ItemStack stack, String key) {
        var tag = stack.getTagElement("singulo_arsenal");
        return tag == null ? 0 : tag.getLong(key);
    }
    public static void set(ItemStack stack, String key, int value) {
        stack.getOrCreateTagElement("singulo_arsenal").putInt(key, value);
    }
    public static void set(ItemStack stack, String key, long value) {
        stack.getOrCreateTagElement("singulo_arsenal").putLong(key, value);
    }
    public static boolean has(ItemStack stack, String key) {
        return stack.getTagElement("singulo_arsenal") != null && stack.getTagElement("singulo_arsenal").contains(key);
    }
}
