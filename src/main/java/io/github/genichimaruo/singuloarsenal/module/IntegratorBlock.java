package io.github.genichimaruo.singuloarsenal.module;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class IntegratorBlock extends BaseEntityBlock {

    public IntegratorBlock(Properties properties) {
        super(properties);
    }

    /** The whole machine is drawn by its block entity renderer, so its arms and work piece can move. */
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.ENTITYBLOCK_ANIMATED; }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new IntegratorBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModuleContent.INTEGRATOR_ENTITY.get(), IntegratorBlockEntity::tick);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof IntegratorBlockEntity entity) {
            net.minecraftforge.network.NetworkHooks.openScreen((net.minecraft.server.level.ServerPlayer) player, entity, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Nothing a player put in here may vanish with the block. */
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState next, boolean moved) {
        if (!state.is(next.getBlock()) && level.getBlockEntity(pos) instanceof IntegratorBlockEntity entity) {
            for (int i = 0; i < IntegratorBlockEntity.SLOTS; i++) {
                Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), entity.items().getStackInSlot(i));
            }
        }
        super.onRemove(state, level, pos, next, moved);
    }
}
