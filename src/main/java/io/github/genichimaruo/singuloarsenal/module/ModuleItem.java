package io.github.genichimaruo.singuloarsenal.module;

import io.github.genichimaruo.singuloarsenal.core.Tips;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class ModuleItem extends Item {
    public final ModuleType type;

    public ModuleItem(ModuleType type) {
        super(new Properties().stacksTo(16));
        this.type = type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.singulo_arsenal." + type.id).withStyle(ChatFormatting.GRAY));
        if (!Tips.detail()) {
            Tips.hint(lines);
            return;
        }
        lines.add(Component.translatable("tooltip.singulo_arsenal." + type.id + ".detail").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.translatable("tooltip.singulo_arsenal.module_hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
