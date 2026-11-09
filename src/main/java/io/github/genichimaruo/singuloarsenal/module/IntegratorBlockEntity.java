package io.github.genichimaruo.singuloarsenal.module;

import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Builds modules into a piece of gear. Placing gear on the bed takes out whatever it already carries and lays it in the sockets;
 * running the machine writes the sockets back. A module is therefore always in exactly one place, in the gear or in a socket.
 */
public final class IntegratorBlockEntity extends BlockEntity implements MenuProvider {
    public static final int GEAR = 0, FIRST_SOCKET = 1, SOCKETS = 3, SLOTS = FIRST_SOCKET + SOCKETS;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                if (slot == GEAR) gearChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
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

    private static final class Energy extends EnergyStorage {
        Energy() {
            super(Rules.INTEGRATOR_CAPACITY, Rules.INTEGRATOR_CAPACITY, 0);
        }

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
    public EnergyStorage energy() { return energy; }
    public boolean working() { return working; }
    public float progress(float partial) { return Math.min(1, (progress + (working ? partial : 0)) / (float) Rules.INTEGRATE_TICKS); }

    public final ContainerData data = new ContainerData() {
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> progress * 100 / Rules.INTEGRATE_TICKS;
                case 1 -> (int) (energy.getEnergyStored() * 100L / energy.getMaxEnergyStored());
                default -> working ? 1 : 0;
            };
        }
        @Override public void set(int index, int value) {}
        @Override public int getCount() { return 3; }
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
        if (level.isClientSide) {
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
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("energy", energy.serializeNBT(registries));
        tag.putInt("progress", progress);
        tag.putBoolean("working", working);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        if (tag.contains("energy")) energy.deserializeNBT(registries, tag.get("energy"));
        progress = tag.getInt("progress");
        working = tag.getBoolean("working");
        seen = items.getStackInSlot(GEAR).copy();
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) { return saveWithoutMetadata(registries); }
    @Override public Packet<ClientGamePacketListener> getUpdatePacket() { return ClientboundBlockEntityDataPacket.create(this); }

    @Override public Component getDisplayName() { return Component.translatable("block.singulo_arsenal.module_integrator"); }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new IntegratorMenu(id, inventory, this);
    }
}
