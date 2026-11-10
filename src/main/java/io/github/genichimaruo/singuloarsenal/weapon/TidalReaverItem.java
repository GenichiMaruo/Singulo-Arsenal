package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.ArsenalDamage;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * A cleaver with a contained tidal field. Hits leave tidal stress on the target; holding use opens a Roche focus that drags
 * everything nearby inward, and releasing shears them with damage that scales with the stress they carry. Spaghettification
 * is visible: stressed bodies are drawn out along their height.
 */
public final class TidalReaverItem extends PoweredSword {
    public TidalReaverItem() {
        super(WeaponTier.REAVER, 21, -3.0F, Rules.REAVER_CAPACITY, 5, Rarity.EPIC);
    }

    @Override public boolean accretes() { return true; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return Rules.REAVER_MAX_HOLD; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    public static Vec3 focus(LivingEntity entity, float partialTick) {
        return entity.getEyePosition(partialTick).add(entity.getViewVector(partialTick).scale(Rules.REAVER_FOCUS));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (Fe.stored(stack) < Rules.REAVER_FE_PER_10_TICKS) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("message.singulo_arsenal.no_energy"), true);
            return InteractionResultHolder.fail(stack);
        }
        player.startUsingItem(hand);
        if (!level.isClientSide) stack.set(ArsenalData.FOCUS_START.get(), level.getGameTime());
        return InteractionResultHolder.consume(stack);
    }

    /** How far the Roche focus has grown from 0 to 1, or -1 when it is closed. Read from the stack, so it is the same on every client. */
    public static float focusProgress(ItemStack stack, long now, float partial) {
        if (!stack.has(ArsenalData.FOCUS_START.get())) return -1;
        return Math.max(0, Math.min(1, (now - ArsenalData.time(stack, ArsenalData.FOCUS_START) + partial) / Rules.REAVER_MAX_HOLD));
    }

    /** Closes the focus however the hold ended: released, out of power, or the blade put away. */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !stack.has(ArsenalData.FOCUS_START.get()) || !(entity instanceof LivingEntity holder)) return;
        if (!holder.isUsingItem() || holder.getMainHandItem() != stack) stack.remove(ArsenalData.FOCUS_START.get());
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return;
        int held = Rules.REAVER_MAX_HOLD - remaining;
        if (held > 0 && held % 10 == 0 && !Fe.spend(stack, Rules.REAVER_FE_PER_10_TICKS)) {
            player.stopUsingItem();
            player.getCooldowns().addCooldown(this, Rules.LANCE_SHORT_COOLDOWN);
            return;
        }
        if (held % 2 == 0) pull(player);
    }

    private void pull(Player player) {
        Vec3 focus = focus(player, 1);
        for (LivingEntity target : ArsenalDamage.around(player, focus, Rules.REAVER_RANGE)) {
            Vec3 toward = focus.subtract(target.getBoundingBox().getCenter());
            if (toward.lengthSqr() < Rules.REAVER_STOP * Rules.REAVER_STOP) continue;
            double strength = target.getMaxHealth() >= Rules.REAVER_HEAVY_HEALTH ? Rules.REAVER_HEAVY_PULL : Rules.REAVER_PULL;
            target.setDeltaMovement(target.getDeltaMovement().scale(0.7).add(toward.normalize().scale(strength)));
            target.hurtMarked = true;
            target.resetFallDistance();
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remaining) {
        stack.remove(ArsenalData.FOCUS_START.get());
        if (level instanceof ServerLevel server && entity instanceof Player player) {
            if (Rules.REAVER_MAX_HOLD - remaining >= Rules.REAVER_MIN_HOLD) shear(player, server);
            else player.getCooldowns().addCooldown(this, Rules.LANCE_SHORT_COOLDOWN);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        stack.remove(ArsenalData.FOCUS_START.get());
        if (level instanceof ServerLevel server && entity instanceof Player player) shear(player, server);
        return stack;
    }

    /** Public so GameTests exercise the real path. */
    public void shear(Player player, ServerLevel level) {
        Vec3 focus = focus(player, 1);
        var source = ArsenalDamage.source(level, ArsenalDamage.SHEAR, player);
        for (LivingEntity target : ArsenalDamage.around(player, focus, Rules.REAVER_RANGE)) {
            int stress = Weapons.stress(target);
            if (target.hurt(source, Weapons.amplified(player.getMainHandItem(), Rules.shearDamage(stress)))) {
                Weapons.setStress(target, 0);
                FxPayload.sendEntity(level, FxPayload.STRETCH, target, focus, stress);
            }
        }
        player.getCooldowns().addCooldown(this, Weapons.cooled(player.getMainHandItem(), Rules.REAVER_COOLDOWN));
        Weapons.beginReload(player.getMainHandItem(), level.getGameTime(), Weapons.cooled(player.getMainHandItem(), Rules.REAVER_COOLDOWN));
        FxPayload.send(level, FxPayload.SHEAR, focus, player.getLookAngle(), (float) Rules.REAVER_RANGE);
        Weapons.soundAt(level, focus, "jet_forge.pulse", 2F, 1.3F);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.level() instanceof ServerLevel level) {
            int stress = Rules.stressAfterHit(Weapons.stress(target));
            Weapons.setStress(target, stress);
            FxPayload.sendEntity(level, FxPayload.STRETCH, target, target.position(), -stress);
        }
        return super.hurtEnemy(stack, target, attacker);
    }
}
