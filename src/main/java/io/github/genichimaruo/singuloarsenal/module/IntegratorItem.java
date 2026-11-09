package io.github.genichimaruo.singuloarsenal.module;

public final class IntegratorItem extends net.minecraft.world.item.BlockItem {
    public IntegratorItem(net.minecraft.world.level.block.Block block) { super(block, new Properties()); }
    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        io.github.genichimaruo.singuloarsenal.client.ArsenalClient.extensions(consumer);
    }
}
