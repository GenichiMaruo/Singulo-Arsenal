package io.github.genichimaruo.singuloarsenal.core;

import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.registries.ForgeRegistries;

/** FE bookkeeping for gear. The capability only accepts: gear can never be used as a battery for machines. */
public final class Fe {
    public static final java.util.function.Supplier<Item> MASS_PELLET = () -> ForgeRegistries.ITEMS.getValue(new ResourceLocation("singulo", "mass_pellet"));

    private Fe() {}

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof Powered powered ? Rules.capacityWith(powered.capacity(), Modules.count(stack, ModuleType.CAPACITOR)) : 0;
    }

    public static int stored(ItemStack stack) {
        return Rules.clamp(ArsenalData.get(stack, ArsenalData.ENERGY), capacity(stack));
    }

    public static void set(ItemStack stack, int amount) {
        ArsenalData.set(stack, ArsenalData.ENERGY, Rules.clamp(amount, capacity(stack)));
    }

    /** All or nothing: a cost that cannot be paid in full takes nothing. */
    public static boolean spend(ItemStack stack, int amount) {
        int have = stored(stack);
        if (amount < 0 || have < amount) return false;
        set(stack, have - amount);
        return true;
    }

    public static IEnergyStorage storage(ItemStack stack) {
        return new IEnergyStorage() {
            @Override public int receiveEnergy(int requested, boolean simulate) {
                int taken = Rules.accepted(stored(stack), capacity(stack), requested);
                if (!simulate && taken > 0) set(stack, stored(stack) + taken);
                return taken;
            }
            @Override public int extractEnergy(int requested, boolean simulate) { return 0; }
            @Override public int getEnergyStored() { return stored(stack); }
            @Override public int getMaxEnergyStored() { return capacity(stack); }
            @Override public boolean canExtract() { return false; }
            @Override public boolean canReceive() { return capacity(stack) > 0; }
        };
    }

    /** Hold the gear and sneak-use the output face of any FE source, the same gesture as Singulo's gauntlet. */
    public static InteractionResult chargeFromBlock(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isShiftKeyDown() || capacity(stack) == 0) return InteractionResult.PASS;
        var blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());
        var source = blockEntity == null ? null : blockEntity.getCapability(ForgeCapabilities.ENERGY, context.getClickedFace()).orElse(null);
        if (source == null || !source.canExtract()) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide) {
            var own = storage(stack);
            int wanted = own.receiveEnergy(Rules.chargeRequest(capacity(stack)), true);
            int got = source.extractEnergy(wanted, false);
            own.receiveEnergy(got, false);
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.charged", Rules.compactEnergy(got), Rules.compactEnergy(stored(stack))), true);
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    /** Swallow one pellet if the tank has room for all of it. Returns whether a pellet was consumed. */
    public static boolean accrete(Player player, ItemStack gear) {
        if (!(gear.getItem() instanceof Powered powered) || !powered.accretes()) return false;
        if (!Rules.canAccrete(stored(gear), capacity(gear))) return false;
        // Canisters first: they exist to be drawn on, and loose pellets may be wanted for crafting.
        if (PelletCanisterItem.draw(player)) {
            set(gear, stored(gear) + Rules.PELLET_FE);
            return true;
        }
        for (ItemStack fuel : player.getInventory().items) {
            if (fuel.is(MASS_PELLET.get())) {
                fuel.shrink(1);
                set(gear, stored(gear) + Rules.PELLET_FE);
                return true;
            }
        }
        return false;
    }
}
