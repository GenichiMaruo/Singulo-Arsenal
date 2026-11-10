package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.core.Fe;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.level.BlockEvent;

/** What happens when a powered tool breaks a block: the FE is paid, and whatever else the tool takes with it goes too, as this player. */
public final class Mining {
    /** Players whose extra blocks are being broken now, so those breaks do not start more of their own. */
    private static final Set<UUID> BUSY = new HashSet<>();

    private Mining() {}

    public static void register() {
        // Lowest: if a claim or anything else cancels the break, none of this happens.
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, Mining::broken);
        NeoForge.EVENT_BUS.addListener(Mining::drops);
    }

    private static void broken(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || BUSY.contains(player.getUUID())) return;
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof PoweredTool tool) || !Fe.spend(stack, tool.fePerBlock())) return;
        List<BlockPos> more = tool.alsoBreaks(player, stack, event.getPos(), event.getState());
        if (more.isEmpty()) return;
        BUSY.add(player.getUUID());
        try {
            for (BlockPos pos : more) {
                if (!Fe.spend(stack, tool.fePerBlock())) break;
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            BUSY.remove(player.getUUID());
        }
    }

    /** A drill's hole swallows what it cuts: drops and experience arrive directly, and only what will not fit is left behind. */
    private static void drops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof ServerPlayer player) || !(event.getTool().getItem() instanceof AccretionDrillItem)) return;
        event.getDrops().removeIf(drop -> absorb(player, drop));
        player.giveExperiencePoints(event.getDroppedExperience());
        event.setDroppedExperience(0);
    }

    private static boolean absorb(ServerPlayer player, ItemEntity drop) {
        ItemStack stack = drop.getItem();
        player.getInventory().add(stack);
        return stack.isEmpty();
    }

    /** Whether a tool may take this block along: never anything holding data, a Singulo machine part, or much harder than what was struck. */
    static boolean mayTake(ServerLevel level, ItemStack stack, BlockPos pos, BlockState state, BlockState struck, BlockPos origin) {
        if (state.isAir() || state.hasBlockEntity() || !stack.isCorrectToolForDrops(state)) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > struck.getDestroySpeed(level, origin) * 2 + 1) return false;
        return !BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().startsWith("singulo");
    }

    /** The square of blocks around the struck one, in the plane of the face being mined. */
    static List<BlockPos> square(ServerPlayer player, ItemStack stack, BlockPos origin, BlockState struck, int reach) {
        ServerLevel level = player.serverLevel();
        HitResult hit = player.pick(player.blockInteractionRange(), 1, false);
        Direction face = hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block.getDirection() : Direction.orderedByNearest(player)[0];
        Direction.Axis axis = face.getAxis();
        List<BlockPos> out = new ArrayList<>();
        for (int a = -reach; a <= reach; a++) {
            for (int b = -reach; b <= reach; b++) {
                if (a == 0 && b == 0) continue;
                BlockPos pos = switch (axis) {
                    case X -> origin.offset(0, a, b);
                    case Y -> origin.offset(a, 0, b);
                    case Z -> origin.offset(a, b, 0);
                };
                if (mayTake(level, stack, pos, level.getBlockState(pos), struck, origin)) out.add(pos);
            }
        }
        return out;
    }

    /** Everything touching the origin, corners included, that matches. The origin is not in the result. */
    static List<BlockPos> connected(BlockPos origin, Predicate<BlockPos> matches, int limit) {
        Set<BlockPos> seen = new HashSet<>(List.of(origin));
        Deque<BlockPos> frontier = new ArrayDeque<>(List.of(origin));
        List<BlockPos> out = new ArrayList<>();
        while (!frontier.isEmpty() && out.size() < limit) {
            BlockPos at = frontier.poll();
            for (BlockPos next : BlockPos.betweenClosed(at.offset(-1, -1, -1), at.offset(1, 1, 1))) {
                BlockPos fixed = next.immutable();
                if (out.size() < limit && seen.add(fixed) && matches.test(fixed)) {
                    out.add(fixed);
                    frontier.add(fixed);
                }
            }
        }
        return out;
    }
}
