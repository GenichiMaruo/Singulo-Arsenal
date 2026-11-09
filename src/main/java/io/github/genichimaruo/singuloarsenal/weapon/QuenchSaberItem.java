package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.ArsenalDamage;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.Vec3;

/**
 * Current climbs with every hit until the superconducting blade passes critical and quenches: all the stored field
 * dumps as heat at once. Without FE it is just a good sword.
 */
public final class QuenchSaberItem extends PoweredSword {
    public QuenchSaberItem() {
        super(WeaponTier.QUENCH, 8, -2.4F, Rules.QUENCH_CAPACITY, 2, Rarity.UNCOMMON);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker instanceof Player player && player.level() instanceof ServerLevel level && !player.getCooldowns().isOnCooldown(this)) {
            long now = level.getGameTime();
            int current = now - ArsenalData.time(stack, ArsenalData.LAST_HIT) > Rules.QUENCH_DECAY_TICKS ? 0 : ArsenalData.get(stack, ArsenalData.CURRENT);
            int next = Rules.currentAfterHit(current);
            ArsenalData.set(stack, ArsenalData.LAST_HIT, now);
            if (next >= Rules.QUENCH_MAX_CURRENT && Fe.spend(stack, Rules.QUENCH_FE)) {
                ArsenalData.set(stack, ArsenalData.CURRENT, 0);
                quench(player, level, target, stack);
            } else {
                ArsenalData.set(stack, ArsenalData.CURRENT, Math.min(next, Rules.QUENCH_MAX_CURRENT - 1));
            }
        }
        return super.hurtEnemy(stack, target, attacker);
    }

    private void quench(Player player, ServerLevel level, LivingEntity origin, ItemStack stack) {
        Vec3 center = origin.getBoundingBox().getCenter();
        var source = ArsenalDamage.source(level, ArsenalDamage.QUENCH, player);
        for (LivingEntity target : ArsenalDamage.around(player, center, Rules.QUENCH_RADIUS)) {
            target.invulnerableTime = 0;
            target.hurt(source, Weapons.amplified(stack, Rules.QUENCH_DAMAGE));
            target.setSecondsOnFire(3);
        }
        player.getCooldowns().addCooldown(this, Weapons.cooled(player.getMainHandItem(), Rules.QUENCH_COOLDOWN));
        Weapons.beginReload(player.getMainHandItem(), level.getGameTime(), Weapons.cooled(player.getMainHandItem(), Rules.QUENCH_COOLDOWN));
        FxPayload.send(level, FxPayload.QUENCH, center, Vec3.ZERO, (float) Rules.QUENCH_RADIUS);
        Weapons.sound(origin, "keraunos_tower.discharge", 1.1F, 1.3F);
    }
}
