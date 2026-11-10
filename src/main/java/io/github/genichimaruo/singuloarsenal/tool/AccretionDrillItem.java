package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

/** A drill with a contained hole behind the bit. It cuts anything, and what it cuts falls straight into your pack. Right click changes how much it takes. */
public final class AccretionDrillItem extends PoweredTool {
    private static final TagKey<Block> MINEABLE = TagKey.create(Registries.BLOCK, SinguloArsenal.id("mineable/drill"));

    public AccretionDrillItem() {
        super(ToolTier.ACCRETION, MINEABLE, 11, -2.8F, Rules.DRILL_CAPACITY, 5, Rules.DRILL_FE, Rarity.EPIC);
    }

    @Override public boolean accretes() { return true; }

    public static int mode(ItemStack stack) {
        return Rules.clamp(ArsenalData.get(stack, ArsenalData.TOOL_MODE), Rules.DRILL_VEIN);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            int next = (mode(stack) + 1) % (Rules.DRILL_VEIN + 1);
            ArsenalData.set(stack, ArsenalData.TOOL_MODE, next);
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.drill_mode", Component.translatable("tool.singulo_arsenal.drill_mode." + next)), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_DISPENSE, SoundSource.PLAYERS, 0.6F, 1 + next * 0.2F);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public List<BlockPos> alsoBreaks(ServerPlayer player, ItemStack stack, BlockPos origin, BlockState state) {
        ServerLevel level = player.serverLevel();
        return switch (mode(stack)) {
            case Rules.DRILL_AREA -> Mining.square(player, stack, origin, state, 1);
            case Rules.DRILL_VEIN -> state.is(Tags.Blocks.ORES)
                    ? Mining.connected(origin, pos -> level.isLoaded(pos) && level.getBlockState(pos).is(state.getBlock())
                            && Mining.mayTake(level, stack, pos, level.getBlockState(pos), state, origin), Rules.VEIN_LIMIT)
                    : List.of();
            default -> List.of();
        };
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tool.singulo_arsenal.drill_mode." + mode(stack)).withStyle(ChatFormatting.YELLOW));
        super.appendHoverText(stack, context, lines, flag);
    }
}
