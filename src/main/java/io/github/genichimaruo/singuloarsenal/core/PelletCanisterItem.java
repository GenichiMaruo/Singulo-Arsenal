package io.github.genichimaruo.singuloarsenal.core;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A canister that holds mass pellets by the thousand, so the gear's accretion can feed for a long time without the pellets
 * filling the inventory. Accretion takes from canisters before loose pellets.
 */
public final class PelletCanisterItem extends Item {
    public PelletCanisterItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.UNCOMMON).fireResistant());
    }

    public static int stored(ItemStack canister) {
        return Math.min(Rules.CANISTER_CAPACITY, ArsenalData.get(canister, ArsenalData.PELLETS));
    }

    private static void setStored(ItemStack canister, int count) {
        if (count <= 0) canister.remove(ArsenalData.PELLETS.get());
        else canister.set(ArsenalData.PELLETS.get(), Math.min(Rules.CANISTER_CAPACITY, count));
    }

    /** Moves as much of a pellet stack in as fits, shrinking it. Returns how many went in. */
    public static int insert(ItemStack canister, ItemStack pellets) {
        if (!pellets.is(Fe.MASS_PELLET.get())) return 0;
        int moved = Math.min(pellets.getCount(), Rules.CANISTER_CAPACITY - stored(canister));
        if (moved <= 0) return 0;
        setStored(canister, stored(canister) + moved);
        pellets.shrink(moved);
        return moved;
    }

    /** Up to one stack out of the canister. */
    private static ItemStack extract(ItemStack canister) {
        int count = Math.min(stored(canister), new ItemStack(Fe.MASS_PELLET.get()).getMaxStackSize());
        setStored(canister, stored(canister) - count);
        return count > 0 ? new ItemStack(Fe.MASS_PELLET.get(), count) : ItemStack.EMPTY;
    }

    /** Takes one pellet from the first canister the player carries that has any. */
    public static boolean draw(Player player) {
        var inventory = player.getInventory();
        for (var list : List.of(inventory.items, inventory.offhand)) {
            for (ItemStack stack : list) {
                if (stack.getItem() instanceof PelletCanisterItem && stored(stack) > 0) {
                    setStored(stack, stored(stack) - 1);
                    return true;
                }
            }
        }
        return false;
    }

    /** Right click gathers every loose pellet carried; sneaking, it hands one stack back. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack canister = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(canister);
        if (player.isShiftKeyDown()) {
            ItemStack out = extract(canister);
            if (out.isEmpty()) return InteractionResultHolder.fail(canister);
            if (!player.getInventory().add(out)) player.drop(out, false);
        } else {
            int moved = 0;
            var inventory = player.getInventory();
            for (var list : List.of(inventory.items, inventory.offhand)) {
                for (ItemStack stack : list) moved += insert(canister, stack);
            }
            if (moved == 0) return InteractionResultHolder.fail(canister);
        }
        player.displayClientMessage(Component.translatable("message.singulo_arsenal.canister", stored(canister), Rules.CANISTER_CAPACITY), true);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BUNDLE_INSERT, SoundSource.PLAYERS, 0.8F, 0.8F);
        return InteractionResultHolder.consume(canister);
    }

    /** In a menu, like a bundle: pellets dropped on the canister go in, and a right click with an empty cursor takes a stack out. */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack canister, ItemStack carried, Slot slot, ClickAction action, Player player, SlotAccess access) {
        if (!slot.allowModification(player)) return false;
        if (action == ClickAction.PRIMARY && carried.is(Fe.MASS_PELLET.get())) {
            if (insert(canister, carried) > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F);
            return true;
        }
        if (action == ClickAction.SECONDARY && carried.isEmpty() && stored(canister) > 0) {
            access.set(extract(canister));
            player.playSound(SoundEvents.BUNDLE_REMOVE_ONE, 0.8F, 0.8F);
            return true;
        }
        return false;
    }

    /** Carrying the canister, a right click on a stack of pellets swallows it. */
    @Override
    public boolean overrideStackedOnOther(ItemStack canister, Slot slot, ClickAction action, Player player) {
        if (action != ClickAction.SECONDARY || !slot.getItem().is(Fe.MASS_PELLET.get())) return false;
        int room = Rules.CANISTER_CAPACITY - stored(canister);
        if (room > 0) {
            ItemStack taken = slot.safeTake(slot.getItem().getCount(), room, player);
            if (insert(canister, taken) > 0) player.playSound(SoundEvents.BUNDLE_INSERT, 0.8F, 0.8F);
        }
        return true;
    }

    @Override public boolean isBarVisible(ItemStack stack) { return stored(stack) > 0; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * stored(stack) / Rules.CANISTER_CAPACITY); }
    @Override public int getBarColor(ItemStack stack) { return 0xB89CFF; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.singulo_arsenal.pellet_canister.summary").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.singulo_arsenal.pellet_canister.count", stored(stack), Rules.CANISTER_CAPACITY).withStyle(ChatFormatting.GRAY));
        if (!Tips.detail()) {
            Tips.hint(lines);
            return;
        }
        for (int i = 1; i <= 3; i++) lines.add(Component.translatable("tooltip.singulo_arsenal.pellet_canister." + i).withStyle(ChatFormatting.DARK_AQUA));
    }
}
