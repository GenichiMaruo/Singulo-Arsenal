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
    /** Singulo's frame: the standard width, the inventory measured up from the bottom edge. */
    public static final int WIDTH = 176, HEIGHT = 186, INVENTORY_X = 8, ARROW_X = 88, ARROW_Y = 37;
    /** Modules flow left to right, through the arrow, into the gear. */
    public static final int[][] POSITIONS = {{118, 37}, {26, 28}, {44, 28}, {62, 28}, {35, 46}, {53, 46}};
    private static final int MACHINE_SLOTS = IntegratorBlockEntity.SLOTS;

    private final IItemHandler items;
    private final ContainerData data;
    private final IntegratorBlockEntity entity;

    /** Client side. */
    public IntegratorMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, lookup(inventory, buffer), new ItemStackHandler(MACHINE_SLOTS), new SimpleContainerData(IntegratorBlockEntity.DATA_COUNT));
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
        int top = HEIGHT - 82;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column + row * 9 + 9, INVENTORY_X + column * 18, top + row * 18));
        }
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, INVENTORY_X + column * 18, HEIGHT - 24));
        addDataSlots(data);
    }

    private boolean socketsAcceptInstalled(ItemStack incoming) {
        int free = 0;
        for (int i = IntegratorBlockEntity.FIRST_SOCKET; i < MACHINE_SLOTS; i++) if (items.getStackInSlot(i).isEmpty()) free++;
        return Modules.installed(incoming).size() <= free;
    }

    public ItemStack gear() { return items.getStackInSlot(IntegratorBlockEntity.GEAR); }
    public boolean socketOpen(int socket) { return socket < Modules.slots(gear()); }
    public int progress() { return data.get(IntegratorBlockEntity.DATA_PROGRESS); }
    public int energyStored() { return (data.get(IntegratorBlockEntity.DATA_ENERGY_LOW) & 0xFFFF) | (data.get(IntegratorBlockEntity.DATA_ENERGY_HIGH) & 0xFFFF) << 16; }
    public boolean working() { return data.get(IntegratorBlockEntity.DATA_WORKING) != 0; }

    /** Whether the module in this socket can go into the gear on the bed. */
    public boolean socketHolds(int socket) {
        return socketOpen(socket) && Modules.fits(gear(), items.getStackInSlot(IntegratorBlockEntity.FIRST_SOCKET + socket));
    }

    /** The same test the machine applies when it starts, so the button is never live for a run that would be refused. */
    public boolean valid() {
        for (int socket = 0; socket < IntegratorBlockEntity.SOCKETS; socket++) {
            if (!items.getStackInSlot(IntegratorBlockEntity.FIRST_SOCKET + socket).isEmpty() && !socketHolds(socket)) return false;
        }
        return true;
    }

    public boolean canStart() { return !working() && !gear().isEmpty() && valid(); }

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
