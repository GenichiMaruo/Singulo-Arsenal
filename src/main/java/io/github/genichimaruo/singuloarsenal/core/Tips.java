package io.github.genichimaruo.singuloarsenal.core;

import java.util.List;
import java.util.function.BooleanSupplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Tooltip conventions: one short line by default, everything else behind Shift. Energy reads 1.5M FE unless Shift asks for the exact number. */
public final class Tips {
    /** Whether Shift is held. Set by the client at startup, so this class stays safe to load on a dedicated server. */
    public static BooleanSupplier detail = () -> false;

    private Tips() {}

    public static boolean detail() {
        return detail.getAsBoolean();
    }

    public static String energy(long fe) {
        return detail() ? String.format(java.util.Locale.ROOT, "%,d", fe) : Rules.compactEnergy(fe);
    }

    public static void energyLine(ItemStack stack, List<Component> lines) {
        lines.add(Component.translatable("tooltip.singulo_arsenal.energy", energy(Fe.stored(stack)), energy(Fe.capacity(stack))).withStyle(ChatFormatting.AQUA));
    }

    public static void hint(List<Component> lines) {
        lines.add(Component.translatable("tooltip.singulo_arsenal.more").withStyle(ChatFormatting.DARK_GRAY));
    }
}
