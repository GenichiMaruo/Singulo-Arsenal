package io.github.genichimaruo.singuloarsenal.module;

import io.github.genichimaruo.singuloarsenal.core.Powered;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

public final class IntegratorMenu extends AbstractContainerMenu {
    public static final int[][] POSITIONS = {{80, 35}, {44, 35}, {116, 35}, {80, 59}};
    private static final int MACHINE_SLOTS = IntegratorBlockEntity.SLOTS;

    private final IItemHandler items;
    private final ContainerData data;
    private final IntegratorBlockEntity entity;

    /** Client side. */
    public IntegratorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, lookup(inventory, buffer), new ItemStackHandler(MACHINE_SLOTS), new SimpleContainerData(3));
    }

    private static IntegratorBlockEntity lookup(Inventory inventory, FriendlyByteBuf buffer) {
        BlockEntity found = inventory.player.level().getBlockEntity(buffer.readBlockPos());
        return found instanceof IntegratorBlockEntity entity ? entity : null;
    }

    public IntegratorMenu(int id, Inventory inventory, IntegratorBlockEntity entity) {
        this(id, inventory, entity, entity.items(), entity.data);
    }

    private IntegratorMenu(int id, Inventory inventory, IntegratorBlockEntity entity, IItemHandler items, ContainerData data) {
        super(ModuleContent.INTEGRATOR_MENU.get(), id);
        this.entity = entity;
        this.items = items;
        this.data = data;
        addSlot(new SlotItemHandler(items, IntegratorBlockEntity.GEAR, POSITIONS[0][0], POSITIONS[0][1]) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof Powered && socketsAcceptInstalled(stack); }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int i = 0; i < IntegratorBlockEntity.SOCKETS; i++) {
            int socket = i;
            addSlot(new SlotItemHandler(items, IntegratorBlockEntity.FIRST_SOCKET + i, POSITIONS[i + 1][0], POSITIONS[i + 1][1]) {
                @Override public boolean mayPlace(ItemStack stack) { return socketOpen(socket) && Modules.fits(gear(), stack); }
                @Override public int getMaxStackSize() { return 1; }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 142));
        addDataSlots(data);
    }

    private boolean socketsAcceptInstalled(ItemStack incoming) {
        int free = 0;
        for (int i = IntegratorBlockEntity.FIRST_SOCKET; i < MACHINE_SLOTS; i++) if (items.getStackInSlot(i).isEmpty()) free++;
        return Modules.installed(incoming).size() <= free;
    }

    public ItemStack gear() { return items.getStackInSlot(IntegratorBlockEntity.GEAR); }
    public boolean socketOpen(int socket) { return socket < Modules.slots(gear()); }
    public int progress() { return data.get(0); }
    public int energy() { return data.get(1); }
    public boolean working() { return data.get(2) != 0; }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || entity == null) return false;
        entity.start();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = index < MACHINE_SLOTS
                ? moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)
                : moveItemStackTo(stack, 0, MACHINE_SLOTS, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return entity != null && !entity.isRemoved() && player.distanceToSqr(entity.getBlockPos().getCenter()) <= 64;
    }
}
