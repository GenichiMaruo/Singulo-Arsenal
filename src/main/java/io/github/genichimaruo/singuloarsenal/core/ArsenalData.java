package io.github.genichimaruo.singuloarsenal.core;

import net.minecraft.world.item.ItemStack;

/** Namespaced stack NBT is saved and synchronized by Minecraft 1.20.1. */
public final class ArsenalData {
    public static final String ENERGY = "energy", LOAD = "load", CURRENT = "current";
    public static final String LOCKED_UNTIL = "locked_until", HORIZON_UNTIL = "horizon_until", HORIZON_READY = "horizon_ready";
    public static final String ARMED_UNTIL = "armed_until", LAST_HIT = "last_hit", RELOAD_UNTIL = "reload_until", RELOAD_TICKS = "reload_ticks";
    public static final String MODULES = "modules";
    public static final String FOCUS_START = "focus_start";
    public static final String CHARGE_UNTIL = "charge_until";
    public static final String JET_UNTIL = "jet_until";
    public static final String SHOTS = "shots";
    public static final String TOOL_MODE = "tool_mode";
    public static final String SCAN_TIER = "scan_tier";
    public static final String PELLETS = "pellets";
    public static final String TERRAIN_MODE = "terrain_mode";
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
    public static void remove(ItemStack stack, String key) {
        var tag = stack.getTagElement("singulo_arsenal");
        if (tag != null) tag.remove(key);
    }
    public static boolean has(ItemStack stack, String key) {
        return stack.getTagElement("singulo_arsenal") != null && stack.getTagElement("singulo_arsenal").contains(key);
    }
}
