package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.ScanPayload;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.network.PacketDistributor;

/**
 * A superconducting pick with a SQUID in its head. Right click reads the field around you and shows the ores nearby through the rock.
 * Which ores show depends on its sensitivity, raised exactly as on Singulo's neutrino scanner: use a sensitivity module with the
 * pick in the other hand. A pick made from an already tuned scanner keeps that scanner's sensitivity.
 */
public final class SquidPickItem extends PoweredTool {
    private static final TagKey<Block> TIER_2 = BlockTags.create(new ResourceLocation("singulo", "scanner_tier_2"));
    private static final TagKey<Block> TIER_3 = BlockTags.create(new ResourceLocation("singulo", "scanner_tier_3"));
    private static final ResourceLocation SCANNER_TIER = new ResourceLocation("singulo", "scanner_tier");

    /** What a scan found. {@code hidden} counts the ores there that the pick is not yet sensitive enough to show. */
    public record Scan(List<BlockPos> ores, int hidden) {}

    public SquidPickItem() {
        super(ToolTier.SQUID, BlockTags.MINEABLE_WITH_PICKAXE, 5, -2.8F, Rules.PICK_CAPACITY, 2, Rules.PICK_FE, Rarity.UNCOMMON);
    }

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(SquidPickItem::tune);
    }

    public static int tier(ItemStack stack) {
        return Math.max(1, Math.min(Rules.SCAN_MAX_TIER, ArsenalData.get(stack, ArsenalData.SCAN_TIER)));
    }

    /** The same rule as Singulo's scanner, so an ore shows at the same sensitivity on both. */
    public static int oreTier(BlockState state) {
        if (state.is(TIER_3) || state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return 3;
        return state.is(TIER_2) ? 2 : 1;
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
            Scan found = scan(server, tier(stack));
            player.getCooldowns().addCooldown(this, Rules.SCAN_COOLDOWN);
            var message = Component.translatable("message.singulo_arsenal.scan", found.ores().size());
            if (found.hidden() > 0) message.append(Component.translatable("message.singulo_arsenal.scan_hidden", found.hidden()));
            player.displayClientMessage(message, true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1, 0.7F);
            if (io.github.genichimaruo.singuloarsenal.net.ArsenalNetwork.CHANNEL.isRemotePresent(server.connection.connection)) io.github.genichimaruo.singuloarsenal.net.ArsenalNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> server), new ScanPayload(found.ores()));
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Public so GameTests exercise the real search. The nearest ores come first and the list is capped. */
    public static Scan scan(ServerPlayer player, int tier) {
        Level level = player.level();
        BlockPos center = player.blockPosition();
        int r = Rules.SCAN_RADIUS;
        List<BlockPos> found = new ArrayList<>();
        int hidden = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-r, -r, -r), center.offset(r, r, r))) {
            if (!level.isLoaded(pos)) continue;
            BlockState state = level.getBlockState(pos);
            if (!state.is(Tags.Blocks.ORES)) continue;
            if (oreTier(state) <= tier) found.add(pos.immutable());
            else hidden++;
        }
        found.sort(Comparator.comparingDouble(pos -> pos.distSqr(center)));
        return new Scan(found.size() > Rules.SCAN_MAX ? List.copyOf(found.subList(0, Rules.SCAN_MAX)) : found, hidden);
    }

    /**
     * A Singulo sensitivity module used with this pick in the other hand tunes the pick, as it would the scanner. Caught before the
     * module's own use, which would only complain that there is no scanner.
     */
    private static void tune(PlayerInteractEvent.RightClickItem event) {
        ItemStack module = event.getItemStack();
        int tier = moduleTier(module);
        if (tier == 0) return;
        Player player = event.getEntity();
        ItemStack pick = player.getItemInHand(event.getHand() == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
        if (!(pick.getItem() instanceof SquidPickItem)) return;
        Level level = event.getLevel();
        event.setCanceled(true);
        int now = tier(pick);
        if (now >= tier) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.singulo.scanner_module.already", now), true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
        if (level.isClientSide) return;
        ArsenalData.set(pick, ArsenalData.SCAN_TIER, tier);
        if (!player.getAbilities().instabuild) module.shrink(1);
        player.displayClientMessage(Component.translatable("message.singulo.scanner_module.done", tier), true);
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1, 1.2F);
    }

    /** 2 or 3 for Singulo's Mk2 and Mk3 sensitivity modules, 0 for anything else. */
    public static int moduleTier(ItemStack stack) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!id.getNamespace().equals("singulo")) return 0;
        return switch (id.getPath()) {
            case "scanner_module_2" -> 2;
            case "scanner_module_3" -> 3;
            default -> 0;
        };
    }

    /**
     * Called while the ingredients are still in the grid, by a plain take and a shift-click alike: the scanner that went into the
     * head passes its sensitivity on.
     */
    @Override
    public void onCraftedBy(ItemStack stack, Level level, Player player) {
        for (Slot slot : player.containerMenu.slots) {
            if (!(slot.container instanceof CraftingContainer)) continue;
            var tag = slot.getItem().getTag();
            int scanner = tag == null ? 0 : tag.getInt("singulo:scanner_tier");
            if (scanner > tier(stack)) {
                ArsenalData.set(stack, ArsenalData.SCAN_TIER, Math.min(Rules.SCAN_MAX_TIER, scanner));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(1, Component.translatable("tooltip.singulo_arsenal.scan_tier", tier(stack), Rules.SCAN_MAX_TIER).withStyle(ChatFormatting.GRAY));
    }
}
