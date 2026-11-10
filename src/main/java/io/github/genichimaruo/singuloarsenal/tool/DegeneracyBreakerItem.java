package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A hammer of degenerate plate. One strike caves in a three-by-three face of stone or earth; sneak to take a single block. */
public final class DegeneracyBreakerItem extends PoweredTool {
    private static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, SinguloArsenal.id("mineable/breaker"));

    public DegeneracyBreakerItem() {
        super(ToolTier.PRESSURE, MINEABLE, 9, -3.2F, Rules.TIER4_TOOL_CAPACITY, 4, Rules.BREAKER_FE, Rarity.RARE);
    }

    @Override
    public List<BlockPos> alsoBreaks(ServerPlayer player, ItemStack stack, BlockPos origin, BlockState state) {
        return player.isShiftKeyDown() ? List.of() : Mining.square(player, stack, origin, state, 1);
    }
}
