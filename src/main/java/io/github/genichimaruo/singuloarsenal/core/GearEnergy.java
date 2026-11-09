package io.github.genichimaruo.singuloarsenal.core;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

public final class GearEnergy implements ICapabilityProvider {
    private final LazyOptional<IEnergyStorage> energy;
    public GearEnergy(ItemStack stack) { energy = LazyOptional.of(() -> Fe.storage(stack)); }
    @Override public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        return capability == ForgeCapabilities.ENERGY ? energy.cast() : LazyOptional.empty();
    }
}
