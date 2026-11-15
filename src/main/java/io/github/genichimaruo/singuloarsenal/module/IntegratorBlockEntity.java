package io.github.genichimaruo.singuloarsenal.module;

import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Builds modules into a piece of gear. Placing gear on the bed takes out whatever it already carries and lays it in the sockets;
 * running the machine writes the sockets back. A module is therefore always in exactly one place, in the gear or in a socket.
 */
public final class IntegratorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GEAR = 0, FIRST_SOCKET = 1, SOCKETS = 5, SLOTS = FIRST_SOCKET + SOCKETS;
    public static final int DATA_PROGRESS = 0, DATA_ENERGY_LOW = 1, DATA_WORKING = 2, DATA_ENERGY_HIGH = 3, DATA_COUNT = 4;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                if (slot == GEAR) gearChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public void deserialize(ValueInput input) {
            // Historical saves have fewer sockets. Ignore their Size, but retain every valid slot.
            setSize(SLOTS);
            input.listOrEmpty("Items", net.minecraft.world.ItemStackWithSlot.CODEC).forEach(slot -> {
                if (slot.isValidInContainer(SLOTS)) stacks.set(slot.slot(), slot.stack());
            });
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final Energy energy = new Energy();
    private int progress;
    private boolean working;
    private ItemStack seen = ItemStack.EMPTY;

    private final class Energy extends SimpleEnergyHandler {
        Energy() {
            super(Rules.INTEGRATOR_CAPACITY, Rules.INTEGRATOR_CAPACITY, 0);
        }

        @Override
        protected void onEnergyChanged(int previous) { setChanged(); }
        boolean take(int amount) {
            if (energy < amount) return false;
            energy -= amount;
            return true;
        }
    }

    public IntegratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModuleContent.INTEGRATOR_ENTITY.get(), pos, state);
    }

    public ItemStackHandler items() { return items; }
    public SimpleEnergyHandler energy() { return energy; }
    public boolean working() { return working; }
    public float progress(float partial) { return Math.min(1, (progress + (working ? partial : 0)) / (float) Rules.INTEGRATE_TICKS); }

    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress * 100 / Rules.INTEGRATE_TICKS;
                // Container data travels as shorts.
                case DATA_ENERGY_LOW -> energy.getAmountAsInt() & 0xFFFF;
                case DATA_ENERGY_HIGH -> energy.getAmountAsInt() >>> 16;
                default -> working ? 1 : 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return DATA_COUNT; }
    };

    /** Unpacks gear as it arrives. A piece of gear is only unpacked once, and only if its modules all have somewhere to go. */
    private void gearChanged() {
        ItemStack gear = items.getStackInSlot(GEAR);
        if (ItemStack.isSameItemSameComponents(gear, seen)) return;
        progress = 0;
        working = false;
        List<ItemStack> installed = Modules.installed(gear);
        if (!installed.isEmpty() && freeSockets() >= installed.size()) {
            Modules.install(gear, List.of());
            int socket = FIRST_SOCKET;
            for (ItemStack module : installed) {
                while (!items.getStackInSlot(socket).isEmpty()) socket++;
                items.setStackInSlot(socket, module);
            }
        }
        seen = gear.copy();
    }

    private int freeSockets() {
        int free = 0;
        for (int i = FIRST_SOCKET; i < SLOTS; i++) if (items.getStackInSlot(i).isEmpty()) free++;
        return free;
    }

    /** Whether the sockets hold anything this gear cannot take: too many modules, or the wrong kind. */
    public boolean socketsValid() {
        ItemStack gear = items.getStackInSlot(GEAR);
        int used = 0;
        for (int i = FIRST_SOCKET; i < SLOTS; i++) {
            ItemStack module = items.getStackInSlot(i);
            if (module.isEmpty()) continue;
            used++;
            if (!Modules.fits(gear, module)) return false;
        }
        return used <= Modules.slots(gear);
    }

    /** Starts a run. The menu button lands here; the packet carries no values. */
    public void start() {
        if (working || items.getStackInSlot(GEAR).isEmpty() || !socketsValid()) return;
        working = true;
        progress = 0;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.5F, 1.7F);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, IntegratorBlockEntity entity) {
        if (level.isClientSide()) {
            if (entity.working && entity.progress < Rules.INTEGRATE_TICKS) entity.progress++;
            return;
        }
        entity.serverTick();
    }

    /** One tick of work. Waits without losing progress when power runs short, and stops if the job stops being valid. */
    public void serverTick() {
        if (!working) return;
        if (items.getStackInSlot(GEAR).isEmpty() || !socketsValid()) {
            working = false;
            progress = 0;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            return;
        }
        if (!energy.take(Rules.INTEGRATE_FE_PER_TICK)) return;
        progress++;
        setChanged();
        if (progress >= Rules.INTEGRATE_TICKS) finish();
    }

    private void finish() {
        ItemStack gear = items.getStackInSlot(GEAR).copy();
        List<ItemStack> modules = new ArrayList<>();
        for (int i = FIRST_SOCKET; i < SLOTS; i++) {
            ItemStack module = items.getStackInSlot(i);
            if (!module.isEmpty()) modules.add(module.copy());
            items.setStackInSlot(i, ItemStack.EMPTY);
        }
        Modules.install(gear, modules);
        // Remember the finished gear first, so writing it back is not taken for a new arrival and unpacked again.
        seen = gear.copy();
        items.setStackInSlot(GEAR, gear);
        working = false;
        progress = 0;
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        level.playSound(null, worldPosition, SoundEvents.SMITHING_TABLE_USE, SoundSource.BLOCKS, 0.9F, 1.3F);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        items.serialize(output.child("items"));
        output.putInt("energy", energy.getAmountAsInt());
        output.putInt("progress", progress);
        output.putBoolean("working", working);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items.deserialize(input.childOrEmpty("items"));
        energy.set(input.getIntOr("energy", 0));
        progress = input.getIntOr("progress", 0);
        working = input.getBooleanOr("working", false);
        seen = items.getStackInSlot(GEAR).copy();
    }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public Component getDisplayName() { return Component.translatable("block.singulo_arsenal.module_integrator"); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IntegratorMenu(id, inventory, this);
    }
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            for (int i = 0; i < items.getSlots(); i++) {
                net.minecraft.world.Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), items.getStackInSlot(i));
            }
        }
    }
}