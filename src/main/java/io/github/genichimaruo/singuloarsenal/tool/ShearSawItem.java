package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

/** A saw whose chain runs in a shear field: cut one log of a living tree and the rest of the trunk comes down. Sneak to cut a single log. */
public final class ShearSawItem extends PoweredTool {
    public ShearSawItem() {
        super(ToolTier.PRESSURE, BlockTags.MINEABLE_WITH_AXE, 13, -3.0F, Rules.TIER4_TOOL_CAPACITY, 4, Rules.SAW_FE, Rarity.RARE);
    }

    @Override
    public List<BlockPos> alsoBreaks(ServerPlayer player, ItemStack stack, BlockPos origin, BlockState state) {
        if (player.isShiftKeyDown() || !state.is(BlockTags.LOGS)) return List.of();
        ServerLevel level = player.serverLevel();
        List<BlockPos> trunk = Mining.connected(origin, pos -> level.getBlockState(pos).is(BlockTags.LOGS), Rules.TREE_LIMIT);
        // Only a living tree falls. Log walls and beams in a building have no natural leaves on them.
        boolean alive = trunk.stream().anyMatch(pos -> hasNaturalLeaves(level, pos)) || hasNaturalLeaves(level, origin);
        return alive ? trunk : List.of();
    }

    private static boolean hasNaturalLeaves(ServerLevel level, BlockPos log) {
        for (Direction direction : Direction.values()) {
            BlockState next = level.getBlockState(log.relative(direction));
            if (next.is(BlockTags.LEAVES) && next.hasProperty(LeavesBlock.PERSISTENT) && !next.getValue(LeavesBlock.PERSISTENT)) return true;
        }
        return false;
    }
}
