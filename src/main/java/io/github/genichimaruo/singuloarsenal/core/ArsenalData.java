package io.github.genichimaruo.singuloarsenal.core;

import com.mojang.serialization.Codec;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Gear state lives in item components, so it saves with the stack and reaches other players' clients through equipment sync. */
public final class ArsenalData {
    public static final DeferredRegister.DataComponents REGISTER =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, SinguloArsenal.MOD_ID);

    public static final Supplier<DataComponentType<Integer>> ENERGY = integer("energy");
    /** Degeneracy pressure, Collapsar pasta load. */
    public static final Supplier<DataComponentType<Integer>> LOAD = integer("load");
    public static final Supplier<DataComponentType<Integer>> CURRENT = integer("current");
    public static final Supplier<DataComponentType<Long>> LOCKED_UNTIL = time("locked_until");
    public static final Supplier<DataComponentType<Long>> HORIZON_UNTIL = time("horizon_until");
    public static final Supplier<DataComponentType<Long>> HORIZON_READY = time("horizon_ready");
    public static final Supplier<DataComponentType<Long>> ARMED_UNTIL = time("armed_until");
    public static final Supplier<DataComponentType<Long>> LAST_HIT = time("last_hit");
    /** When the current reload animation ends, and how long it runs. On the stack so other players' clients see it too. */
    public static final Supplier<DataComponentType<Long>> RELOAD_UNTIL = time("reload_until");
    public static final Supplier<DataComponentType<Integer>> RELOAD_TICKS = integer("reload_ticks");
    /** When the reaver's Roche focus opened; absent when it is closed. On the stack, so every client sees the same focus. */
    public static final Supplier<DataComponentType<Long>> FOCUS_START = time("focus_start");
    /** When the cannon's spool-up ends and the shot goes off. */
    public static final Supplier<DataComponentType<Long>> CHARGE_UNTIL = time("charge_until");
    /** When the current cannon shot goes out. Other players' clients draw the jet from this. */
    public static final Supplier<DataComponentType<Long>> JET_UNTIL = time("jet_until");
    /** Shots fired since the last reload. */
    public static final Supplier<DataComponentType<Integer>> SHOTS = integer("shots");
    /** How much the accretion drill takes per block: one of {@code Rules.DRILL_*}. */
    public static final Supplier<DataComponentType<Integer>> TOOL_MODE = integer("tool_mode");
    /** The SQUID pick's sensitivity, 1 to 3 as on Singulo's neutrino scanner; missing reads as 1. */
    public static final Supplier<DataComponentType<Integer>> SCAN_TIER = integer("scan_tier");
    /** Mass pellets held in a pellet canister. */
    public static final Supplier<DataComponentType<Integer>> PELLETS = integer("pellets");
    /** 0 spares the terrain, 1 melts it. */
    public static final Supplier<DataComponentType<Integer>> TERRAIN_MODE = integer("terrain_mode");

    public static final Supplier<DataComponentType<ItemContainerContents>> MODULES = REGISTER.registerComponentType("modules",
            b -> b.persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).cacheEncoding());

    private ArsenalData() {}

    private static Supplier<DataComponentType<Integer>> integer(String name) {
        return REGISTER.registerComponentType(name,
                b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));
    }

    private static Supplier<DataComponentType<Long>> time(String name) {
        return REGISTER.registerComponentType(name, b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    public static int get(ItemStack stack, Supplier<DataComponentType<Integer>> key) {
        return Math.max(0, stack.getOrDefault(key.get(), 0));
    }

    public static long time(ItemStack stack, Supplier<DataComponentType<Long>> key) {
        return stack.getOrDefault(key.get(), 0L);
    }
}
