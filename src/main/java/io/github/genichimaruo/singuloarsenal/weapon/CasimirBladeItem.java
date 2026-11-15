package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Two mirror plates with nothing between them. What cuts is the vacuum in the gap, and armor has little to say about
 * vacuum. Holding use draws the mirrors together; the next hit then ignores armor outright.
 */
public final class CasimirBladeItem extends PoweredSword {
    public CasimirBladeItem(net.minecraft.world.item.Item.Properties properties) {
        super(properties, WeaponTier.CASIMIR, 12, -2.4F, Rules.CASIMIR_CAPACITY, 4, Rarity.RARE);
    }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return Rules.CASIMIR_CLOSE_TICKS; }
    @Override public ItemUseAnimation getUseAnimation(ItemStack stack) { return ItemUseAnimation.NONE; }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
        if (Fe.stored(stack) < Rules.CASIMIR_FE) {
            if (!level.isClientSide()) player.sendOverlayMessage(Component.translatable("message.singulo_arsenal.no_energy"));
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel server && entity instanceof Player player && Fe.spend(stack, Rules.CASIMIR_FE)) {
            stack.set(ArsenalData.ARMED_UNTIL.get(), server.getGameTime() + Rules.CASIMIR_ARMED_TICKS);
            player.getCooldowns().addCooldown(player.getMainHandItem(), Weapons.cooled(stack, Rules.CASIMIR_COOLDOWN));
            Weapons.beginReload(stack, server.getGameTime(), Weapons.cooled(stack, Rules.CASIMIR_COOLDOWN));
            Weapons.sound(player, "casimir_cavity_running", 1, 1.7F);
        }
        return stack;
    }

    /** True once per arming, on the hit that spends it. */
    boolean consumeArmed(Player attacker, ItemStack stack, LivingEntity target) {
        if (!(attacker.level() instanceof ServerLevel level) || ArsenalData.time(stack, ArsenalData.ARMED_UNTIL) <= level.getGameTime()) return false;
        stack.set(ArsenalData.ARMED_UNTIL.get(), 0L);
        FxPayload.send(level, FxPayload.RUPTURE, target.getBoundingBox().getCenter(), attacker.getLookAngle(), 1);
        Weapons.sound(target, "wormhole_mouth_ambient", 1.4F, 2);
        return true;
    }

    public static boolean armed(ItemStack stack, long now) {
        return ArsenalData.time(stack, ArsenalData.ARMED_UNTIL) > now;
    }
}
