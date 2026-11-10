package io.github.genichimaruo.singuloarsenal.armor;

import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.core.Tips;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

public final class SuitItem extends ArmorItem implements Powered {
    public final ArmorSet set;

    public SuitItem(ArmorMaterial material, Type type, ArmorSet set) {
        super(material, type, properties(type, set));
        this.set = set;
    }

    private static int materialDurability(Type type, ArmorSet set) {
        return set.durability * switch (type) { case HELMET -> 11; case CHESTPLATE -> 16; case LEGGINGS -> 15; case BOOTS -> 13; };
    }
    private static Properties properties(Type type, ArmorSet set) {
        Properties properties = new Properties().durability(materialDurability(type, set))
                .rarity(set == ArmorSet.COLLAPSAR ? Rarity.EPIC : set == ArmorSet.DEGENERACY ? Rarity.RARE : Rarity.UNCOMMON);
        return set.fireproof ? properties.fireResistant() : properties;
    }

    @Override public int capacity() { return set.capacity; }
    @Override public boolean accretes() { return set == ArmorSet.COLLAPSAR; }
    @Override public String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity,
            net.minecraft.world.entity.EquipmentSlot slot, String layer) {
        return "singulo_arsenal:textures/misc/armor_blank.png";
    }

    @Override public InteractionResult useOn(UseOnContext context) { return Fe.chargeFromBlock(context); }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack from, ItemStack to, boolean changed) {
        return !ItemStack.isSameItem(from, to);
    }

    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * Fe.stored(stack) / Math.max(1, Fe.capacity(stack))); }
    @Override public int getBarColor(ItemStack stack) { return 0x6FE3FF; }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flag) {
        String id = set.id + "." + getType().getName();
        lines.add(Component.translatable("tooltip.singulo_arsenal." + id + ".summary").withStyle(ChatFormatting.GRAY));
        Tips.energyLine(stack, lines);
        Modules.describe(stack, lines);
        if (!Tips.detail()) {
            Tips.hint(lines);
            return;
        }
        lines.add(Component.translatable("tooltip.singulo_arsenal." + id).withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.translatable("tooltip.singulo_arsenal.full_set." + set.id).withStyle(ChatFormatting.LIGHT_PURPLE));
        if (accretes()) lines.add(Component.translatable("tooltip.singulo_arsenal.accretion").withStyle(ChatFormatting.DARK_AQUA));
        lines.add(Component.translatable("tooltip.singulo_arsenal.charge").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.singulo_arsenal.stage", set.stage).withStyle(ChatFormatting.GOLD));
    }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        io.github.genichimaruo.singuloarsenal.client.ArsenalClient.extensions(consumer);
    }
}
