package io.github.genichimaruo.singuloarsenal.mixin;

import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingArmorMixin {
    @Redirect(method = "getDamageAfterArmorAbsorb", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/damagesource/CombatRules;getDamageAfterAbsorb(FFF)F"))
    private float arsenalArmor(float raw, float armor, float toughness, DamageSource source, float incoming) {
        float armored = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(raw, armor, toughness);
        return Weapons.afterArmor((LivingEntity) (Object) this, source, raw, armored);
    }
}
