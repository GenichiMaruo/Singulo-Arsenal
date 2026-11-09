package io.github.genichimaruo.singuloarsenal.module;

import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.core.Tips;
import io.github.genichimaruo.singuloarsenal.weapon.ArsenalWeapon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

/** Modules are stored inside the gear itself, so they survive saves, trades and death exactly as the gear does. */
public final class Modules {
    private Modules() {}

    public static int slots(ItemStack gear) {
        if (gear.getItem() instanceof SuitItem suit) return Rules.moduleSlots(suit.set.stage);
        if (gear.getItem() instanceof ArsenalWeapon weapon) return Rules.moduleSlots(weapon.stage());
        return 0;
    }

    public static List<ItemStack> installed(ItemStack gear) {
        List<ItemStack> out = new ArrayList<>();
        gear.getOrDefault(ArsenalData.MODULES.get(), ItemContainerContents.EMPTY).nonEmptyStream().forEach(out::add);
        return out;
    }

    public static void install(ItemStack gear, List<ItemStack> modules) {
        gear.set(ArsenalData.MODULES.get(), ItemContainerContents.fromItems(modules));
    }

    public static int count(ItemStack gear, ModuleType type) {
        if (!gear.has(ArsenalData.MODULES.get())) return 0;
        int total = 0;
        for (ItemStack module : installed(gear)) {
            if (module.getItem() instanceof ModuleItem item && item.type == type) total++;
        }
        return total;
    }

    public static boolean fits(ItemStack gear, ItemStack module) {
        return module.getItem() instanceof ModuleItem item && item.type.fits(gear.getItem());
    }

    public static void describe(ItemStack gear, List<Component> lines) {
        int slots = slots(gear);
        if (slots == 0) return;
        List<ItemStack> modules = installed(gear);
        lines.add(Component.translatable("tooltip.singulo_arsenal.modules", modules.size(), slots).withStyle(ChatFormatting.BLUE));
        if (Tips.detail()) for (ItemStack module : modules) lines.add(Component.literal(" - ").append(module.getHoverName()).withStyle(ChatFormatting.DARK_AQUA));
    }
}
