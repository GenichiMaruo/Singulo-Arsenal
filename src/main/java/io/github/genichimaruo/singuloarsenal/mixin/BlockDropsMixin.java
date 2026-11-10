package io.github.genichimaruo.singuloarsenal.mixin;

import io.github.genichimaruo.singuloarsenal.tool.Mining;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Forge 1.20.1 has no BlockDropsEvent; intercept only the actual spawning path. */
@Mixin(Block.class)
public abstract class BlockDropsMixin {
    @Redirect(method = "dropResources(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;Z)V", remap = false,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;", remap = true))
    private static List<ItemStack> arsenalDrops(BlockState state, ServerLevel level, BlockPos pos, BlockEntity blockEntity, Entity breaker, ItemStack tool) {
        return Mining.processDrops(level, pos, breaker, tool, Block.getDrops(state, level, pos, blockEntity, breaker, tool));
    }
}
