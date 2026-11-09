package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.BlockEvent;

/**
 * What the jet may destroy. Only natural-style blocks: anything holding data, anything from Singulo (a multiblock's casing is a machine,
 * not terrain), and anything a claim or spawn protection forbids this player to break is left alone.
 */
final class Terrain {
    private static final float HARDEST = 50;

    private Terrain() {}

    private static boolean canBreak(ServerPlayer player, ServerLevel level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) return false;
        float hardness = state.getDestroySpeed(level, pos);
        if (hardness < 0 || hardness > HARDEST) return false;
        if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().startsWith("singulo")) return false;
        if (!player.mayBuild() || !level.mayInteract(player, pos)) return false;
        return !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, pos, state, player)).isCanceled();
    }

    private static boolean isStone(BlockState state) {
        return state.is(BlockTags.BASE_STONE_OVERWORLD) || state.is(BlockTags.BASE_STONE_NETHER) || state.is(Tags.Blocks.COBBLESTONES) || state.is(Tags.Blocks.ORES);
    }

    /**
     * The one bite the jet takes: a tunnel straight through everything ahead, in a single tick. It stops at the first thing it may not
     * break. Melted blocks drop nothing. Returns the deepest point where it actually melted something, or null if it touched no terrain.
     */
    static Vec3 drill(ServerPlayer player, ServerLevel level, Vec3 from, Vec3 direction, double range) {
        BlockHitResult first = level.clip(new ClipContext(from, from.add(direction.scale(range)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (first.getType() != HitResult.Type.BLOCK) return null;
        int reach = Mth.ceil(Rules.LANCE_BORE_RADIUS);
        double limit = Rules.LANCE_BORE_RADIUS * Rules.LANCE_BORE_RADIUS;
        Set<BlockPos> tried = new HashSet<>();
        Vec3 deepest = null;
        BlockPos previous = null;
        for (double distance = from.distanceTo(first.getLocation()) + 0.05; distance <= range; distance += 0.5) {
            Vec3 point = from.add(direction.scale(distance));
            BlockPos center = BlockPos.containing(point);
            if (center.equals(previous)) continue;
            previous = center;
            if (!level.isLoaded(center)) break;
            BlockState ahead = level.getBlockState(center);
            if (!ahead.isAir() && ahead.getFluidState().isEmpty() && !canBreak(player, level, center, ahead)) break;
            boolean melted = false;
            for (BlockPos pos : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
                if (pos.distSqr(center) > limit || !tried.add(pos.immutable())) continue;
                BlockState state = level.getBlockState(pos);
                if (canBreak(player, level, pos, state)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                    melted = true;
                }
            }
            if (melted) deepest = point;
        }
        return deepest;
    }

    /**
     * A rough bowl. Stone melts: gone inside, magma and a little lava in the wall. Everything else simply blows apart, the way an
     * explosion takes it, with an explosion's chance of dropping.
     */
    static void crater(ServerPlayer player, ServerLevel level, Vec3 at, double radius) {
        BlockPos center = BlockPos.containing(at);
        int reach = Mth.ceil(radius) + 2;
        for (BlockPos mutable : BlockPos.betweenClosed(center.offset(-reach, -reach, -reach), center.offset(reach, reach, reach))) {
            BlockPos pos = mutable.immutable();
            BlockState state = level.getBlockState(pos);
            if (state.isAir()) continue;
            double distance = Math.sqrt(pos.distSqr(center));
            double edge = radius * (0.82 + 0.18 * level.random.nextDouble());
            if (distance <= edge) {
                if (!canBreak(player, level, pos, state)) continue;
                if (!isStone(state) && level.random.nextDouble() < 1 / radius) Block.dropResources(state, level, pos, null, player, ItemStack.EMPTY);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            } else if (distance <= edge + 1.5 && isStone(state) && level.random.nextFloat() < 0.45F && canBreak(player, level, pos, state)) {
                boolean pool = pos.getY() < center.getY() && level.random.nextFloat() < 0.15F;
                level.setBlock(pos, pool ? Blocks.LAVA.defaultBlockState() : Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
            }
        }
    }
}
