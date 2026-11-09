package io.github.genichimaruo.singuloarsenal.gametest;

import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.module.IntegratorBlockEntity;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Integration tests that run with the real Singulo JAR. Excluded from the distributed JAR. */
@GameTestHolder(SinguloArsenal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArsenalGameTests {
    private static final String ARENA = "empty";
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final List<ArmorItem.Type> TYPES = ArsenalItems.PIECES;

    private ArsenalGameTests() {}

    private static ItemStack charged(ArmorSet set, ArmorItem.Type type) {
        ItemStack stack = new ItemStack(ArsenalItems.SUITS.get(set).get(type).get());
        Fe.set(stack, Fe.capacity(stack));
        return stack;
    }

    private static ServerPlayer player(GameTestHelper h, double x, double z) {
        ServerPlayer player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(new Vec3(x, 1, z));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        player.setHealth(player.getMaxHealth());
        clearSpawnProtection(player);
        return player;
    }

    /** Freshly placed players get vanilla's three seconds of immunity, which would swallow every damage assertion. */
    private static void clearSpawnProtection(ServerPlayer player) {
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void dress(ServerPlayer player, ArmorSet set) {
        for (int i = 0; i < TYPES.size(); i++) player.setItemSlot(ARMOR_SLOTS[i], charged(set, TYPES.get(i)));
    }

    private static Zombie target(GameTestHelper h, double x, double z) {
        Zombie mob = EntityType.ZOMBIE.create(h.getLevel());
        Vec3 at = h.absoluteVec(new Vec3(x, 1, z));
        mob.moveTo(at.x, at.y, at.z, 0, 0);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.setHealth(100);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }

    /** Armor-bypassing, so the suit effects are measured in isolation from the armor formula. */
    private static void magicHit(ServerPlayer player, float amount) {
        player.invulnerableTime = 0;
        player.hurt(player.damageSources().magic(), amount);
    }

    private static void near(GameTestHelper h, float actual, float expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < 0.01F, message + ": expected " + expected + " got " + actual);
    }

    // ---- Energy and data ---------------------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void gearOnlyAcceptsEnergy(GameTestHelper h) {
        ItemStack chest = new ItemStack(ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR).get(ArmorItem.Type.CHESTPLATE).get());
        var energy = chest.getCapability(Capabilities.EnergyStorage.ITEM);
        h.assertTrue(energy != null, "FE capability missing");
        h.assertTrue(energy.receiveEnergy(500, true) == 500 && energy.getEnergyStored() == 0, "simulation changed energy");
        h.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, false) == Rules.COLLAPSAR_CAPACITY, "overfill");
        h.assertTrue(!energy.canExtract() && energy.extractEnergy(1000, false) == 0, "gear must not work as a battery for machines");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void stateSurvivesSaveAndLoad(GameTestHelper h) {
        ItemStack chest = charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE);
        chest.set(ArsenalData.LOAD.get(), 42);
        chest.set(ArsenalData.HORIZON_READY.get(), 98_765L);
        var restored = ItemStack.parse(h.getLevel().registryAccess(), chest.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(Fe.stored(restored) == Rules.COLLAPSAR_CAPACITY && ArsenalData.get(restored, ArsenalData.LOAD) == 42
                && ArsenalData.time(restored, ArsenalData.HORIZON_READY) == 98_765L, "state lost on save/load");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void accretionConsumesPelletsOnlyWhenTheyFit(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack chest = new ItemStack(ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR).get(ArmorItem.Type.CHESTPLATE).get());
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        ItemStack pellets = new ItemStack(Fe.MASS_PELLET.get(), 3);
        p.getInventory().add(pellets);
        h.assertTrue(Fe.accrete(p, chest) && Fe.stored(chest) == Rules.PELLET_FE && p.getInventory().countItem(Fe.MASS_PELLET.get()) == 2, "accretion");
        Fe.set(chest, Rules.COLLAPSAR_CAPACITY);
        h.assertTrue(!Fe.accrete(p, chest) && p.getInventory().countItem(Fe.MASS_PELLET.get()) == 2, "pellet burned with no room");
        ItemStack meissner = charged(ArmorSet.MEISSNER, ArmorItem.Type.CHESTPLATE);
        Fe.set(meissner, 0);
        h.assertTrue(!Fe.accrete(p, meissner), "only collapsar-class gear accretes");
        h.succeed();
    }

    // ---- Meissner --------------------------------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void meissnerExpelsProjectiles(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack chest = charged(ArmorSet.MEISSNER, ArmorItem.Type.CHESTPLATE);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        Arrow arrow = new Arrow(EntityType.ARROW, h.getLevel());
        Vec3 from = p.getBoundingBox().getCenter().add(0, 0, -2);
        arrow.moveTo(from.x, from.y, from.z);
        arrow.setDeltaMovement(0, 0, 1.2);
        h.getLevel().addFreshEntity(arrow);
        // Far-away GameTest chunks do not step arrows, so post the exact event vanilla's arrow tick posts on impact.
        h.assertTrue(net.neoforged.neoforge.event.EventHooks.onProjectileImpact(arrow, new net.minecraft.world.phys.EntityHitResult(p)), "impact was not cancelled");
        h.assertTrue(arrow.getOwner() == p, "arrow was not expelled");
        h.assertTrue(Fe.stored(chest) == Rules.MEISSNER_CAPACITY - Rules.DEFLECT_FE, "deflection cost");
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "arrow still hurt the wearer");
        h.assertTrue(arrow.getDeltaMovement().z < 0, "arrow kept travelling toward the wearer");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void persistentCurrentBalancesWithoutLoss(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.MEISSNER);
        Fe.set(p.getItemBySlot(EquipmentSlot.HEAD), 0);
        Fe.set(p.getItemBySlot(EquipmentSlot.FEET), 7);
        int before = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) before += Fe.stored(p.getItemBySlot(slot));
        SuitEvents.upkeep(p);
        int after = 0, lowest = Integer.MAX_VALUE, highest = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            int stored = Fe.stored(p.getItemBySlot(slot));
            after += stored;
            lowest = Math.min(lowest, stored);
            highest = Math.max(highest, stored);
        }
        h.assertTrue(after == before, "balancing changed total energy");
        h.assertTrue(highest - lowest < ARMOR_SLOTS.length, "pieces did not settle");
        h.succeed();
    }

    // ---- Degeneracy ------------------------------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void chandrasekharLimitCapsAHitAndCharges(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.DEGENERACY);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        magicHit(p, 18);
        near(h, p.getHealth(), 20 - Rules.CHANDRASEKHAR_LIMIT, "hit was not capped");
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == 10, "pressure did not record the excess");
        h.assertTrue(Fe.stored(chest) == Rules.DEGENERACY_CAPACITY - 10 * Rules.PRESSURE_FE_PER_HP, "excess was not paid for");
        magicHit(p, 4);
        near(h, p.getHealth(), 20 - Rules.CHANDRASEKHAR_LIMIT - 4, "small hits must pass untouched");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void collapseAtTheLimitLocksTheSuit(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.DEGENERACY);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Zombie nearby = target(h, 4.5, 2.5);
        chest.set(ArsenalData.LOAD.get(), Rules.PRESSURE_MAX - 1);
        magicHit(p, 10);
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == 0, "pressure not released");
        h.assertTrue(ArsenalData.time(chest, ArsenalData.LOCKED_UNTIL) == h.getLevel().getGameTime() + Rules.COLLAPSE_LOCKOUT, "no lockout");
        h.assertTrue(nearby.getHealth() < 100, "collapse shockwave missed");
        p.setHealth(p.getMaxHealth());
        magicHit(p, 15);
        near(h, p.getHealth(), 5, "a collapsed suit must not cap hits");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void ventRequiresMinimumPressure(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.DEGENERACY);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        chest.set(ArsenalData.LOAD.get(), Rules.VENT_MINIMUM - 1);
        SuitEvents.activate(p);
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == Rules.VENT_MINIMUM - 1, "vented below the minimum");
        chest.set(ArsenalData.LOAD.get(), Rules.VENT_MINIMUM);
        p.getPersistentData().remove("singulo_arsenal.last_ability");
        SuitEvents.activate(p);
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == 0, "vent did not release");
        h.succeed();
    }

    // ---- Collapsar ---------------------------------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void pastaPhaseDrivesAttributes(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        double base = p.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) (base * (1 + Rules.GNOCCHI_SPEED)), "gnocchi speed");
        chest.set(ArsenalData.LOAD.get(), 50);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) base, "spaghetti leaves speed alone");
        h.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= Rules.SPAGHETTI_KNOCKBACK, "spaghetti knockback");
        chest.set(ArsenalData.LOAD.get(), 90);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) (base * (1 + Rules.LASAGNA_SPEED)), "lasagna slows");
        magicHit(p, 10);
        near(h, p.getHealth(), 20 - 10 * Rules.LASAGNA_FACTOR, "lasagna mitigation");
        p.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) base, "modifiers must vanish with the greaves");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void eventHorizonCatchesLethalHitOnce(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Zombie nearby = target(h, 4.5, 2.5);
        magicHit(p, 100);
        h.assertTrue(p.isAlive(), "lethal hit was not caught");
        near(h, p.getHealth(), 20 * Rules.HORIZON_HEALTH_FRACTION, "health after horizon");
        h.assertTrue(Fe.stored(chest) == Rules.COLLAPSAR_CAPACITY - Rules.HORIZON_FE, "horizon cost");
        h.assertTrue(nearby.getHealth() < 100, "horizon wave missed");
        // During the horizon window nothing lands.
        float during = p.getHealth();
        magicHit(p, 100);
        near(h, p.getHealth(), during, "invulnerable window");
        // Past the window the cooldown holds, so a second lethal hit kills.
        chest.set(ArsenalData.HORIZON_UNTIL.get(), 0L);
        magicHit(p, 100);
        h.assertTrue(p.isDeadOrDying(), "horizon ignored its cooldown");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void jetBurstRequiresBootsAndBurnsBehind(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 6.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack boots = p.getItemBySlot(EquipmentSlot.FEET);
        Zombie behind = target(h, 2.5, 3.5);
        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        SuitEvents.activate(p);
        h.assertTrue(Fe.stored(chest) == Rules.COLLAPSAR_CAPACITY, "burst without boots");
        p.setItemSlot(EquipmentSlot.FEET, boots);
        p.getPersistentData().remove("singulo_arsenal.last_ability");
        chest.set(ArsenalData.LOAD.get(), 40);
        SuitEvents.activate(p);
        h.assertTrue(Fe.stored(chest) == Rules.COLLAPSAR_CAPACITY - Rules.JET_BURST_FE, "burst cost");
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == 40 - Rules.JET_BURST_LOAD, "burst load");
        h.assertTrue(behind.getHealth() < 100, "rear jet missed");
        h.assertTrue(p.getDeltaMovement().length() > 1.5, "no thrust");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void noHairClearsHarmfulEffects(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack helmet = p.getItemBySlot(EquipmentSlot.HEAD);
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 200));
        p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, 200));
        SuitEvents.upkeep(p);
        h.assertTrue(!p.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "poison survived");
        h.assertTrue(p.hasEffect(net.minecraft.world.effect.MobEffects.DAMAGE_BOOST), "beneficial effect removed");
        h.assertTrue(Fe.stored(helmet) == Rules.COLLAPSAR_CAPACITY - Rules.CLEANSE_FE, "cleanse cost");
        h.succeed();
    }

    // ---- Weapons -----------------------------------------------------------------------------------------------------

    private static ItemStack hold(ServerPlayer p, ItemStack stack) {
        Fe.set(stack, Fe.capacity(stack));
        p.setItemSlot(EquipmentSlot.MAINHAND, stack);
        return p.getMainHandItem();
    }

    @GameTest(template = ARENA)
    public static void quenchOnFifthHit(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack saber = hold(p, new ItemStack(ArsenalItems.QUENCH_SABER.get()));
        Zombie first = target(h, 2.5, 4.5);
        Zombie second = target(h, 3.5, 4.5);
        for (int i = 0; i < 4; i++) saber.getItem().hurtEnemy(saber, first, p);
        h.assertTrue(second.getHealth() == 100 && ArsenalData.get(saber, ArsenalData.CURRENT) == 4, "quenched early");
        saber.getItem().hurtEnemy(saber, first, p);
        h.assertTrue(second.getHealth() < 100, "quench missed bystander");
        h.assertTrue(ArsenalData.get(saber, ArsenalData.CURRENT) == 0, "current not reset");
        h.assertTrue(Fe.stored(saber) == Rules.QUENCH_CAPACITY - Rules.QUENCH_FE, "quench cost");
        h.assertTrue(p.getCooldowns().isOnCooldown(saber.getItem()), "no cooldown");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void casimirIgnoresArmorWhenArmed(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack blade = hold(p, new ItemStack(ArsenalItems.CASIMIR_BLADE.get()));
        Zombie armored = target(h, 2.5, 4.5);
        armored.getAttribute(Attributes.ARMOR).setBaseValue(20);
        Zombie reference = target(h, 3.5, 4.5);
        reference.getAttribute(Attributes.ARMOR).setBaseValue(20);
        reference.hurt(h.getLevel().damageSources().mobAttack(reference), 10);
        float vanillaLoss = 100 - reference.getHealth();
        armored.hurt(p.damageSources().playerAttack(p), 10);
        float passiveLoss = 100 - armored.getHealth();
        h.assertTrue(passiveLoss > vanillaLoss, "blade did not reduce armor: " + passiveLoss + " vs " + vanillaLoss);
        h.assertTrue(passiveLoss < 10, "unarmed blade must not ignore armor outright");
        blade.set(ArsenalData.ARMED_UNTIL.get(), h.getLevel().getGameTime() + 100);
        armored.setHealth(100);
        armored.invulnerableTime = 0;
        armored.hurt(p.damageSources().playerAttack(p), 10);
        near(h, 100 - armored.getHealth(), 10 * Rules.CASIMIR_ARMED_BONUS, "armed hit");
        h.assertTrue(ArsenalData.time(blade, ArsenalData.ARMED_UNTIL) == 0, "arming must be single use");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void tidalStressStacksAndShears(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack reaver = hold(p, new ItemStack(ArsenalItems.TIDAL_REAVER.get()));
        Zombie victim = target(h, 2.5, 5.5);
        for (int i = 0; i < Rules.STRESS_MAX + 2; i++) reaver.getItem().hurtEnemy(reaver, victim, p);
        h.assertTrue(Weapons.stress(victim) == Rules.STRESS_MAX, "stress should clamp at the maximum");
        victim.setHealth(100);
        victim.invulnerableTime = 0;
        victim.hurt(h.getLevel().damageSources().generic(), 10);
        near(h, 100 - victim.getHealth(), 10 * Rules.stressMultiplier(Rules.STRESS_MAX), "stress multiplier");
        victim.setHealth(100);
        victim.invulnerableTime = 0;
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, victim.getEyePosition());
        ((io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem) reaver.getItem()).shear(p, h.getLevel());
        h.assertTrue(Weapons.stress(victim) == 0, "shear must spend stress");
        h.assertTrue(victim.getHealth() <= 100 - Rules.shearDamage(Rules.STRESS_MAX), "shear damage scales with stress");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void lanceFiresForwardOnlyAndStopsAtWalls(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        p.setYRot(0);
        p.setXRot(0);
        Zombie front = target(h, 2.5, 4.2);
        Zombie rear = target(h, 2.5, 1);
        JetLanceItem.pulse(p, h.getLevel());
        near(h, 100 - front.getHealth(), Rules.LANCE_DAMAGE, "jet");
        h.assertTrue(rear.getHealth() == 100, "the rear pole is sealed: nothing behind the wielder may be hurt");
        JetLanceItem.pulse(p, h.getLevel());
        near(h, 100 - front.getHealth(), Rules.LANCE_DAMAGE * 2, "pulses must not be blocked by hurt cooldown");
        h.setBlock(2, 3, 3, Blocks.IRON_BLOCK);
        h.setBlock(2, 2, 3, Blocks.IRON_BLOCK);
        float before = front.getHealth();
        JetLanceItem.pulse(p, h.getLevel());
        h.assertTrue(front.getHealth() == before, "jet passed through a wall");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void lanceNeverHurtsTheWielder(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 4.5);
        hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        JetLanceItem.pulse(p, h.getLevel());
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "self-damage");
        h.succeed();
    }

    // ---- Module Integrator -------------------------------------------------------------------------------------------

    private static ItemStack module(ModuleType type) {
        return new ItemStack(ModuleContent.MODULES.get(type).get());
    }

    private static IntegratorBlockEntity integrator(GameTestHelper h) {
        h.setBlock(2, 2, 2, ModuleContent.INTEGRATOR.get());
        IntegratorBlockEntity entity = h.getBlockEntity(new net.minecraft.core.BlockPos(2, 2, 2));
        entity.energy().receiveEnergy(Rules.INTEGRATOR_CAPACITY, false);
        return entity;
    }

    private static void run(IntegratorBlockEntity entity) {
        entity.start();
        for (int i = 0; i < Rules.INTEGRATE_TICKS; i++) entity.serverTick();
    }

    @GameTest(template = ARENA)
    public static void integrationInstallsAndPaysForModules(GameTestHelper h) {
        IntegratorBlockEntity entity = integrator(h);
        var items = entity.items();
        items.setStackInSlot(IntegratorBlockEntity.GEAR, new ItemStack(ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR).get(ArmorItem.Type.CHESTPLATE).get()));
        items.setStackInSlot(IntegratorBlockEntity.FIRST_SOCKET, module(ModuleType.CAPACITOR));
        items.setStackInSlot(IntegratorBlockEntity.FIRST_SOCKET + 1, module(ModuleType.BIT_DRONE));
        run(entity);
        ItemStack gear = items.getStackInSlot(IntegratorBlockEntity.GEAR);
        h.assertTrue(Modules.count(gear, ModuleType.CAPACITOR) == 1 && Modules.count(gear, ModuleType.BIT_DRONE) == 1, "modules not installed");
        h.assertTrue(items.getStackInSlot(IntegratorBlockEntity.FIRST_SOCKET).isEmpty(), "module duplicated into the gear");
        h.assertTrue(Fe.capacity(gear) == Rules.capacityWith(Rules.COLLAPSAR_CAPACITY, 1), "capacitor did not raise capacity");
        h.assertTrue(entity.energy().getEnergyStored() == Rules.INTEGRATOR_CAPACITY - Rules.INTEGRATE_TICKS * Rules.INTEGRATE_FE_PER_TICK, "wrong energy cost");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void placingGearUnpacksItsModulesExactlyOnce(GameTestHelper h) {
        IntegratorBlockEntity entity = integrator(h);
        var items = entity.items();
        ItemStack gear = new ItemStack(ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR).get(ArmorItem.Type.CHESTPLATE).get());
        Modules.install(gear, java.util.List.of(module(ModuleType.PLATING), module(ModuleType.CAPACITOR)));
        items.setStackInSlot(IntegratorBlockEntity.GEAR, gear);
        ItemStack held = items.getStackInSlot(IntegratorBlockEntity.GEAR);
        h.assertTrue(Modules.installed(held).isEmpty(), "gear kept its modules");
        int found = 0;
        for (int i = IntegratorBlockEntity.FIRST_SOCKET; i < IntegratorBlockEntity.SLOTS; i++) if (!items.getStackInSlot(i).isEmpty()) found++;
        h.assertTrue(found == 2, "modules lost or duplicated: " + found);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void integrationRefusesWrongOrExcessModules(GameTestHelper h) {
        IntegratorBlockEntity entity = integrator(h);
        var items = entity.items();
        items.setStackInSlot(IntegratorBlockEntity.GEAR, new ItemStack(ArsenalItems.QUENCH_SABER.get()));
        items.setStackInSlot(IntegratorBlockEntity.FIRST_SOCKET, module(ModuleType.BIT_DRONE));
        run(entity);
        h.assertTrue(Modules.installed(items.getStackInSlot(IntegratorBlockEntity.GEAR)).isEmpty(), "a sword took a bit drone");
        h.assertTrue(!items.getStackInSlot(IntegratorBlockEntity.FIRST_SOCKET).isEmpty(), "refused module was consumed");
        items.setStackInSlot(IntegratorBlockEntity.FIRST_SOCKET, module(ModuleType.COOLANT));
        items.setStackInSlot(IntegratorBlockEntity.FIRST_SOCKET + 1, module(ModuleType.COOLANT));
        run(entity);
        h.assertTrue(Modules.installed(items.getStackInSlot(IntegratorBlockEntity.GEAR)).isEmpty(), "stage 2 gear took two modules");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void bitsPayEnergyAndHitHostiles(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack chest = charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE);
        Modules.install(chest, java.util.List.of(module(ModuleType.BIT_DRONE)));
        Fe.set(chest, Rules.BIT_FE * 10);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        Zombie mob = target(h, 2.5, 4.5);
        SuitEvents.fireBits(p);
        h.assertTrue(mob.getHealth() == 100 - Rules.BIT_DAMAGE * Rules.BITS_PER_MODULE, "bits did not land: " + mob.getHealth());
        h.assertTrue(Fe.stored(chest) == Rules.BIT_FE * (10 - Rules.BITS_PER_MODULE), "bit cost");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void fluxPinConsumesTheLastPartialCharge(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack boots = charged(ArmorSet.MEISSNER, ArmorItem.Type.BOOTS);
        Fe.set(boots, 1);
        p.setItemSlot(EquipmentSlot.FEET, boots);
        p.setOnGround(false);
        p.setShiftKeyDown(true);
        p.tickCount = 10;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(p));
        h.assertTrue(Fe.stored(boots) == 0, "partial charge allowed indefinite free pinning");
        h.assertTrue(!SuitEvents.pinned(p), "empty boots still pin");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void balancingDifferentCapacitiesConservesEnergy(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack head = charged(ArmorSet.MEISSNER, ArmorItem.Type.HELMET);
        Modules.install(head, List.of(module(ModuleType.CAPACITOR)));
        Fe.set(head, Fe.capacity(head));
        ItemStack legs = charged(ArmorSet.MEISSNER, ArmorItem.Type.LEGGINGS);
        p.setItemSlot(EquipmentSlot.HEAD, head);
        p.setItemSlot(EquipmentSlot.LEGS, legs);
        int before = Fe.stored(head) + Fe.stored(legs);
        SuitEvents.upkeep(p);
        h.assertTrue(Fe.stored(head) + Fe.stored(legs) == before, "balancing discarded charge at a smaller tank's limit");
        Fe.set(legs, 0);
        int available = Fe.stored(head);
        SuitEvents.upkeep(p);
        h.assertTrue(Fe.stored(head) == available / 2 && Fe.stored(legs) == available / 2, "unequal tanks did not share available energy");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void integratorChargingMarksTheChunkForSaving(GameTestHelper h) {
        IntegratorBlockEntity entity = integrator(h);
        var chunk = h.getLevel().getChunkAt(entity.getBlockPos());
        entity.energy().deserializeNBT(h.getLevel().registryAccess(), net.minecraft.nbt.IntTag.valueOf(0));
        chunk.setUnsaved(false);
        entity.energy().receiveEnergy(1000, true);
        h.assertTrue(!chunk.isUnsaved(), "simulated charging dirtied the chunk");
        entity.energy().receiveEnergy(1000, false);
        h.assertTrue(chunk.isUnsaved(), "received FE may disappear after a chunk reload");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void holdingArmedBladeDoesNotModifyDroneDamage(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack blade = hold(p, new ItemStack(ArsenalItems.CASIMIR_BLADE.get()));
        Modules.install(blade, List.of(module(ModuleType.AMPLIFIER)));
        long armedUntil = h.getLevel().getGameTime() + 100;
        blade.set(ArsenalData.ARMED_UNTIL.get(), armedUntil);
        ItemStack chest = charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE);
        Modules.install(chest, List.of(module(ModuleType.BIT_DRONE)));
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        Zombie mob = target(h, 2.5, 4.5);
        SuitEvents.fireBits(p);
        near(h, 100 - mob.getHealth(), Rules.BIT_DAMAGE * Rules.BITS_PER_MODULE, "weapon bonuses leaked into drones");
        h.assertTrue(ArsenalData.time(blade, ArsenalData.ARMED_UNTIL) == armedUntil, "a drone consumed the charged blade strike");
        mob.invulnerableTime = 0;
        mob.setHealth(100);
        mob.hurt(p.damageSources().playerAttack(p), 10);
        near(h, 100 - mob.getHealth(), 10 * Rules.amplified(1) * Rules.CASIMIR_ARMED_BONUS, "actual blade strike lost its bonuses");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void cannonPulseUsesItsAmplifier(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        Modules.install(cannon, List.of(module(ModuleType.AMPLIFIER)));
        Zombie mob = target(h, 2.5, 4.5);
        JetLanceItem.pulse(p, h.getLevel());
        near(h, 100 - mob.getHealth(), Rules.LANCE_DAMAGE * Rules.amplified(1), "cannon amplification");
        h.succeed();
    }

    // ---- Recipes against the real base mod ---------------------------------------------------------------------------

    @GameTest(template = ARENA)
    public static void everyRecipeLoadsAgainstRealSingulo(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        int found = 0;
        for (var entry : ArsenalItems.ITEMS.getEntries()) {
            ResourceLocation id = entry.getId();
            h.assertTrue(recipes.byKey(id).isPresent(), "recipe failed to load: " + id);
            found++;
        }
        h.assertTrue(found == 28, "unexpected item count " + found);
        var lamina = recipes.byKey(SinguloArsenal.id("collapsar_lamina")).orElseThrow().value();
        h.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.getKey(lamina.getSerializer()).toString().equals("singulo:machine"), "lamina must be a Singulo machine recipe");
        h.succeed();
    }
}
