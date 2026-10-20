package io.github.genichimaruo.singuloarsenal.armor;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

public final class ArsenalArmorItem extends ArmorItem {
    public final ArmorTier tier;

    public ArsenalArmorItem(Holder<ArmorMaterial> material, Type type, ArmorTier tier) {
        super(material, type, new Properties().durability(type.getDurability(tier.durability)));
        this.tier = tier;
    }

    /** Explorer leggings protect against freezing; boots provide the separate walking hook. */
    @Override
    public boolean canWalkOnPowderedSnow(ItemStack stack, LivingEntity wearer) {
        return tier == ArmorTier.EXPLORER && getEquipmentSlot() == EquipmentSlot.FEET;
    }

    /** Standard FE item capability, also used by Singulo's wireless charger. */
    public static IEnergyStorage energy(ItemStack stack) {
        int capacity = stack.getItem() instanceof ArsenalArmorItem armor ? armor.tier.capacity : 0;
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int requested, boolean simulate) {
                int stored = getEnergyStored();
                int amount = ArmorRules.received(stored, capacity, requested);
                if (!simulate && amount > 0) stack.set(ArmorData.ENERGY.get(), stored + amount);
                return amount;
            }
            @Override public int extractEnergy(int requested, boolean simulate) {
                int stored = getEnergyStored();
                int amount = ArmorRules.extracted(stored, capacity, requested);
                if (!simulate && amount > 0) stack.set(ArmorData.ENERGY.get(), stored - amount);
                return amount;
            }
            @Override public int getEnergyStored() { return ArmorRules.clamp(ArmorData.get(stack, ArmorData.ENERGY), capacity); }
            @Override public int getMaxEnergyStored() { return capacity; }
            @Override public boolean canExtract() { return capacity > 0; }
            @Override public boolean canReceive() { return capacity > 0; }
        };
    }

    /** Hold an armor piece and sneak-use a battery/output port to charge, like Singulo's gauntlet. */
    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (tier.capacity == 0 || player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;
        var source = context.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (source == null || !source.canExtract()) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            var own = energy(context.getItemInHand());
            int wanted = own.receiveEnergy(20_000, true);
            int got = source.extractEnergy(wanted, false);
            own.receiveEnergy(got, false);
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.charged", got), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.singulo_arsenal.stage", tier.stage).withStyle(ChatFormatting.GOLD));
        lines.add(Component.translatable("tooltip.singulo_arsenal." + tier.id + "." + getType().getName()).withStyle(ChatFormatting.GRAY));
        if (tier.capacity > 0) {
            lines.add(Component.translatable("tooltip.singulo_arsenal.energy", energy(stack).getEnergyStored(), tier.capacity).withStyle(ChatFormatting.AQUA));
            lines.add(Component.translatable("tooltip.singulo_arsenal.charge").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (tier == ArmorTier.CATALYTIC && getEquipmentSlot() == EquipmentSlot.CHEST) {
            lines.add(Component.translatable("tooltip.singulo_arsenal.repair", ArmorData.get(stack, ArmorData.REPAIR), ArmorRules.REPAIR_UNITS));
        }
    }
}
