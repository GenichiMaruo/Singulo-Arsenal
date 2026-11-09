package io.github.genichimaruo.singuloarsenal.core;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ArsenalDamage {
    public static final ResourceKey<DamageType> QUENCH = key("quench");
    public static final ResourceKey<DamageType> COLLAPSE = key("collapse");
    public static final ResourceKey<DamageType> JET = key("jet");
    public static final ResourceKey<DamageType> SHEAR = key("shear");
    public static final ResourceKey<DamageType> HORIZON = key("horizon");
    public static final ResourceKey<DamageType> BIT = key("bit");

    private ArsenalDamage() {}

    private static ResourceKey<DamageType> key(String name) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, SinguloArsenal.id(name));
    }

    public static DamageSource source(ServerLevel level, ResourceKey<DamageType> type, Entity attacker) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(type), attacker);
    }

    /** The only definition of "may this player's gear hurt that": never themselves, allies, creative or PvP-protected players. */
    public static boolean canHit(Player attacker, LivingEntity target) {
        return target != attacker && target.isAlive() && !target.isSpectator() && !(target instanceof ArmorStand) && !target.isAlliedTo(attacker)
                && (!(target instanceof Player other) || (!other.isCreative() && attacker.canHarmPlayer(other)));
    }

    public static List<LivingEntity> around(Player attacker, Vec3 center, double radius) {
        return attacker.level().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                e -> canHit(attacker, e) && e.getBoundingBox().distanceToSqr(center) <= radius * radius);
    }

    /** Everything a straight segment passes through. Width is added to the bounding boxes. */
    public static List<LivingEntity> along(Player attacker, Vec3 from, Vec3 to, double width) {
        return attacker.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(width + 1),
                e -> {
                    if (!canHit(attacker, e)) return false;
                    AABB bounds = e.getBoundingBox().inflate(width);
                    return bounds.contains(from) || bounds.clip(from, to).isPresent();
                });
    }
}
