package io.github.genichimaruo.singuloarsenal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Wrap instead of redirect so other armor mods (e.g. Apothic Attributes armor pen) can hook the same call. */
@Mixin(LivingEntity.class)
public abstract class LivingArmorMixin {
    @WrapOperation(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"))
    private float arsenalArmor(float raw, float armor, float toughness, Operation<Float> original, DamageSource source, float incoming) {
        float armored = original.call(raw, armor, toughness);
        return Weapons.afterArmor((LivingEntity) (Object) this, source, raw, armored);
    }
}
