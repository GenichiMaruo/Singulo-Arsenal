package io.github.genichimaruo.singuloarsenal.armor;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** All costs, cooldowns and damage changes are authoritative on the server. */
public final class ArmorAbilities {
    private static final String LAST_COMBAT = "singulo_arsenal.last_combat";
    private static final String LAST_REQUEST = "singulo_arsenal.last_request";
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private ArmorAbilities() {}

    public static void register(IEventBus bus) {
        bus.addListener(ArmorAbilities::capabilities);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::tick);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ArmorAbilities::fall);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::incoming);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, ArmorAbilities::damage);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::afterDamage);
        NeoForge.EVENT_BUS.addListener(ArmorAbilities::knockback);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (ArmorTier tier : ArmorTier.values()) {
            if (tier.capacity > 0) {
                for (var item : ArsenalContent.ARMOR.get(tier).values()) {
                    event.registerItem(Capabilities.EnergyStorage.ITEM, (stack, ctx) -> ArsenalArmorItem.energy(stack), item.get());
                }
            }
        }
    }

    public static boolean wearing(Player player, EquipmentSlot slot, ArmorTier tier) {
        return player.getItemBySlot(slot).getItem() instanceof ArsenalArmorItem armor && armor.tier == tier;
    }

    private static long now(Player player) { return player.level().getGameTime(); }

    private static boolean ready(Player player, ItemStack stack) {
        return now(player) >= ArmorData.time(stack, ArmorData.READY_AT) && !player.getCooldowns().isOnCooldown(stack.getItem());
    }

    private static void cooldown(Player player, ItemStack stack, int ticks) {
        stack.set(ArmorData.READY_AT.get(), now(player) + ticks);
        player.getCooldowns().addCooldown(stack.getItem(), ticks);
    }

    private static boolean pay(ItemStack stack, int amount) {
        var energy = ArsenalArmorItem.energy(stack);
        if (energy.extractEnergy(amount, true) != amount) return false;
        return energy.extractEnergy(amount, false) == amount;
    }

    private static void deny(ServerPlayer player, String reason) {
        player.displayClientMessage(Component.translatable("message.singulo_arsenal." + reason), true);
    }

    /** A packet supplies only an action, never energy, direction, cooldown or target values. */
    public static void activate(ServerPlayer player, int action) {
        if (!player.isAlive() || player.isSpectator() || player.isSleeping() || action < 0 || action > 2) return;
        CompoundTag data = player.getPersistentData();
        long now = now(player);
        if (data.contains(LAST_REQUEST) && now - data.getLong(LAST_REQUEST) < 5) return;
        data.putLong(LAST_REQUEST, now);
        switch (action) {
            case 0 -> vision(player);
            case 1 -> dodge(player);
            case 2 -> barrier(player);
            default -> { }
        }
    }

    private static void vision(ServerPlayer player) {
        if (!wearing(player, EquipmentSlot.HEAD, ArmorTier.EXPLORER)) { deny(player, "need_vision"); return; }
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!ready(player, helmet)) { deny(player, "cooling"); return; }
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 200, 0, false, false, true));
        cooldown(player, helmet, 600);
    }

    private static void dodge(ServerPlayer player) {
        if (!wearing(player, EquipmentSlot.LEGS, ArmorTier.INERTIAL)) { deny(player, "need_dodge"); return; }
        ItemStack legs = player.getItemBySlot(EquipmentSlot.LEGS);
        // The legs have their own small reservoir; the chest can feed them and enlarge collection.
        ItemStack reservoir = wearing(player, EquipmentSlot.CHEST, ArmorTier.INERTIAL)
                ? player.getItemBySlot(EquipmentSlot.CHEST) : legs;
        int impact = ArmorData.get(reservoir, ArmorData.IMPACT);
        boolean disabled = now(player) < ArmorData.time(reservoir, ArmorData.OVERLOADED_UNTIL)
                || player.isPassenger() || player.isInWater() || player.isFallFlying();
        if (!ArmorRules.canDodge(player.onGround(), disabled, impact, ArsenalArmorItem.energy(legs).getEnergyStored(), !ready(player, legs))) {
            deny(player, "dodge_unavailable"); return;
        }
        if (!pay(legs, ArmorRules.DODGE_FE)) return;
        reservoir.set(ArmorData.IMPACT.get(), impact - ArmorRules.DODGE_IMPACT);
        Vec3 look = player.getLookAngle();
        Vec3 direction = new Vec3(look.x, 0, look.z);
        if (direction.lengthSqr() < 0.001) direction = new Vec3(0, 0, 1).yRot((float) Math.toRadians(-player.getYRot()));
        direction = direction.normalize().scale(0.85);
        player.setDeltaMovement(direction.x, player.getDeltaMovement().y, direction.z);
        player.hurtMarked = true;
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_FLAP, SoundSource.PLAYERS, 0.25F, 1.8F);
        player.serverLevel().sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2, player.getZ(), 6, 0.2, 0.1, 0.2, 0.02);
        cooldown(player, legs, ArmorRules.DODGE_COOLDOWN);
    }

    private static void barrier(ServerPlayer player) {
        if (!wearing(player, EquipmentSlot.CHEST, ArmorTier.BOUNDARY)) { deny(player, "need_barrier"); return; }
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ready(player, chest)) { deny(player, "cooling"); return; }
        if (!pay(chest, ArmorRules.BARRIER_START_FE)) { deny(player, "no_energy"); return; }
        chest.set(ArmorData.SHIELD.get(), ArmorRules.BARRIER_POOL);
        chest.set(ArmorData.ACTIVE_UNTIL.get(), now(player) + (wearing(player, EquipmentSlot.LEGS, ArmorTier.BOUNDARY) ? 100L : 60L));
        cooldown(player, chest, ArmorRules.BARRIER_COOLDOWN);
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.5F, 1.6F);
    }

    private static void tick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        long now = now(player);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (wearing(player, EquipmentSlot.CHEST, ArmorTier.BOUNDARY)) {
            if (ArmorData.active(chest, now) && !pay(chest, ArmorRules.BARRIER_TICK_FE)) {
                chest.set(ArmorData.SHIELD.get(), 0);
            }
            if (!ArmorData.active(chest, now) && ArmorData.get(chest, ArmorData.SHIELD) > 0) chest.set(ArmorData.SHIELD.get(), 0);
            if (ArmorData.active(chest, now) && player.tickCount % 5 == 0) {
                for (int i = 0; i < 8; i++) {
                    double angle = Math.PI * i / 4 + now * 0.1;
                    player.serverLevel().sendParticles(ParticleTypes.END_ROD,
                            player.getX() + Math.cos(angle) * 0.65, player.getY() + 1, player.getZ() + Math.sin(angle) * 0.65,
                            1, 0, 0, 0, 0);
                }
            }
        }
        if (wearing(player, EquipmentSlot.FEET, ArmorTier.CATALYTIC) && !player.isInLava() && player.getRemainingFireTicks() > 40) {
            player.setRemainingFireTicks(40);
        }
        if (player.tickCount % 20 != 0) return;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (piece.getItem() instanceof ArsenalArmorItem armor && armor.tier == ArmorTier.INERTIAL) {
                int impact = ArmorData.get(piece, ArmorData.IMPACT);
                if (impact > 0) piece.set(ArmorData.IMPACT.get(), impact - 1);
            }
        }
        if (wearing(player, EquipmentSlot.CHEST, ArmorTier.CATALYTIC)) repair(player, chest);
    }

    /** One repair point/second total; leggings extend the chest's repair to other catalytic pieces. */
    public static void repair(Player player, ItemStack chest) {
        long lastCombat = player.getPersistentData().getLong(LAST_COMBAT);
        ItemStack target = chest.isDamaged() ? chest : ItemStack.EMPTY;
        if (target.isEmpty() && wearing(player, EquipmentSlot.LEGS, ArmorTier.CATALYTIC)) {
            for (EquipmentSlot slot : SLOTS) {
                if (wearing(player, slot, ArmorTier.CATALYTIC) && player.getItemBySlot(slot).isDamaged()) {
                    target = player.getItemBySlot(slot); break;
                }
            }
        }
        if (target.isEmpty() || now(player) - lastCombat < ArmorRules.COMBAT_DELAY) return;
        int reserve = ArmorData.get(chest, ArmorData.REPAIR);
        if (reserve == 0) {
            // Refill only when a real repair is possible. No fuel is wasted on pristine equipment.
            for (ItemStack fuel : player.getInventory().items) {
                if (fuel.is(ArsenalContent.CARTRIDGE.get())) {
                    fuel.shrink(1); reserve = ArmorRules.REPAIR_UNITS; break;
                }
            }
        }
        if (!ArmorRules.canRepair(now(player), lastCombat, target.isDamaged(), reserve)) return;
        target.setDamageValue(target.getDamageValue() - 1);
        chest.set(ArmorData.REPAIR.get(), reserve - 1);
    }

    private static void incoming(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getAmount() > 0) {
            player.getPersistentData().putLong(LAST_COMBAT, now(player));
        }
    }

    /** Apply the shield after armor/potion reductions, before vanilla absorption, preserving excess damage. */
    private static void damage(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !wearing(player, EquipmentSlot.CHEST, ArmorTier.BOUNDARY)
                || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ArmorData.active(chest, now(player))) return;
        int pool = ArmorData.get(chest, ArmorData.SHIELD);
        float absorbed = ArmorRules.absorbed(event.getNewDamage(), pool);
        if (absorbed <= 0) return;
        chest.set(ArmorData.SHIELD.get(), ArmorRules.poolAfter(absorbed, pool));
        event.setNewDamage(event.getNewDamage() - absorbed);
    }

    private static void afterDamage(LivingDamageEvent.Post event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker && event.getNewDamage() > 0) {
            attacker.getPersistentData().putLong(LAST_COMBAT, now(attacker));
        }
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0
                || event.getSource().getEntity() == null || event.getSource().getEntity() == player) return;
        ItemStack reservoir = wearing(player, EquipmentSlot.CHEST, ArmorTier.INERTIAL) ? player.getItemBySlot(EquipmentSlot.CHEST)
                : wearing(player, EquipmentSlot.LEGS, ArmorTier.INERTIAL) ? player.getItemBySlot(EquipmentSlot.LEGS) : ItemStack.EMPTY;
        collect(player, reservoir, ArmorRules.impactFromDamage(event.getNewDamage()));
    }

    private static void collect(Player player, ItemStack stack, int amount) {
        if (stack.isEmpty() || amount <= 0 || now(player) < ArmorData.time(stack, ArmorData.OVERLOADED_UNTIL)
                || now(player) < ArmorData.time(stack, ArmorData.COLLECT_AT)) return;
        int capacity = stack.getItem() instanceof ArsenalArmorItem armor && armor.getEquipmentSlot() == EquipmentSlot.CHEST
                ? ArmorRules.IMPACT_CAPACITY : 40;
        int stored = Math.min(capacity, Math.min(capacity, ArmorData.get(stack, ArmorData.IMPACT)) + amount);
        stack.set(ArmorData.COLLECT_AT.get(), now(player) + 20);
        stack.set(ArmorData.IMPACT.get(), stored);
        if (stored >= capacity) {
            stack.set(ArmorData.IMPACT.get(), 0);
            stack.set(ArmorData.OVERLOADED_UNTIL.get(), now(player) + ArmorRules.OVERLOAD_TICKS);
            player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.5F, 0.7F);
            if (player instanceof ServerPlayer serverPlayer) deny(serverPlayer, "overloaded");
        }
    }

    private static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (wearing(player, EquipmentSlot.FEET, ArmorTier.EXPLORER)) {
            // Feather Falling style cushioning, not fall immunity.
            event.setDistance(Math.max(0, event.getDistance() - 3));
        } else if (wearing(player, EquipmentSlot.FEET, ArmorTier.INERTIAL)) {
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            if (event.getDistance() > 3 && pay(boots, 200)) {
                ItemStack reservoir = wearing(player, EquipmentSlot.CHEST, ArmorTier.INERTIAL) ? player.getItemBySlot(EquipmentSlot.CHEST)
                        : wearing(player, EquipmentSlot.LEGS, ArmorTier.INERTIAL) ? player.getItemBySlot(EquipmentSlot.LEGS) : boots;
                collect(player, reservoir, ArmorRules.impactFromFall(event.getDistance()));
                event.setDamageMultiplier(event.getDamageMultiplier() * 0.5F);
            }
        } else if (wearing(player, EquipmentSlot.FEET, ArmorTier.BOUNDARY) && wearing(player, EquipmentSlot.CHEST, ArmorTier.BOUNDARY)
                && ArmorData.active(player.getItemBySlot(EquipmentSlot.CHEST), now(player))) {
            event.setDamageMultiplier(event.getDamageMultiplier() * 0.25F);
        }
    }

    private static void knockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (wearing(player, EquipmentSlot.CHEST, ArmorTier.INERTIAL)) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            if (now(player) >= ArmorData.time(chest, ArmorData.OVERLOADED_UNTIL) && pay(chest, 100)) {
                event.setStrength(event.getStrength() * 0.4F);
            }
        } else if (wearing(player, EquipmentSlot.FEET, ArmorTier.BOUNDARY) && wearing(player, EquipmentSlot.CHEST, ArmorTier.BOUNDARY)
                && ArmorData.active(player.getItemBySlot(EquipmentSlot.CHEST), now(player))) {
            event.setStrength(event.getStrength() * 0.25F);
        }
    }
}
