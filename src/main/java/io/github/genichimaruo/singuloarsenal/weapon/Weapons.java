package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.ArsenalDamage;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.core.Tips;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
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

    static void describe(String id, ItemStack stack, int stage, List<Component> lines) {
        lines.add(Component.translatable("tooltip.singulo_arsenal." + id + ".summary").withStyle(ChatFormatting.GRAY));
        Tips.energyLine(stack, lines);
        Modules.describe(stack, lines);
        if (!Tips.detail()) {
            Tips.hint(lines);
            return;
        }
        for (int i = 1; i <= 3; i++) lines.add(Component.translatable("tooltip.singulo_arsenal." + id + "." + i).withStyle(ChatFormatting.DARK_AQUA));
        if (stack.getItem() instanceof Powered powered && powered.accretes()) {
            lines.add(Component.translatable("tooltip.singulo_arsenal.accretion").withStyle(ChatFormatting.DARK_AQUA));
        }
        lines.add(Component.translatable("tooltip.singulo_arsenal.charge").withStyle(ChatFormatting.DARK_GRAY));
        lines.add(Component.translatable("tooltip.singulo_arsenal.stage", stage).withStyle(ChatFormatting.GOLD));
    }

    /** A cooldown after this weapon's coolant modules. */
    public static int cooled(ItemStack stack, int ticks) {
        return Rules.cooled(ticks, Modules.count(stack, ModuleType.COOLANT));
    }

    public static float amplified(ItemStack stack, float damage) {
        return damage * Rules.amplified(Modules.count(stack, ModuleType.AMPLIFIER));
    }

    /** Starts a reload animation. Stored on the stack, because item cooldowns are only ever sent to their owner. */
    public static void beginReload(ItemStack stack, long now, int ticks) {
        stack.set(ArsenalData.RELOAD_UNTIL.get(), now + ticks);
        stack.set(ArsenalData.RELOAD_TICKS.get(), ticks);
    }

    /** Progress of the running reload from 0 to 1, or -1 when none is running. */
    public static float reloadProgress(ItemStack stack, long now, float partial) {
        long until = ArsenalData.time(stack, ArsenalData.RELOAD_UNTIL);
        int total = ArsenalData.get(stack, ArsenalData.RELOAD_TICKS);
        if (total <= 0 || until <= now) return -1;
        return Math.max(0, Math.min(1, 1 - (until - now - partial) / total));
    }

    static void sound(Entity at, String singuloEvent, float volume, float pitch) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("singulo", singuloEvent));
        if (event != null) at.level().playSound(null, at.getX(), at.getY(), at.getZ(), event, SoundSource.PLAYERS, volume, pitch);
    }

    static void soundAt(ServerLevel level, Vec3 at, String singuloEvent, float volume, float pitch) {
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("singulo", singuloEvent));
        if (event != null) level.playSound(null, at.x, at.y, at.z, event, SoundSource.PLAYERS, volume, pitch);
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
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getEntity() instanceof Player wielder
                && wielder.getMainHandItem().getItem() instanceof ArsenalWeapon) {
            event.setAmount(amplified(wielder.getMainHandItem(), event.getAmount()));
        }
        int stress = stress(target);
        if (stress > 0) event.setAmount(event.getAmount() * Rules.stressMultiplier(stress));

        if (event.getSource().is(ArsenalDamage.JET)) {
            event.addReductionModifier(DamageContainer.Reduction.ARMOR, (container, reduction) -> reduction * Rules.LANCE_ARMOR_KEPT);
            return;
        }
        if (event.getSource().is(DamageTypes.PLAYER_ATTACK) && event.getSource().getDirectEntity() instanceof Player attacker
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
