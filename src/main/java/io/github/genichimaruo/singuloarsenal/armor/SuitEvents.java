package io.github.genichimaruo.singuloarsenal.armor;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.core.ArsenalDamage;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import io.github.genichimaruo.singuloarsenal.weapon.ArsenalWeapon;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.TickEvent;

/** Every suit effect. All decisions are made on the server; only the flux pin also runs on the client, because movement is client-side. */
public final class SuitEvents {
    public static final EquipmentSlot[] SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final java.util.UUID SPEED = id("suit_speed");
    private static final java.util.UUID REACH = id("suit_reach");
    private static final java.util.UUID KNOCKBACK = id("suit_knockback");
    private static final java.util.UUID METRIC_FLIGHT = id("metric_flight"), METRIC_FALL = id("metric_fall");
    private static final String LAST_DEFLECT = "singulo_arsenal.last_deflect";
    private static final String LAST_ABILITY = "singulo_arsenal.last_ability";

    private SuitEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::pinTick);
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::serverTick);
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::projectile);
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::incoming);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOW, SuitEvents::damage);
        MinecraftForge.EVENT_BUS.addListener(EventPriority.LOWEST, SuitEvents::fall);
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::knockback);
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::equipment);
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::moduleAttributes);
        MinecraftForge.EVENT_BUS.addListener(SuitEvents::jump);
    }

    // ---- helpers ------------------------------------------------------------------------------------------------

    public static boolean wearing(LivingEntity entity, EquipmentSlot slot, ArmorSet set) {
        return entity.getItemBySlot(slot).getItem() instanceof SuitItem suit && suit.set == set;
    }

    public static boolean fullSet(LivingEntity entity, ArmorSet set) {
        for (EquipmentSlot slot : SLOTS) if (!wearing(entity, slot, set)) return false;
        return true;
    }

    private static long now(LivingEntity entity) { return entity.level().getGameTime(); }

    private static SoundEvent singulo(String name) {
        return BuiltInRegistries.SOUND_EVENT.get(new ResourceLocation("singulo", name));
    }

    private static void sound(Entity at, SoundEvent event, float volume, float pitch) {
        if (event != null) at.level().playSound(null, at.getX(), at.getY(), at.getZ(), event, SoundSource.PLAYERS, volume, pitch);
    }

    private static boolean pinRestricted(Player player) {
        return player.getAbilities().flying || player.isFallFlying() || player.isInWater() || player.isInLava()
                || player.isPassenger() || player.onClimbable() || player.isSpectator();
    }

    public static boolean pinned(Player player) {
        // Only the owning client knows its real velocity: the server and other clients see position updates, so for them any
        // airborne sneak counts as falling. The local check exists to keep a sneak-jump from being cut off on the way up.
        double verticalSpeed = player.isLocalPlayer() ? player.getDeltaMovement().y : -1;
        return wearing(player, EquipmentSlot.FEET, ArmorSet.MEISSNER)
                && Rules.shouldPin(player.onGround(), player.isShiftKeyDown(), pinRestricted(player),
                        verticalSpeed, Fe.stored(player.getItemBySlot(EquipmentSlot.FEET)));
    }

    // ---- flux pinning -------------------------------------------------------------------------------------------

    /** Runs on both sides: the client moves the player, the server pays the FE and forgives the fall. */
    private static void pinTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        Player player = event.player;
        if (!pinned(player)) return;
        if (player.isLocalPlayer()) {
            Vec3 motion = player.getDeltaMovement();
            player.setDeltaMovement(motion.x * 0.8, Rules.FLUX_CREEP, motion.z * 0.8);
        }
        player.resetFallDistance();
        if (!player.level().isClientSide && player.tickCount % 10 == 0) {
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            Fe.spend(boots, Math.min(Fe.stored(boots), Rules.PIN_FE_PER_10_TICKS));
        }
    }

    // ---- once-a-second upkeep -----------------------------------------------------------------------------------

    private static void serverTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player) || !player.isAlive() || player.isSpectator()) return;
        if (player.tickCount % 10 == 0) applyAttributes(player);
        metricDrive(player);
        if (player.tickCount % Rules.COLLECT_INTERVAL == 0) collect(player);
        if (player.tickCount % 20 == 0) upkeep(player);
    }

    /** Public so GameTests can drive it: mock players are not ticked by a network connection. */
    public static void upkeep(ServerPlayer player) {
        for (EquipmentSlot slot : SLOTS) Fe.accrete(player, player.getItemBySlot(slot));
        ItemStack held = player.getMainHandItem();
        // Fe.accrete itself checks that this is gear that feeds on pellets.
        if (!(held.getItem() instanceof SuitItem)) Fe.accrete(player, held);

        fireBits(player);
        induct(player);
        shareCurrent(player);
        filterHelmets(player);
        relaxLining(player);
    }

    /** Each bit shoots once a second at the nearest hostile thing it can see, paid for by the chestpiece. */
    public static void fireBits(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int bits = Modules.count(chest, ModuleType.BIT_DRONE) * Rules.BITS_PER_MODULE;
        if (bits == 0) return;
        List<Mob> targets = new ArrayList<>(player.level().getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(Rules.BIT_RANGE),
                e -> e instanceof Enemy && ArsenalDamage.canHit(player, e) && player.distanceTo(e) <= Rules.BIT_RANGE && player.hasLineOfSight(e)));
        if (targets.isEmpty()) return;
        targets.sort(java.util.Comparator.comparingDouble(player::distanceToSqr));
        var source = ArsenalDamage.source(player.serverLevel(), ArsenalDamage.BIT, player);
        for (int bit = 0; bit < bits; bit++) {
            if (!Fe.spend(chest, Rules.BIT_FE)) return;
            Mob target = targets.get(bit % targets.size());
            target.hurt(source, Rules.BIT_DAMAGE);
            FxPayload.sendEntity(player.serverLevel(), FxPayload.BIT_SHOT, player, target.getBoundingBox().getCenter(), bit);
        }
    }

    /**
     * Induction coils feed the chestpiece's charge into whatever the wearer carries that takes FE, this mod's or any other's,
     * the held items first. Public so GameTests can drive it.
     */
    public static void induct(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int coils = Modules.count(chest, ModuleType.INDUCTION);
        if (coils == 0) return;
        int budget = Rules.induced(Fe.stored(chest), Integer.MAX_VALUE, coils);
        List<ItemStack> carried = new ArrayList<>(List.of(player.getMainHandItem(), player.getOffhandItem()));
        for (ItemStack stack : player.getInventory().items) if (stack != carried.get(0)) carried.add(stack);
        for (ItemStack stack : carried) {
            if (budget <= 0) return;
            if (stack.isEmpty()) continue;
            var storage = stack.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY).orElse(null);
            if (storage == null || !storage.canReceive()) continue;
            int got = storage.receiveEnergy(budget, false);
            if (got <= 0) continue;
            Fe.set(chest, Fe.stored(chest) - got);
            budget -= got;
        }
    }

    /**
     * Collector modules on any worn pieces add up to one field that brings loose items and experience to the wearer. Sneaking
     * turns it off, and what the wearer threw away stays where it fell. Public so GameTests can drive it.
     */
    public static void collect(ServerPlayer player) {
        if (player.isShiftKeyDown()) return;
        int modules = 0;
        ItemStack payer = ItemStack.EMPTY;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            int n = Modules.count(piece, ModuleType.COLLECTOR);
            if (n == 0) continue;
            modules += n;
            if (Fe.stored(piece) > Fe.stored(payer)) payer = piece;
        }
        double radius = Rules.collectRadius(modules);
        if (radius <= 0 || payer.isEmpty()) return;
        var box = player.getBoundingBox().inflate(radius);
        double reach = radius * radius;
        Vec3 at = player.position();
        var inventory = player.getInventory();
        for (ItemEntity item : player.level().getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.hasPickUpDelay()
                && e.getOwner() != player && e.distanceToSqr(player) <= reach && e.distanceToSqr(player) > 2.25)) {
            // Anything that could not be picked up would only be dragged along, and paid for again every time.
            if (inventory.getSlotWithRemainingSpace(item.getItem()) < 0 && inventory.getFreeSlot() < 0) continue;
            if (!Fe.spend(payer, Rules.COLLECT_FE)) return;
            item.teleportTo(at.x, at.y, at.z);
        }
        for (ExperienceOrb orb : player.level().getEntitiesOfClass(ExperienceOrb.class, box, e -> e.isAlive()
                && e.distanceToSqr(player) <= reach && e.distanceToSqr(player) > 2.25)) {
            if (!Fe.spend(payer, Rules.COLLECT_FE)) return;
            orb.teleportTo(at.x, at.y, at.z);
        }
    }

    /** Flight from electricity alone: the metric-drive module grants what Singulo's item does in zero-g mode, paid for only while airborne under power. */
    public static Attribute legacyAttribute(String name) {
        return java.util.Objects.requireNonNull(net.minecraftforge.registries.ForgeRegistries.ATTRIBUTES.getValue(new ResourceLocation("singulo", name)));
    }

    public static void metricDrive(ServerPlayer player) {
        ItemStack drive = ItemStack.EMPTY;
        for (EquipmentSlot slot : SLOTS) {
            ItemStack piece = player.getItemBySlot(slot);
            if (Modules.count(piece, ModuleType.METRIC_DRIVE) > 0 && Fe.stored(piece) >= Rules.METRIC_FE_PER_10_TICKS) {
                drive = piece;
                break;
            }
        }
        modifier(player, legacyAttribute("creative_flight"), METRIC_FLIGHT, drive.isEmpty() ? 0 : 1, AttributeModifier.Operation.ADDITION);
        modifier(player, legacyAttribute("fall_damage_multiplier"), METRIC_FALL, drive.isEmpty() ? 0 : -1, AttributeModifier.Operation.MULTIPLY_TOTAL);
        if (!drive.isEmpty() && player.getAbilities().flying && !player.getAbilities().instabuild && player.tickCount % 10 == 0) Fe.spend(drive, Rules.METRIC_FE_PER_10_TICKS);
    }

    private static void moduleAttributes(net.minecraftforge.event.ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof SuitItem suit) || event.getSlotType() != suit.getEquipmentSlot()) return;
        int plating = Modules.count(stack, ModuleType.PLATING);
        if (plating > 0) {
            addToBase(event, Attributes.ARMOR, plating * Rules.PLATING_ARMOR, "module_armor_" + suit.getType().getName());
            addToBase(event, Attributes.ARMOR_TOUGHNESS, plating * Rules.PLATING_TOUGHNESS, "module_toughness_" + suit.getType().getName());
        }
        int thrusters = Modules.count(stack, ModuleType.THRUSTER);
        if (thrusters > 0) {
            event.addModifier(net.minecraftforge.common.ForgeMod.STEP_HEIGHT_ADDITION.get(), new AttributeModifier(id("module_step"), "Arsenal step", thrusters * Rules.THRUSTER_STEP, AttributeModifier.Operation.ADDITION));
        }
    }
    private static void addToBase(net.minecraftforge.event.ItemAttributeModifierEvent event, Attribute attribute, double extra, String fallback) {
        for (var modifier : List.copyOf(event.getModifiers().get(attribute))) {
            if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                event.removeModifier(attribute, modifier);
                event.addModifier(attribute, new AttributeModifier(modifier.getId(), modifier.getName(), modifier.getAmount() + extra, modifier.getOperation()));
                return;
            }
        }
        event.addModifier(attribute, new AttributeModifier(id(fallback), "Arsenal plating", extra, AttributeModifier.Operation.ADDITION));
    }

    private static void jump(net.minecraftforge.event.entity.living.LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        int thrusters = Modules.count(player.getItemBySlot(EquipmentSlot.FEET), ModuleType.THRUSTER);
        if (thrusters > 0) player.setDeltaMovement(player.getDeltaMovement().add(0, thrusters * Rules.THRUSTER_JUMP, 0));
    }

    /** Meissner: a persistent current flows without loss, so worn pieces settle to one level. */
    private static void shareCurrent(Player player) {
        if (!wearing(player, EquipmentSlot.LEGS, ArmorSet.MEISSNER)) return;
        List<ItemStack> pieces = new ArrayList<>();
        for (EquipmentSlot slot : SLOTS) {
            if (wearing(player, slot, ArmorSet.MEISSNER)) pieces.add(player.getItemBySlot(slot));
        }
        int total = 0;
        boolean even = true;
        for (ItemStack piece : pieces) {
            total += Fe.stored(piece);
            even &= Fe.stored(piece) == Fe.stored(pieces.get(0));
        }
        if (even) return;
        // Fill smaller tanks first so capacitor upgrades cannot discard excess charge.
        pieces.sort(java.util.Comparator.comparingInt(Fe::capacity));
        int remaining = total;
        for (int i = 0; i < pieces.size(); i++) {
            ItemStack piece = pieces.get(i);
            int share = Math.min(Fe.capacity(piece), remaining / (pieces.size() - i));
            Fe.set(piece, share);
            remaining -= share;
        }
    }

    private static void filterHelmets(ServerPlayer player) {
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (wearing(player, EquipmentSlot.HEAD, ArmorSet.DEGENERACY)) {
            boolean harmful = player.hasEffect(MobEffects.POISON) || player.hasEffect(MobEffects.WITHER);
            if (harmful && Fe.spend(helmet, Rules.FILTER_FE)) {
                player.removeEffect(MobEffects.POISON);
                player.removeEffect(MobEffects.WITHER);
            }
        } else if (wearing(player, EquipmentSlot.HEAD, ArmorSet.COLLAPSAR)) {
            // No-hair theorem: all a black hole remembers is mass, charge and spin.
            List<net.minecraft.world.effect.MobEffect> bad = new ArrayList<>();
            for (MobEffectInstance effect : player.getActiveEffects()) {
                if (effect.getEffect().getCategory() == MobEffectCategory.HARMFUL) bad.add(effect.getEffect());
            }
            if (!bad.isEmpty() && Fe.spend(helmet, Rules.CLEANSE_FE)) bad.forEach(player::removeEffect);
        }
    }

    private static void relaxLining(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!(chest.getItem() instanceof SuitItem suit) || suit.set == ArmorSet.MEISSNER) return;
        int load = ArsenalData.get(chest, ArsenalData.LOAD);
        if (load > 0) ArsenalData.set(chest, ArsenalData.LOAD, load - 1);
    }

    /** Pasta phases and the Meissner legs. Transient modifiers are never written to the player's save. */
    public static void applyAttributes(Player player) {
        float speed = 0, reach = 0, knockback = 0;
        if (wearing(player, EquipmentSlot.LEGS, ArmorSet.MEISSNER)) speed = Rules.MEISSNER_LEG_SPEED;
        if (wearing(player, EquipmentSlot.CHEST, ArmorSet.COLLAPSAR) && wearing(player, EquipmentSlot.LEGS, ArmorSet.COLLAPSAR)) {
            switch (Rules.phaseOf(ArsenalData.get(player.getItemBySlot(EquipmentSlot.CHEST), ArsenalData.LOAD))) {
                case Rules.PHASE_GNOCCHI -> speed = Rules.GNOCCHI_SPEED;
                case Rules.PHASE_SPAGHETTI -> { reach = Rules.SPAGHETTI_REACH; knockback = Rules.SPAGHETTI_KNOCKBACK; }
                default -> speed = Rules.LASAGNA_SPEED;
            }
        }
        modifier(player, Attributes.MOVEMENT_SPEED, SPEED, speed, AttributeModifier.Operation.MULTIPLY_BASE);
        modifier(player, net.minecraftforge.common.ForgeMod.ENTITY_REACH.get(), REACH, reach, AttributeModifier.Operation.ADDITION);
        modifier(player, Attributes.KNOCKBACK_RESISTANCE, KNOCKBACK, knockback, AttributeModifier.Operation.ADDITION);
    }

    private static java.util.UUID id(String name) {
        return java.util.UUID.nameUUIDFromBytes(("singulo_arsenal:" + name).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
    private static void modifier(Player player, Attribute attribute, java.util.UUID id, double amount, AttributeModifier.Operation operation) {
        var instance = player.getAttribute(attribute);
        if (instance == null) return;
        instance.removeModifier(id);
        if (amount != 0) instance.addTransientModifier(new AttributeModifier(id, "Singulo Arsenal suit", amount, operation));
    }

    // ---- Meissner field -----------------------------------------------------------------------------------------

    private static void projectile(ProjectileImpactEvent event) {
        Projectile projectile = event.getProjectile();
        if (projectile.level().isClientSide || !(event.getRayTraceResult() instanceof EntityHitResult hit)
                || !(hit.getEntity() instanceof ServerPlayer player) || projectile.getOwner() == player
                || !wearing(player, EquipmentSlot.CHEST, ArmorSet.MEISSNER)) return;
        CompoundTag data = player.getPersistentData();
        if (data.contains(LAST_DEFLECT) && now(player) - data.getLong(LAST_DEFLECT) < Rules.DEFLECT_COOLDOWN) return;
        if (!Fe.spend(player.getItemBySlot(EquipmentSlot.CHEST), Rules.DEFLECT_FE)) return;
        data.putLong(LAST_DEFLECT, now(player));
        event.setCanceled(true);
        Vec3 away = projectile.position().subtract(player.getBoundingBox().getCenter()).normalize();
        projectile.setDeltaMovement(away.scale(Math.max(0.6, projectile.getDeltaMovement().length() * 0.5)));
        projectile.hurtMarked = true;
        projectile.setOwner(player);
        FxPayload.send(player.serverLevel(), FxPayload.RIPPLE, projectile.position(), away, 1);
        sound(player, SoundEvents.SHIELD_BLOCK, 0.8F, 1.3F);
    }

    // ---- damage -------------------------------------------------------------------------------------------------

    private static void incoming(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        boolean absolute = event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!absolute && chest.getItem() instanceof SuitItem suit && suit.set == ArmorSet.COLLAPSAR
                && ArsenalData.time(chest, ArsenalData.HORIZON_UNTIL) > now(player)) {
            event.setCanceled(true);
            return;
        }
        // Lenz's law: a conductor resists whatever pushes on it, so the attacker is thrown back.
        if (event.getSource().getDirectEntity() instanceof LivingEntity attacker && attacker != player
                && fullSet(player, ArmorSet.MEISSNER) && Fe.spend(chest, Rules.LENZ_FE)) {
            Vec3 push = attacker.position().subtract(player.position()).multiply(1, 0, 1);
            if (push.lengthSqr() > 1.0E-4) {
                push = push.normalize().scale(Rules.LENZ_PUSH);
                attacker.push(push.x, 0.2, push.z);
                attacker.hurtMarked = true;
                FxPayload.send(player.serverLevel(), FxPayload.RIPPLE, attacker.getEyePosition(), push.normalize().reverse(), 1);
            }
        }
    }

    private static void damage(LivingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        float damage = event.getAmount();
        if (damage <= 0) return;
        boolean absolute = event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY);
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);

        if (wearing(player, EquipmentSlot.LEGS, ArmorSet.DEGENERACY) && player.isShiftKeyDown() && player.onGround() && !absolute) {
            damage *= Rules.BRACE_FACTOR;
        }
        if (wearing(player, EquipmentSlot.CHEST, ArmorSet.DEGENERACY) && !absolute && now(player) >= ArsenalData.time(chest, ArsenalData.LOCKED_UNTIL)) {
            int pressure = Rules.pressureFrom(damage, Fe.stored(chest));
            if (pressure > 0) {
                Fe.spend(chest, pressure * Rules.PRESSURE_FE_PER_HP);
                damage -= pressure;
                int total = ArsenalData.get(chest, ArsenalData.LOAD) + pressure;
                ArsenalData.set(chest, ArsenalData.LOAD, Math.min(total, Rules.PRESSURE_MAX));
                FxPayload.send(player.serverLevel(), FxPayload.RIPPLE, player.getEyePosition(), Vec3.ZERO, 2);
                if (total >= Rules.PRESSURE_MAX) {
                    collapse(player, chest, Rules.PRESSURE_MAX);
                    ArsenalData.set(chest, ArsenalData.LOCKED_UNTIL, now(player) + Rules.COLLAPSE_LOCKOUT);
                    player.displayClientMessage(Component.translatable("message.singulo_arsenal.collapse"), true);
                }
            }
        }
        if (wearing(player, EquipmentSlot.CHEST, ArmorSet.COLLAPSAR)) {
            if (wearing(player, EquipmentSlot.LEGS, ArmorSet.COLLAPSAR) && !absolute
                    && Rules.phaseOf(ArsenalData.get(chest, ArsenalData.LOAD)) == Rules.PHASE_LASAGNA) {
                damage *= Rules.LASAGNA_FACTOR;
            }
            if (fullSet(player, ArmorSet.COLLAPSAR) && !absolute) damage = shield(player, chest, damage);
            if (fullSet(player, ArmorSet.COLLAPSAR) && Rules.horizonCatches(damage, player.getHealth(), absolute, Fe.stored(chest),
                    now(player) >= ArsenalData.time(chest, ArsenalData.HORIZON_READY))) {
                enterHorizon(player, chest);
                damage = 0;
            }
        }
        event.setAmount(damage);
        if (damage > 0 && wearing(player, EquipmentSlot.CHEST, ArmorSet.COLLAPSAR)) {
            ArsenalData.set(chest, ArsenalData.LOAD, Math.min(Rules.LOAD_MAX, ArsenalData.get(chest, ArsenalData.LOAD) + Rules.loadFromDamage(damage)));
        }
    }

    /**
     * The suit pays for the hit in FE instead of the wearer paying in health, from the other pieces first. While the event horizon is
     * ready, the chestpiece keeps back what it needs, so a suit running dry still has its last resort.
     */
    private static float shield(ServerPlayer player, ItemStack chest, float damage) {
        long reserve = now(player) >= ArsenalData.time(chest, ArsenalData.HORIZON_READY) ? Rules.HORIZON_FE : 0;
        float absorbed = 0;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST}) {
            ItemStack piece = player.getItemBySlot(slot);
            long available = Math.max(0, Fe.stored(piece) - (slot == EquipmentSlot.CHEST ? reserve : 0));
            float paid = Rules.shieldable(damage - absorbed, available);
            if (paid <= 0) continue;
            Fe.spend(piece, Math.min((int) available, (int) Math.ceil(paid * Rules.SHIELD_FE_PER_HP)));
            absorbed += paid;
            if (absorbed >= damage) break;
        }
        if (absorbed <= 0) return damage;
        // A hit the suit paid for still strains the lining, or the pasta phases would never move.
        ArsenalData.set(chest, ArsenalData.LOAD, Math.min(Rules.LOAD_MAX, ArsenalData.get(chest, ArsenalData.LOAD) + Rules.loadFromDamage(absorbed)));
        FxPayload.send(player.serverLevel(), FxPayload.RIPPLE, player.getBoundingBox().getCenter(), Vec3.ZERO, 1);
        return damage - absorbed;
    }

    private static void enterHorizon(ServerPlayer player, ItemStack chest) {
        Fe.spend(chest, Rules.HORIZON_FE);
        ArsenalData.set(chest, ArsenalData.HORIZON_UNTIL, now(player) + Rules.HORIZON_TICKS);
        ArsenalData.set(chest, ArsenalData.HORIZON_READY, now(player) + Rules.HORIZON_COOLDOWN);
        ArsenalData.set(chest, ArsenalData.LOAD, 0);
        player.setHealth(player.getMaxHealth() * Rules.HORIZON_HEALTH_FRACTION);
        Vec3 center = player.getBoundingBox().getCenter();
        var source = ArsenalDamage.source(player.serverLevel(), ArsenalDamage.HORIZON, player);
        for (LivingEntity target : ArsenalDamage.around(player, center, Rules.HORIZON_RADIUS)) target.hurt(source, Rules.HORIZON_DAMAGE);
        FxPayload.send(player.serverLevel(), FxPayload.HORIZON, center, Vec3.ZERO, (float) Rules.HORIZON_RADIUS);
        sound(player, singulo("black_hole.open"), 2.5F, 1);
    }

    /** Releases stored pressure as a shockwave: forced at the limit, or on command with the ability key. */
    private static void collapse(ServerPlayer player, ItemStack chest, int pressure) {
        ArsenalData.set(chest, ArsenalData.LOAD, 0);
        double radius = Rules.COLLAPSE_RADIUS * Math.max(0.5, (double) pressure / Rules.PRESSURE_MAX);
        Vec3 center = player.getBoundingBox().getCenter();
        var source = ArsenalDamage.source(player.serverLevel(), ArsenalDamage.COLLAPSE, player);
        for (LivingEntity target : ArsenalDamage.around(player, center, radius)) {
            if (target.hurt(source, Rules.collapseDamage(pressure))) {
                Vec3 away = target.position().subtract(player.position()).multiply(1, 0, 1);
                if (away.lengthSqr() > 1.0E-4) target.knockback(1.6, -away.x, -away.z);
            }
        }
        FxPayload.send(player.serverLevel(), FxPayload.COLLAPSE, center, Vec3.ZERO, (float) radius);
        sound(player, singulo("degenerate_furnace_press"), 2, 0.7F);
    }

    private static void fall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (wearing(player, EquipmentSlot.FEET, ArmorSet.COLLAPSAR)) {
            event.setDamageMultiplier(0);
            return;
        }
        if (!wearing(player, EquipmentSlot.FEET, ArmorSet.DEGENERACY)) return;
        float blocks = event.getDistance();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (blocks < 4 || !Fe.spend(boots, (int) (blocks * Rules.SLAM_FE_PER_BLOCK))) return;
        event.setDamageMultiplier(0);
        BlockPos below = player.blockPosition();
        Vec3 center = Vec3.atBottomCenterOf(below);
        double radius = Rules.slamRadius(blocks);
        var source = ArsenalDamage.source(player.serverLevel(), ArsenalDamage.COLLAPSE, player);
        for (LivingEntity target : ArsenalDamage.around(player, center, radius)) target.hurt(source, Rules.slamDamage(blocks));
        FxPayload.send(player.serverLevel(), FxPayload.SLAM, center, new Vec3(0, 1, 0), (float) radius);
        sound(player, SoundEvents.ANVIL_LAND, 1, 0.5F);
    }

    private static void knockback(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && wearing(player, EquipmentSlot.LEGS, ArmorSet.DEGENERACY)
                && player.isShiftKeyDown() && player.onGround()) {
            event.setCanceled(true);
        }
    }

    /** Equipment sounds are played here because the armor material's own sound would fire on every FE change. */
    private static void equipment(LivingEquipmentChangeEvent event) {
        if (event.getEntity().level().isClientSide || event.getSlot().getType() != EquipmentSlot.Type.ARMOR) return;
        if (event.getTo().getItem() instanceof SuitItem && !ItemStack.isSameItem(event.getFrom(), event.getTo())) {
            sound(event.getEntity(), SoundEvents.ARMOR_EQUIP_NETHERITE, 0.8F, 1.1F);
        }
    }

    // ---- ability key --------------------------------------------------------------------------------------------

    /** The packet carries no values: everything about the action is decided from server state. */
    public static void activate(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator() || player.isSleeping()) return;
        CompoundTag data = player.getPersistentData();
        if (data.contains(LAST_ABILITY) && now(player) - data.getLong(LAST_ABILITY) < 4) return;
        data.putLong(LAST_ABILITY, now(player));
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (wearing(player, EquipmentSlot.CHEST, ArmorSet.COLLAPSAR)) jetBurst(player, chest);
        else if (wearing(player, EquipmentSlot.CHEST, ArmorSet.DEGENERACY)) vent(player, chest);
        else player.displayClientMessage(Component.translatable("message.singulo_arsenal.no_ability"), true);
    }

    private static void vent(ServerPlayer player, ItemStack chest) {
        int pressure = ArsenalData.get(chest, ArsenalData.LOAD);
        if (now(player) < ArsenalData.time(chest, ArsenalData.LOCKED_UNTIL) || pressure < Rules.VENT_MINIMUM) {
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.vent_unavailable", Rules.VENT_MINIMUM), true);
            return;
        }
        collapse(player, chest, pressure);
    }

    private static void jetBurst(ServerPlayer player, ItemStack chest) {
        if (!wearing(player, EquipmentSlot.FEET, ArmorSet.COLLAPSAR)) {
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.need_boots"), true);
            return;
        }
        if (player.getCooldowns().isOnCooldown(chest.getItem())) return;
        if (!Fe.spend(chest, Rules.JET_BURST_FE)) {
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.no_energy"), true);
            return;
        }
        ArsenalData.set(chest, ArsenalData.LOAD, Math.max(0, ArsenalData.get(chest, ArsenalData.LOAD) - Rules.JET_BURST_LOAD));
        Vec3 look = player.getLookAngle().normalize();
        Vec3 center = player.getBoundingBox().getCenter();
        Vec3 rear = center.subtract(look.scale(Rules.JET_BURST_REAR_RANGE));
        var source = ArsenalDamage.source(player.serverLevel(), ArsenalDamage.JET, player);
        for (LivingEntity target : ArsenalDamage.along(player, center, rear, 0.8)) {
            target.hurt(source, Rules.JET_BURST_DAMAGE);
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 60));
        }
        player.setDeltaMovement(look.scale(Rules.JET_BURST_SPEED));
        player.hurtMarked = true;
        player.resetFallDistance();
        player.invulnerableTime = Math.max(player.invulnerableTime, 8);
        player.getCooldowns().addCooldown(chest.getItem(), Rules.JET_BURST_COOLDOWN);
        FxPayload.send(player.serverLevel(), FxPayload.JET_BURST, center, rear, 1);
        sound(player, singulo("jet_forge.jet"), 1.5F, 1.4F);
    }
}
