package io.github.genichimaruo.singuloarsenal.tool;

import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** A mining tool that runs on FE instead of wearing out. Out of power it still works, at the speed of a bare hand. */
public abstract class PoweredTool extends DiggerItem implements Powered {
    private final int capacity;
    private final int stage;
    private final int fePerBlock;

    protected PoweredTool(ToolTier tier, TagKey<Block> mineable, float damage, float attackSpeed, int capacity, int stage, int fePerBlock, Rarity rarity) {
        super(damage, attackSpeed, tier, mineable, new Properties().rarity(rarity).fireResistant());
        this.capacity = capacity;
        this.stage = stage;
        this.fePerBlock = fePerBlock;
    }

    @Override public void initializeClient(java.util.function.Consumer<net.minecraftforge.client.extensions.common.IClientItemExtensions> consumer) {
        io.github.genichimaruo.singuloarsenal.client.ArsenalClient.extensions(consumer);
    }
    @Override public int capacity() { return capacity; }
    public int stage() { return stage; }
    public int fePerBlock() { return fePerBlock; }

    /** Blocks that go along with the one being broken. The event handler pays for each and breaks it as this player. */
    public List<BlockPos> alsoBreaks(ServerPlayer player, ItemStack stack, BlockPos origin, BlockState state) {
        return List.of();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        if (speed <= 1) return speed;
        return Fe.stored(stack) < fePerBlock ? 1 : speed * Rules.overclocked(Modules.count(stack, ModuleType.OVERCLOCK));
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        if (miner instanceof ServerPlayer player) Mining.mined(player, stack, pos, state);
        return true;
    }
    @Override public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) { return true; }
    @Override public InteractionResult useOn(UseOnContext context) { return Fe.chargeFromBlock(context); }
    @Override public boolean shouldCauseReequipAnimation(ItemStack from, ItemStack to, boolean changed) { return !ItemStack.isSameItem(from, to); }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * Fe.stored(stack) / Math.max(1, Fe.capacity(stack))); }
    @Override public int getBarColor(ItemStack stack) { return 0x6FE3FF; }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level context, List<Component> lines, TooltipFlag flag) {
        Weapons.describe(BuiltInRegistries.ITEM.getKey(this).getPath(), stack, stage, lines);
    }
}
