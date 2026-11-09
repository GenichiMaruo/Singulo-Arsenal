package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.ArsenalDamage;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Damage rules shared by the weapons, plus the tooltip and sound helpers they all use. */
public final class Weapons {
    private static final String STRESS = "singulo_arsenal.tidal_stress";
    private static final String STRESS_UNTIL = "singulo_arsenal.tidal_stress_until";

    private Weapons() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(Weapons::incoming);
    }

    static void describe(String id, ItemStack stack, int capacity, List<Component> lines) {
        for (int i = 1; i <= 3; i++) {
            lines.add(Component.translatable("tooltip.singulo_arsenal." + id + "." + i).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("tooltip.singulo_arsenal.energy", Fe.stored(stack), capacity).withStyle(ChatFormatting.AQUA));
        if (stack.getItem() instanceof Powered powered && powered.accretes()) {
            lines.add(Component.translatable("tooltip.singulo_arsenal.accretion").withStyle(ChatFormatting.DARK_AQUA));
        }
        lines.add(Component.translatable("tooltip.singulo_arsenal.charge").withStyle(ChatFormatting.DARK_GRAY));
    }

    static void sound(Entity at, String singuloEvent, float volume, float pitch) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("singulo", singuloEvent));
        if (event != null) at.level().playSound(null, at.getX(), at.getY(), at.getZ(), event, SoundSource.PLAYERS, volume, pitch);
    }

    /** Tidal stress decays by expiry, not per tick, so a mob nobody is fighting costs nothing. */
    public static int stress(LivingEntity target) {
        CompoundTag data = target.getPersistentData();
        return data.getLong(STRESS_UNTIL) > target.level().getGameTime() ? Rules.clamp(data.getInt(STRESS), Rules.STRESS_MAX) : 0;
    }

    public static void setStress(LivingEntity target, int stress) {
        CompoundTag data = target.getPersistentData();
        if (stress <= 0) {
            data.remove(STRESS);
            data.remove(STRESS_UNTIL);
        } else {
            data.putInt(STRESS, stress);
            data.putLong(STRESS_UNTIL, target.level().getGameTime() + Rules.STRESS_TICKS);
        }
    }

    private static void incoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        int stress = stress(target);
        if (stress > 0) event.setAmount(event.getAmount() * Rules.stressMultiplier(stress));

        if (event.getSource().is(ArsenalDamage.JET)) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * Rules.LANCE_ARMOR_KEPT);
            return;
        }
        if (event.getSource().getDirectEntity() instanceof Player attacker
                && attacker.getMainHandItem().getItem() instanceof CasimirBladeItem blade
                && event.getSource().getEntity() == attacker) {
            if (blade.consumeArmed(attacker, attacker.getMainHandItem(), target)) {
                event.setAmount(event.getAmount() * Rules.CASIMIR_ARMED_BONUS);
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> 0F);
            } else {
                event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * Rules.CASIMIR_ARMOR_KEPT);
            }
        }
    }
}
