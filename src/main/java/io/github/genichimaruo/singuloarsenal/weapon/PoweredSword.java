package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.Fe;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/** A sword with a charge bar, FE tooltip and the shared sneak-use charging gesture. */
abstract class PoweredSword extends SwordItem implements ArsenalWeapon {
    private final int capacity;
    private final int stage;

    PoweredSword(WeaponTier tier, int damage, float attackSpeed, int capacity, int stage, Rarity rarity) {
        super(tier, new Properties().rarity(rarity).fireResistant().attributes(createAttributes(tier, damage, attackSpeed)));
        this.capacity = capacity;
        this.stage = stage;
    }

    @Override public int capacity() { return capacity; }
    @Override public int stage() { return stage; }
    @Override public InteractionResult useOn(UseOnContext context) { return Fe.chargeFromBlock(context); }
    @Override public boolean shouldCauseReequipAnimation(ItemStack from, ItemStack to, boolean changed) { return !ItemStack.isSameItem(from, to); }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * Fe.stored(stack) / Math.max(1, Fe.capacity(stack))); }
    @Override public int getBarColor(ItemStack stack) { return 0x6FE3FF; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        Weapons.describe(BuiltInRegistries.ITEM.getKey(this).getPath(), stack, stage, lines);
    }
}
