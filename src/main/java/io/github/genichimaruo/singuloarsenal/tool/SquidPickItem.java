package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.ScanPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.network.PacketDistributor;

/** A superconducting pick with a SQUID in its head. Right click reads the field around you and shows every ore nearby through the rock. */
public final class SquidPickItem extends PoweredTool {
    public SquidPickItem() {
        super(ToolTier.SQUID, BlockTags.MINEABLE_WITH_PICKAXE, 5, -2.8F, Rules.PICK_CAPACITY, 2, Rules.PICK_FE, Rarity.UNCOMMON);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.pass(stack);
        if (player instanceof ServerPlayer server) {
            if (!Fe.spend(stack, Rules.SCAN_FE)) {
                player.displayClientMessage(Component.translatable("message.singulo_arsenal.no_energy"), true);
                return InteractionResultHolder.fail(stack);
            }
            List<BlockPos> ores = scan(server);
            player.getCooldowns().addCooldown(this, Rules.SCAN_COOLDOWN);
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.scan", ores.size()), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1, 0.7F);
            if (server.connection.hasChannel(ScanPayload.TYPE)) PacketDistributor.sendToPlayer(server, new ScanPayload(ores));
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Public so GameTests exercise the real search. The nearest ores come first and the list is capped. */
    public static List<BlockPos> scan(ServerPlayer player) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        int r = Rules.SCAN_RADIUS;
        List<BlockPos> found = new ArrayList<>();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (level.isLoaded(pos) && level.getBlockState(pos).is(Tags.Blocks.ORES)) found.add(pos.immutable());
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return found.size() > Rules.SCAN_MAX ? List.copyOf(found.subList(0, Rules.SCAN_MAX)) : found;
    }
}
