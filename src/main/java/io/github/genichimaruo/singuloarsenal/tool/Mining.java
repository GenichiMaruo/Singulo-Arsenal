package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
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
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.common.Tags;

/** What happens when a powered tool breaks a block: the FE is paid, and whatever else the tool takes with it goes too, as this player. */
public final class Mining {
    /** Players whose extra blocks are being broken now, so those breaks do not start more of their own. */
    private static final Set<UUID> BUSY = new HashSet<>();

    private Mining() {}

    /** Called by the tool after every BreakEvent listener has accepted the break. */
    public static void mined(ServerPlayer player, ItemStack stack, BlockPos origin, BlockState struck) {
        if (!(stack.getItem() instanceof PoweredTool tool) || !Fe.spend(stack, tool.fePerBlock()) || BUSY.contains(player.getUUID())) return;
        List<BlockPos> more = tool.alsoBreaks(player, stack, origin, struck);
        if (more.isEmpty()) return;
        ServerLevel level = player.serverLevel();
        BUSY.add(player.getUUID());
        try {
            for (BlockPos pos : more) {
                if (Fe.stored(stack) < tool.fePerBlock()) break;
                // destroyBlock handles claim events, but its caller normally checks spawn protection and the world border.
                if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos) || !level.mayInteract(player, pos)) continue;
                if (!mayTake(level, stack, pos, level.getBlockState(pos), struck, origin)) continue;
                // The successful break calls mined again and pays once. A refused break costs nothing.
                player.gameMode.destroyBlock(pos);
            }
        } finally {
            BUSY.remove(player.getUUID());
        }
    }

    /** Processes real block drops after Forge has accepted the break, before vanilla spawns them. */
    public static List<ItemStack> processDrops(ServerLevel level, BlockPos pos, net.minecraft.world.entity.Entity breaker,
            ItemStack tool, List<ItemStack> original) {
        if (!(breaker instanceof ServerPlayer player) || !(tool.getItem() instanceof PoweredTool)
                || !level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS) || level.restoringBlockSnapshots) return original;
        ItemStack held = player.getMainHandItem().getItem() == tool.getItem() ? player.getMainHandItem() : tool;
        List<ItemStack> drops = new ArrayList<>();
        float experience = 0;
        for (ItemStack stack : original) {
            ItemStack output = stack;
            if (Modules.count(tool, ModuleType.SMELTER) > 0 && (stack.is(Tags.Items.ORES) || stack.is(Tags.Items.RAW_MATERIALS))) {
                var input = new net.minecraft.world.SimpleContainer(stack);
                var recipe = level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level);
                if (recipe.isPresent()) {
                    ItemStack result = recipe.get().assemble(input, level.registryAccess());
                    if (!result.isEmpty() && Fe.spend(held, Rules.SMELT_FE * stack.getCount())) {
                        result.setCount(result.getCount() * stack.getCount());
                        experience += recipe.get().getExperience() * stack.getCount();
                        output = result;
                    }
                }
            }
            while (!output.isEmpty()) drops.add(output.split(output.getMaxStackSize()));
        }
        int whole = (int) experience;
        if (level.random.nextFloat() < experience - whole) whole++;
        if (tool.getItem() instanceof AccretionDrillItem) {
            drops.removeIf(stack -> { player.getInventory().add(stack); return stack.isEmpty(); });
            player.giveExperiencePoints(whole);
        } else if (whole > 0) net.minecraft.world.entity.ExperienceOrb.award(level, net.minecraft.world.phys.Vec3.atCenterOf(pos), whole);
        return drops;
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
        HitResult hit = player.pick(player.getAttributeValue(net.minecraftforge.common.ForgeMod.BLOCK_REACH.get()), 1, false);
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
                if (level.isLoaded(pos) && mayTake(level, stack, pos, level.getBlockState(pos), struck, origin)) out.add(pos);
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
