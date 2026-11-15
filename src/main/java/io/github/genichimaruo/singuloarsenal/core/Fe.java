package io.github.genichimaruo.singuloarsenal.core;

import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.ItemAccessEnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.registries.DeferredHolder;

/** FE bookkeeping for gear. The capability only accepts: gear can never be used as a battery for machines. */
public final class Fe {
    public static final DeferredHolder<Item, Item> MASS_PELLET =
            DeferredHolder.create(Registries.ITEM, Identifier.fromNamespaceAndPath("singulo", "mass_pellet"));

    private Fe() {}

    public static int capacity(ItemStack stack) {
        return stack.getItem() instanceof Powered powered ? Rules.capacityWith(powered.capacity(), Modules.count(stack, ModuleType.CAPACITOR)) : 0;
    }

    public static int stored(ItemStack stack) {
        return Rules.clamp(ArsenalData.get(stack, ArsenalData.ENERGY), capacity(stack));
    }

    public static void set(ItemStack stack, int amount) {
        stack.set(ArsenalData.ENERGY.get(), Rules.clamp(amount, capacity(stack)));
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

    public static ItemAccessEnergyHandler handler(ItemStack stack, ItemAccess access) {
        int capacity = capacity(stack);
        return new ItemAccessEnergyHandler(access, ArsenalData.ENERGY.get(), capacity, capacity, 0);
    }

    /** Hold the gear and sneak-use the output face of any FE source, the same gesture as Singulo's gauntlet. */
    public static InteractionResult chargeFromBlock(UseOnContext context) {
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player == null || !player.isShiftKeyDown() || capacity(stack) == 0) return InteractionResult.PASS;
        var source = context.getLevel().getCapability(Capabilities.Energy.BLOCK, context.getClickedPos(), context.getClickedFace());
        if (source == null) return InteractionResult.PASS;
        if (!context.getLevel().isClientSide()) {
            var own = handler(stack, ItemAccess.forPlayerInteraction(player, context.getHand()));
            int got;
            try (var tx = Transaction.openRoot()) {
                int wanted = Math.min(Rules.chargeRequest(capacity(stack)), capacity(stack) - stored(stack));
                got = source.extract(wanted, tx);
                if (own.insert(got, tx) != got) return InteractionResult.FAIL;
                tx.commit();
            }
            player.sendOverlayMessage(Component.translatable("message.singulo_arsenal.charged", Rules.compactEnergy(got), Rules.compactEnergy(stored(player.getItemInHand(context.getHand())))));
        }
        return InteractionResult.SUCCESS;
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
        for (ItemStack fuel : player.getInventory().getNonEquipmentItems()) {
            if (fuel.is(MASS_PELLET.get())) {
                fuel.shrink(1);
                set(gear, stored(gear) + Rules.PELLET_FE);
                return true;
            }
        }
        return false;
    }
}
