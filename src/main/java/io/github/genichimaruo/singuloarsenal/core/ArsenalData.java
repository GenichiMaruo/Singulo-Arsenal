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
