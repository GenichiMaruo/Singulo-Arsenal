package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.ArsenalDamage;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A shouldered gun built around a contained black hole. A magnetic mirror seals the rear pole, so the whole jet leaves through the
 * barrel, in the same sequence as the Accretion Jet Forge: ignition, three collimation rings, then irradiation.
 */
public final class JetLanceItem extends Item implements ArsenalWeapon {
    public JetLanceItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override public int capacity() { return Rules.LANCE_CAPACITY; }
    @Override public boolean accretes() { return true; }
    @Override public int stage() { return 5; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return Rules.LANCE_MAX_HOLD; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }
    @Override public InteractionResult useOn(UseOnContext context) { return Fe.chargeFromBlock(context); }
    @Override public boolean shouldCauseReequipAnimation(ItemStack from, ItemStack to, boolean changed) { return !ItemStack.isSameItem(from, to); }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * Fe.stored(stack) / capacity()); }
    @Override public int getBarColor(ItemStack stack) { return 0xFFC36B; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (Fe.stored(stack) < Rules.LANCE_FE_PER_10_TICKS) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.singulo_arsenal.no_energy"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    public static int held(ItemStack stack, LivingEntity entity) {
        return Rules.LANCE_MAX_HOLD - entity.getUseItemRemainingTicks();
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return;
        int held = Rules.LANCE_MAX_HOLD - remaining;
        if (held == 1) Weapons.sound(player, "jet_forge.jet", 1.1F, 1.2F);
        if (held == Rules.LANCE_IGNITE || held == Rules.LANCE_IGNITE + 7 || held == Rules.LANCE_IGNITE + 14) Weapons.sound(player, "jet_forge.focus", 1, 1.1F);
        if (held < Rules.LANCE_COLLIMATE) return;
        if (held == Rules.LANCE_COLLIMATE) Weapons.sound(player, "jet_forge.beam_start", 1.6F, 1.2F);
        if ((held - Rules.LANCE_COLLIMATE) % 10 == 0 && !Fe.spend(stack, Rules.LANCE_FE_PER_10_TICKS)) {
            player.stopUsingItem();
            player.getCooldowns().addCooldown(this, Rules.LANCE_COOLDOWN);
            Weapons.beginReload(stack, server.getGameTime(), Rules.LANCE_COOLDOWN);
            return;
        }
        if ((held - Rules.LANCE_COLLIMATE) % Rules.LANCE_PULSE == 0) pulse(player, server);
        if ((held - Rules.LANCE_COLLIMATE) % 40 == 20) Weapons.sound(player, "jet_forge.beam", 1.2F, 1.15F);
    }

    /** Timed sound cues for the reload: one per step of the animation. Only for the held weapon. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !selected) return;
        float progress = Weapons.reloadProgress(stack, level.getGameTime(), 0);
        if (progress < 0) return;
        int tick = Math.round(progress * Rules.LANCE_COOLDOWN);
        if (tick == 8) Weapons.sound(entity, "sealed_container.vent", 0.9F, 1.1F);
        else if (tick == 34) Weapons.sound(entity, "drone_port.hatch_open", 0.9F, 1.5F);
        else if (tick == 92) Weapons.sound(entity, "sealed_container.insert", 1, 0.9F);
        else if (tick == 110) Weapons.sound(entity, "drone_port.clamp", 1, 0.8F);
        else if (tick == 118) Weapons.sound(entity, "drone_port.clamp", 1, 1.4F);
        else if (tick == 140) Weapons.sound(entity, "sealed_container.unlock", 0.8F, 1.3F);
    }

    /** Applies one damage pulse along the jet. Public so GameTests exercise the real path. */
    public static void pulse(ServerPlayer player, ServerLevel level) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        Vec3 end = level.clip(new ClipContext(eye, eye.add(look.scale(Rules.LANCE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player)).getLocation();
        var source = ArsenalDamage.source(level, ArsenalDamage.JET, player);
        for (LivingEntity target : ArsenalDamage.along(player, eye, end, 0.3)) burn(target, source, Rules.LANCE_DAMAGE);
    }

    private static void burn(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float damage) {
        if (target.hurt(source, damage)) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        if (!level.isClientSide && entity instanceof Player player) {
            boolean fired = Rules.LANCE_MAX_HOLD - remaining >= Rules.LANCE_COLLIMATE;
            player.getCooldowns().addCooldown(this, fired ? Rules.LANCE_COOLDOWN : Rules.LANCE_SHORT_COOLDOWN);
            if (fired) {
                Weapons.beginReload(stack, level.getGameTime(), Rules.LANCE_COOLDOWN);
                Weapons.sound(player, "jet_forge.cool", 1, 1.2F);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!level.isClientSide && entity instanceof Player player) {
            player.getCooldowns().addCooldown(this, Rules.LANCE_COOLDOWN);
            Weapons.beginReload(stack, level.getGameTime(), Rules.LANCE_COOLDOWN);
            Weapons.sound(player, "jet_forge.cool", 1, 1.2F);
        }
        return stack;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.singulo_arsenal.stage", stage()).withStyle(ChatFormatting.GOLD));
        Weapons.describe(BuiltInRegistries.ITEM.getKey(this).getPath(), stack, capacity(), lines);
    }
}
