package io.github.genichimaruo.singuloarsenal.armor;

import com.mojang.serialization.Codec;
import java.util.function.Supplier;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Item components are saved with the equipment and synchronized by vanilla inventory packets. */
public final class ArmorData {
    public static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, "singulo_arsenal");
    public static final Supplier<DataComponentType<Integer>> ENERGY = integer("energy");
    public static final Supplier<DataComponentType<Integer>> IMPACT = integer("impact");
    public static final Supplier<DataComponentType<Integer>> REPAIR = integer("repair");
    public static final Supplier<DataComponentType<Integer>> SHIELD = integer("shield");
    public static final Supplier<DataComponentType<Long>> ACTIVE_UNTIL = time("active_until");
    public static final Supplier<DataComponentType<Long>> READY_AT = time("ready_at");
    public static final Supplier<DataComponentType<Long>> OVERLOADED_UNTIL = time("overloaded_until");
    public static final Supplier<DataComponentType<Long>> COLLECT_AT = time("collect_at");

    private ArmorData() {}

    private static Supplier<DataComponentType<Integer>> integer(String name) {
        return REGISTER.registerComponentType(name,
                b -> b.persistent(Codec.intRange(0, Integer.MAX_VALUE)).networkSynchronized(ByteBufCodecs.VAR_INT));
    }

    private static Supplier<DataComponentType<Long>> time(String name) {
        return REGISTER.registerComponentType(name,
                b -> b.persistent(Codec.LONG).networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    public static int get(ItemStack stack, Supplier<DataComponentType<Integer>> key) {
        return Math.max(0, stack.getOrDefault(key.get(), 0));
    }

    public static long time(ItemStack stack, Supplier<DataComponentType<Long>> key) {
        return stack.getOrDefault(key.get(), 0L);
    }

    public static boolean active(ItemStack stack, long now) {
        return time(stack, ACTIVE_UNTIL) > now && get(stack, SHIELD) > 0;
    }
}
