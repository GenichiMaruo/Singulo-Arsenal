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
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
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
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Integration tests that run with the real Singulo JAR. Excluded from the distributed JAR. */
@GameTestHolder(SinguloArsenal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArsenalGameTests {
    private static final String ARENA = "empty";
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static final List<ArmorItem.Type> TYPES = ArsenalItems.PIECES;

    private ArsenalGameTests() {}

    /** Legacy GameTest grids put fixtures inside the drone acquisition radius. Run distinct batches and remove actors on completion. */
    private static void cleanup(GameTestHelper helper, Runnable action) {
        try {
            var field = GameTestHelper.class.getDeclaredField("testInfo");
            field.setAccessible(true);
            var info = (net.minecraft.gametest.framework.GameTestInfo) field.get(helper);
            info.addListener(new net.minecraft.gametest.framework.GameTestListener() {
                public void testStructureLoaded(net.minecraft.gametest.framework.GameTestInfo test) {}
                public void testPassed(net.minecraft.gametest.framework.GameTestInfo test) { action.run(); }
                public void testFailed(net.minecraft.gametest.framework.GameTestInfo test) { action.run(); }
            });
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }

    private static ItemStack charged(ArmorSet set, ArmorItem.Type type) {
        ItemStack stack = new ItemStack(ArsenalItems.SUITS.get(set).get(type).get());
        Fe.set(stack, Fe.capacity(stack));
        return stack;
    }

    private static ServerPlayer player(GameTestHelper h, double x, double z) {
        var server = h.getLevel().getServer();
        ServerPlayer player = new ServerPlayer(server, h.getLevel(), new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ArsenalTest"));
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player);
        player.setGameMode(GameType.SURVIVAL);
        Vec3 at = h.absoluteVec(new Vec3(x, 1, z));
        h.assertTrue(x >= 0 && x < 5 && z >= 0 && z < 5, "player must stay inside the loaded test structure");
        player.moveTo(at.x, at.y, at.z, 0, 0);
        player.setHealth(player.getMaxHealth());
        clearSpawnProtection(player);
        cleanup(h, () -> { server.getPlayerList().remove(player); player.discard(); });
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
        h.assertTrue(x >= 0 && x < 5 && z >= 0 && z < 5, "target must stay inside the loaded test structure");
        mob.moveTo(at.x, at.y, at.z, 0, 0);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.setHealth(100);
        h.assertTrue(h.getLevel().addFreshEntity(mob), "target could not be added to the test level");
        cleanup(h, mob::discard);
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

    @GameTest(template = ARENA, batch = "gearOnlyAcceptsEnergy")
    public static void gearOnlyAcceptsEnergy(GameTestHelper h) {
        ItemStack chest = new ItemStack(ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR).get(ArmorItem.Type.CHESTPLATE).get());
        var energy = chest.getCapability(ForgeCapabilities.ENERGY).orElseThrow(() -> new AssertionError("missing energy capability"));
        h.assertTrue(energy != null, "FE capability missing");
        h.assertTrue(energy.receiveEnergy(500, true) == 500 && energy.getEnergyStored() == 0, "simulation changed energy");
        h.assertTrue(energy.receiveEnergy(Integer.MAX_VALUE, false) == Rules.COLLAPSAR_CAPACITY, "overfill");
        h.assertTrue(!energy.canExtract() && energy.extractEnergy(1000, false) == 0, "gear must not work as a battery for machines");
        h.succeed();
    }

    /** Exercises the real 0.18 SMES charging slots without a production dependency on its classes. */
    @GameTest(template = ARENA, batch = "singuloSmesCharging")
    public static void singuloSmesChargesArsenalAndPreservesModules(GameTestHelper h) {
        try {
            var at = new net.minecraft.core.BlockPos(2, 1, 2);
            var block = BuiltInRegistries.BLOCK.get(new ResourceLocation("singulo", "smes_cell"));
            h.assertTrue(block != Blocks.AIR, "Singulo SMES missing");
            h.setBlock(at, block);
            var be = h.getLevel().getBlockEntity(h.absolutePos(at));
            Class<?> smes = Class.forName("io.github.genichimaruo.singulo.machine.SmesCellBlockEntity");
            h.assertTrue(smes.isInstance(be), "Unexpected SMES block entity");
            var energy = (net.minecraftforge.energy.IEnergyStorage) smes.getMethod("energy").invoke(be);
            var slots = (net.minecraftforge.items.IItemHandlerModifiable) smes.getMethod("chargeSlots").invoke(be);
            var tick = smes.getMethod("serverTick", net.minecraft.world.level.Level.class,
                    net.minecraft.core.BlockPos.class, net.minecraft.world.level.block.state.BlockState.class, smes);

            ItemStack gear = charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE);
            Modules.install(gear, List.of(module(ModuleType.CAPACITOR)));
            int capacity = Fe.capacity(gear);
            Fe.set(gear, 0);
            h.assertTrue(slots.isItemValid(0, gear) && slots.insertItem(0, gear, false).isEmpty(),
                    "Arsenal gear rejected by SMES charge input");
            h.assertTrue(energy.receiveEnergy(5_000, false) == 5_000, "SMES fixture could not receive energy");
            tick.invoke(null, h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            h.assertTrue(Fe.stored(slots.getStackInSlot(0)) == 5_000 && energy.getEnergyStored() == 0,
                    "SMES must transfer exactly the available FE to Arsenal gear");
            h.assertTrue(slots.getStackInSlot(1).isEmpty(), "Partially charged gear moved to output");

            Fe.set(slots.getStackInSlot(0), capacity - 700);
            h.assertTrue(energy.receiveEnergy(5_000, false) == 5_000, "SMES fixture could not refill");
            tick.invoke(null, h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            h.assertTrue(Fe.stored(slots.getStackInSlot(0)) == capacity && energy.getEnergyStored() == 4_300,
                    "SMES must pay only the FE accepted before the capacity limit");
            ItemStack blocker = new ItemStack(net.minecraft.world.item.Items.STONE);
            slots.setStackInSlot(1, blocker);
            tick.invoke(null, h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            h.assertTrue(!slots.getStackInSlot(0).isEmpty() && slots.getStackInSlot(1) == blocker
                    && energy.getEnergyStored() == 4_300, "Blocked output must not lose gear or consume energy");

            slots.setStackInSlot(1, ItemStack.EMPTY);
            tick.invoke(null, h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
            ItemStack out = slots.getStackInSlot(1);
            h.assertTrue(slots.getStackInSlot(0).isEmpty() && out.is(gear.getItem()), "Full Arsenal gear did not move to output");
            h.assertTrue(Fe.stored(out) == capacity && Fe.capacity(out) == capacity, "Output charge or capacity changed");
            h.assertTrue(Modules.installed(out).size() == 1 && Modules.count(out, ModuleType.CAPACITOR) == 1,
                    "SMES charging lost the installed capacitor module");
            h.assertTrue(energy.getEnergyStored() == 4_300, "Output transfer consumed more energy");
            h.succeed();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Singulo 0.18 charging API unavailable", e);
        }
    }

    @GameTest(template = ARENA, batch = "stateSurvivesSaveAndLoad")
    public static void stateSurvivesSaveAndLoad(GameTestHelper h) {
        ItemStack chest = charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE);
        ArsenalData.set(chest, ArsenalData.LOAD, 42);
        ArsenalData.set(chest, ArsenalData.HORIZON_READY, 98_765L);
        var restored = ItemStack.of(chest.save(new net.minecraft.nbt.CompoundTag()));
        h.assertTrue(Fe.stored(restored) == Rules.COLLAPSAR_CAPACITY && ArsenalData.get(restored, ArsenalData.LOAD) == 42
                && ArsenalData.time(restored, ArsenalData.HORIZON_READY) == 98_765L, "state lost on save/load");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "accretionConsumesPelletsOnlyWhenTheyFit")
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

    @GameTest(template = ARENA, batch = "meissnerExpelsProjectiles")
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
        h.assertTrue(net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(arrow, new net.minecraft.world.phys.EntityHitResult(p)), "impact was not cancelled");
        h.assertTrue(arrow.getOwner() == p, "arrow was not expelled");
        h.assertTrue(Fe.stored(chest) == Rules.MEISSNER_CAPACITY - Rules.DEFLECT_FE, "deflection cost");
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "arrow still hurt the wearer");
        h.assertTrue(arrow.getDeltaMovement().z < 0, "arrow kept travelling toward the wearer");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "persistentCurrentBalancesWithoutLoss")
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

    @GameTest(template = ARENA, batch = "chandrasekharLimitCapsAHitAndCharges")
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

    @GameTest(template = ARENA, batch = "collapseAtTheLimitLocksTheSuit")
    public static void collapseAtTheLimitLocksTheSuit(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.DEGENERACY);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Zombie nearby = target(h, 4.5, 2.5);
        ArsenalData.set(chest, ArsenalData.LOAD, Rules.PRESSURE_MAX - 1);
        magicHit(p, 10);
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == 0, "pressure not released");
        h.assertTrue(ArsenalData.time(chest, ArsenalData.LOCKED_UNTIL) == h.getLevel().getGameTime() + Rules.COLLAPSE_LOCKOUT, "no lockout");
        h.assertTrue(nearby.getHealth() < 100, "collapse shockwave missed");
        p.setHealth(p.getMaxHealth());
        magicHit(p, 15);
        near(h, p.getHealth(), 5, "a collapsed suit must not cap hits");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "ventRequiresMinimumPressure")
    public static void ventRequiresMinimumPressure(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.DEGENERACY);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ArsenalData.set(chest, ArsenalData.LOAD, Rules.VENT_MINIMUM - 1);
        SuitEvents.activate(p);
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == Rules.VENT_MINIMUM - 1, "vented below the minimum");
        ArsenalData.set(chest, ArsenalData.LOAD, Rules.VENT_MINIMUM);
        p.getPersistentData().remove("singulo_arsenal.last_ability");
        SuitEvents.activate(p);
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == 0, "vent did not release");
        h.succeed();
    }

    // ---- Collapsar ---------------------------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "pastaPhaseDrivesAttributes")
    public static void pastaPhaseDrivesAttributes(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        double base = p.getAttributeBaseValue(Attributes.MOVEMENT_SPEED);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) (base * (1 + Rules.GNOCCHI_SPEED)), "gnocchi speed");
        ArsenalData.set(chest, ArsenalData.LOAD, 50);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) base, "spaghetti leaves speed alone");
        h.assertTrue(p.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) >= Rules.SPAGHETTI_KNOCKBACK, "spaghetti knockback");
        ArsenalData.set(chest, ArsenalData.LOAD, 90);
        // Not a full set, so the suit's energy shield stays out of this measurement.
        p.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) (base * (1 + Rules.LASAGNA_SPEED)), "lasagna slows");
        magicHit(p, 10);
        near(h, p.getHealth(), 20 - 10 * Rules.LASAGNA_FACTOR, "lasagna mitigation");
        p.setItemSlot(EquipmentSlot.LEGS, ItemStack.EMPTY);
        SuitEvents.applyAttributes(p);
        near(h, (float) p.getAttributeValue(Attributes.MOVEMENT_SPEED), (float) base, "modifiers must vanish with the greaves");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "eventHorizonCatchesLethalHitOnce")
    public static void eventHorizonCatchesLethalHitOnce(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Zombie nearby = target(h, 4.5, 2.5);
        // Freshly forced test chunks may not expose their entity sections until the next
        // server update. Fire the one-shot wave only after the real spatial query sees its target.
        h.startSequence().thenWaitUntil(() -> h.assertTrue(
                io.github.genichimaruo.singuloarsenal.core.ArsenalDamage.around(
                        p, p.getBoundingBox().getCenter(), Rules.HORIZON_RADIUS).contains(nearby),
                "horizon target not yet visible in the loaded entity section"))
                .thenExecute(() -> {
                    // Only the horizon's reserve left, which the energy shield must not touch.
                    for (EquipmentSlot slot : ARMOR_SLOTS) Fe.set(p.getItemBySlot(slot), slot == EquipmentSlot.CHEST ? Rules.HORIZON_FE : 0);
                    magicHit(p, 100);
                    h.assertTrue(p.isAlive(), "lethal hit was not caught");
                    near(h, p.getHealth(), 20 * Rules.HORIZON_HEALTH_FRACTION, "health after horizon");
                    h.assertTrue(Fe.stored(chest) == 0, "horizon cost");
                    h.assertTrue(nearby.getHealth() < 100, "horizon wave missed");
                    // During the horizon window nothing lands.
                    float during = p.getHealth();
                    magicHit(p, 100);
                    near(h, p.getHealth(), during, "invulnerable window");
                    // Past the window the cooldown holds, so a second lethal hit kills.
                    ArsenalData.set(chest, ArsenalData.HORIZON_UNTIL, 0L);
                    magicHit(p, 100);
                    h.assertTrue(p.isDeadOrDying(), "horizon ignored its cooldown");
                }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = "collapsarSuitPaysForDamageInEnergy")
    public static void collapsarSuitPaysForDamageInEnergy(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack head = p.getItemBySlot(EquipmentSlot.HEAD);
        magicHit(p, 10);
        near(h, p.getHealth(), 20, "the suit should have paid for the hit");
        h.assertTrue(Fe.stored(head) == Rules.COLLAPSAR_CAPACITY - 10 * Rules.SHIELD_FE_PER_HP, "the shield's price: " + Fe.stored(head));
        h.assertTrue(ArsenalData.get(p.getItemBySlot(EquipmentSlot.CHEST), ArsenalData.LOAD) > 0, "a paid hit still strains the lining");
        // Bare of energy, with the horizon spent, the hit lands.
        for (EquipmentSlot slot : ARMOR_SLOTS) Fe.set(p.getItemBySlot(slot), 0);
        magicHit(p, 5);
        near(h, p.getHealth(), 15, "an empty suit must not shield");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "metricDriveFliesOnEnergyAlone")
    public static void metricDriveFliesOnEnergyAlone(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        var flight = p.getAttribute(SuitEvents.legacyAttribute("creative_flight"));
        ItemStack chest = charged(ArmorSet.MEISSNER, ArmorItem.Type.CHESTPLATE);
        Modules.install(chest, List.of(module(ModuleType.METRIC_DRIVE)));
        Fe.set(chest, Rules.METRIC_FE_PER_10_TICKS * 2);
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        p.tickCount = 10;
        SuitEvents.metricDrive(p);
        h.assertTrue(flight.getValue() > 0, "no flight from the module");
        h.assertTrue(Fe.stored(chest) == Rules.METRIC_FE_PER_10_TICKS * 2, "paid while not in the air");
        p.getAbilities().flying = true;
        SuitEvents.metricDrive(p);
        h.assertTrue(Fe.stored(chest) == Rules.METRIC_FE_PER_10_TICKS, "flight is not free");
        Fe.set(chest, 0);
        SuitEvents.metricDrive(p);
        h.assertTrue(flight.getValue() == 0, "flying with an empty tank");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "jetBurstRequiresBootsAndBurnsBehind")
    public static void jetBurstRequiresBootsAndBurnsBehind(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 4.5);
        dress(p, ArmorSet.COLLAPSAR);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        ItemStack boots = p.getItemBySlot(EquipmentSlot.FEET);
        Zombie behind = target(h, 2.5, 3.5);
        p.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
        SuitEvents.activate(p);
        h.assertTrue(Fe.stored(chest) == Rules.COLLAPSAR_CAPACITY, "burst without boots");
        p.setItemSlot(EquipmentSlot.FEET, boots);
        p.getPersistentData().remove("singulo_arsenal.last_ability");
        ArsenalData.set(chest, ArsenalData.LOAD, 40);
        SuitEvents.activate(p);
        h.assertTrue(Fe.stored(chest) == Rules.COLLAPSAR_CAPACITY - Rules.JET_BURST_FE, "burst cost");
        h.assertTrue(ArsenalData.get(chest, ArsenalData.LOAD) == 40 - Rules.JET_BURST_LOAD, "burst load");
        h.assertTrue(behind.getHealth() < 100, "rear jet missed");
        h.assertTrue(p.getDeltaMovement().length() > 1.5, "no thrust");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "noHairClearsHarmfulEffects")
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

    @GameTest(template = ARENA, batch = "quenchOnFifthHit")
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

    @GameTest(template = ARENA, batch = "casimirIgnoresArmorWhenArmed")
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
        ArsenalData.set(blade, ArsenalData.ARMED_UNTIL, h.getLevel().getGameTime() + 100);
        armored.setHealth(100);
        armored.invulnerableTime = 0;
        armored.hurt(p.damageSources().playerAttack(p), 10);
        near(h, 100 - armored.getHealth(), 10 * Rules.CASIMIR_ARMED_BONUS, "armed hit");
        h.assertTrue(ArsenalData.time(blade, ArsenalData.ARMED_UNTIL) == 0, "arming must be single use");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "tidalStressStacksAndShears")
    public static void tidalStressStacksAndShears(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack reaver = hold(p, new ItemStack(ArsenalItems.TIDAL_REAVER.get()));
        Zombie victim = target(h, 2.5, 4.5);
        for (int i = 0; i < Rules.STRESS_MAX + 2; i++) reaver.getItem().hurtEnemy(reaver, victim, p);
        h.assertTrue(Weapons.stress(victim) == Rules.STRESS_MAX, "stress should clamp at the maximum");
        victim.setHealth(100);
        victim.invulnerableTime = 0;
        victim.hurt(h.getLevel().damageSources().generic(), 10);
        near(h, 100 - victim.getHealth(), 10 * Rules.stressMultiplier(Rules.STRESS_MAX), "stress multiplier");
        victim.setHealth(100);
        victim.invulnerableTime = 0;
        p.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, victim.getEyePosition());
        Vec3 focus = io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem.focus(p, 1);
        h.assertTrue(io.github.genichimaruo.singuloarsenal.core.ArsenalDamage.around(p, focus, Rules.REAVER_RANGE).contains(victim),
                "shear target missing from spatial query: focus=" + focus + " target=" + victim.position() + " health=" + victim.getHealth());
        ((io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem) reaver.getItem()).shear(p, h.getLevel());
        h.assertTrue(Weapons.stress(victim) == 0, "shear must spend stress: health=" + victim.getHealth() + " alive=" + victim.isAlive());
        h.assertTrue(victim.getHealth() <= 100 - Rules.shearDamage(Rules.STRESS_MAX), "shear damage scales with stress");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "lanceFiresForwardOnlyAndStopsAtWalls")
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

    @GameTest(template = ARENA, batch = "cannonHitsTargetsContainingTheRayOrigin")
    public static void cannonHitsTargetsContainingTheRayOrigin(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        Zombie mob = target(h, 2.5, 3.0);
        h.assertTrue(mob.getBoundingBox().inflate(Rules.LANCE_HIT_WIDTH).contains(p.getEyePosition()), "target must contain the ray origin");
        JetLanceItem.pulse(p, h.getLevel());
        near(h, 100 - mob.getHealth(), Rules.LANCE_DAMAGE, "point-blank cannon shot missed");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "lanceNeverHurtsTheWielder")
    public static void lanceNeverHurtsTheWielder(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 4.5);
        hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        JetLanceItem.pulse(p, h.getLevel());
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "self-damage");
        h.succeed();
    }

    /** Skips the spool-up: the shot goes off on this call. */
    private static void ignite(GameTestHelper h, ServerPlayer p, ItemStack cannon) {
        h.assertTrue(JetLanceItem.tryFire(p), "the shot was refused");
        ArsenalData.set(cannon, ArsenalData.CHARGE_UNTIL, h.getLevel().getGameTime());
        JetLanceItem.tickShot(cannon, p, h.getLevel());
    }

    /** Runs one whole shot on a level whose clock the test does not control. */
    private static void completeShot(GameTestHelper h, ServerPlayer p, ItemStack cannon) {
        ArsenalData.set(cannon, ArsenalData.JET_UNTIL, h.getLevel().getGameTime() - Rules.LANCE_SHOT_GAP);
        ignite(h, p, cannon);
        ArsenalData.set(cannon, ArsenalData.JET_UNTIL, h.getLevel().getGameTime() + 1);
        JetLanceItem.tickShot(cannon, p, h.getLevel());
    }

    @GameTest(template = ARENA, batch = "cannonFiresAMagazineThenReloads")
    public static void cannonFiresAMagazineThenReloads(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        h.assertTrue(JetLanceItem.tryFire(p), "first shot");
        h.assertTrue(JetLanceItem.jetAge(cannon, h.getLevel().getGameTime(), 0) < 0, "the jet must wait for the spool-up");
        h.assertTrue(JetLanceItem.chargeProgress(cannon, h.getLevel().getGameTime(), 0) == 0, "the spool-up must start");
        h.assertTrue(!JetLanceItem.tryFire(p), "started a second charge over the first");
        h.assertTrue(Fe.stored(cannon) == Rules.LANCE_CAPACITY, "paid before the shot went off");
        ArsenalData.set(cannon, ArsenalData.CHARGE_UNTIL, h.getLevel().getGameTime());
        JetLanceItem.tickShot(cannon, p, h.getLevel());
        h.assertTrue(JetLanceItem.jetAge(cannon, h.getLevel().getGameTime(), 0) == 0, "the jet must be lit at the end of the spool-up");
        h.assertTrue(Fe.stored(cannon) == Rules.LANCE_CAPACITY - Rules.LANCE_SHOT_FE, "shot cost");
        ArsenalData.remove(cannon, ArsenalData.SHOTS);
        for (int i = 0; i < Rules.LANCE_MAGAZINE; i++) {
            h.assertTrue(JetLanceItem.shotsLeft(cannon) == Rules.LANCE_MAGAZINE - i, "magazine count");
            completeShot(h, p, cannon);
        }
        h.assertTrue(Weapons.reloadProgress(cannon, h.getLevel().getGameTime(), 0) >= 0, "an empty magazine must reload");
        h.assertTrue(JetLanceItem.shotsLeft(cannon) == Rules.LANCE_MAGAZINE, "the magazine was not refilled");
        ArsenalData.set(cannon, ArsenalData.JET_UNTIL, 0L);
        h.assertTrue(!JetLanceItem.tryFire(p), "fired during the reload");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "switchingAwayCannotSkipAnEmptyMagazinesReload")
    public static void switchingAwayCannotSkipAnEmptyMagazinesReload(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        ArsenalData.set(cannon, ArsenalData.SHOTS, Rules.LANCE_MAGAZINE - 1);
        ignite(h, p, cannon);
        p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        cannon.getItem().inventoryTick(cannon, h.getLevel(), p, 1, false);
        p.setItemSlot(EquipmentSlot.MAINHAND, cannon);
        h.assertTrue(!JetLanceItem.tryFire(p), "switching away allowed a fifth shot without reloading");
        h.assertTrue(Weapons.reloadProgress(cannon, h.getLevel().getGameTime(), 0) >= 0, "putting an empty cannon away must start its reload");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "expiredShotsStillReloadAnEmptyMagazine")
    public static void expiredShotsStillReloadAnEmptyMagazine(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        ArsenalData.set(cannon, ArsenalData.SHOTS, Rules.LANCE_MAGAZINE);
        ArsenalData.set(cannon, ArsenalData.JET_UNTIL, h.getLevel().getGameTime() - Rules.LANCE_SHOT_GAP);
        h.assertTrue(!JetLanceItem.tryFire(p), "an expired fourth shot allowed a fifth shot");
        JetLanceItem.tickShot(cannon, p, h.getLevel());
        h.assertTrue(Weapons.reloadProgress(cannon, h.getLevel().getGameTime(), 0) >= 0, "missing the final shot tick must not skip the reload");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "onlyTheFirstInstantOfAShotDrillsTerrain")
    public static void onlyTheFirstInstantOfAShotDrillsTerrain(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 0.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        var near = new net.minecraft.core.BlockPos(2, 2, 2);
        var far = new net.minecraft.core.BlockPos(2, 2, 4);
        var chest = new net.minecraft.core.BlockPos(3, 2, 3);
        p.setYRot(0);
        p.setXRot(0);
        h.setBlock(near, Blocks.STONE);
        h.setBlock(far, Blocks.OAK_PLANKS);
        h.setBlock(chest, Blocks.CHEST);
        ignite(h, p, cannon);
        h.assertBlockPresent(Blocks.STONE, near);
        ArsenalData.remove(cannon, ArsenalData.JET_UNTIL);
        ArsenalData.set(cannon, ArsenalData.TERRAIN_MODE, 1);
        ignite(h, p, cannon);
        h.assertBlockPresent(Blocks.AIR, near);
        h.assertBlockPresent(Blocks.AIR, far);
        h.assertBlockPresent(Blocks.CHEST, chest);
        h.setBlock(near, Blocks.STONE);
        JetLanceItem.pulse(p, h.getLevel());
        h.assertBlockPresent(Blocks.STONE, near);
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "reaverFocusIsCarriedOnTheStack")
    public static void reaverFocusIsCarriedOnTheStack(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack reaver = hold(p, new ItemStack(ArsenalItems.TIDAL_REAVER.get()));
        long now = h.getLevel().getGameTime();
        h.assertTrue(TidalReaverItem.focusProgress(reaver, now, 0) < 0, "focus open before use");
        reaver.getItem().use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(TidalReaverItem.focusProgress(reaver, now, 0) == 0, "other clients need the focus on the stack");
        p.stopUsingItem();
        reaver.getItem().inventoryTick(reaver, h.getLevel(), p, 0, true);
        h.assertTrue(TidalReaverItem.focusProgress(reaver, now, 0) < 0, "focus stayed open after the hold ended");
        h.succeed();
    }

    /** In a batch of its own: its line of fire is longer than the gap to the next test's structure, and machines stop the drill. */
    @GameTest(template = ARENA, batch = "drilling_alone")
    public static void drillingGivesOutAtItsDepth(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 0.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        ArsenalData.set(cannon, ArsenalData.TERRAIN_MODE, 1);
        p.setYRot(0);
        p.setXRot(0);
        int depth = (int) Rules.LANCE_BORE_DEPTH;
        var first = new net.minecraft.core.BlockPos(2, 2, 2);
        var inside = first.south(depth - 1);
        var beyond = first.south(depth + 4);
        h.setBlock(first, Blocks.STONE);
        h.setBlock(inside, Blocks.STONE);
        h.setBlock(beyond, Blocks.STONE);
        // The arena's own barrier wall is unbreakable and would rightly stop the drill.
        h.setBlock(first.south(3), Blocks.AIR);
        // GameTests only keep their own structure's chunks loaded, and the drill stops at unloaded ground.
        var level = h.getLevel();
        net.minecraft.world.level.ChunkPos start = new net.minecraft.world.level.ChunkPos(h.absolutePos(first)), stop = new net.minecraft.world.level.ChunkPos(h.absolutePos(beyond));
        int low = Math.min(start.z, stop.z), high = Math.max(start.z, stop.z);
        for (int z = low; z <= high; z++) for (int x = start.x - 1; x <= start.x + 1; x++) level.setChunkForced(x, z, true);
        h.runAfterDelay(10, () -> {
            ignite(h, p, cannon);
            for (int z = low; z <= high; z++) for (int x = start.x - 1; x <= start.x + 1; x++) level.setChunkForced(x, z, false);
            h.assertBlockPresent(Blocks.AIR, first);
            h.assertBlockPresent(Blocks.AIR, inside);
            h.assertBlockPresent(Blocks.STONE, beyond);
            h.succeed();
        });
    }

    @GameTest(template = ARENA, batch = "meltdownGoesOffTheInstantTheShotLands")
    public static void meltdownGoesOffTheInstantTheShotLands(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 1.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        Modules.install(cannon, List.of(module(ModuleType.MELTDOWN)));
        Fe.set(cannon, Rules.LANCE_CAPACITY);
        p.setYRot(0);
        p.setXRot(0);
        h.setBlock(2, 2, 4, Blocks.STONE);
        Zombie aside = target(h, 4.5, 4.5);
        // As with the horizon wave, do not fire a one-shot blast before the freshly
        // forced test chunk exposes the spawned target to the actual spatial query.
        h.startSequence().thenWaitUntil(() -> h.assertTrue(
                io.github.genichimaruo.singuloarsenal.core.ArsenalDamage.around(
                        p, aside.position(), Rules.blastRadius(1)).contains(aside),
                "meltdown target not yet visible in the loaded entity section"))
                .thenExecute(() -> {
                    h.assertTrue(aside.getHealth() == 100, "meltdown target was damaged before firing");
                    ignite(h, p, cannon);
                    h.assertTrue(aside.getHealth() < 100, "the blast must land with the shot");
                    h.assertBlockPresent(Blocks.STONE, new net.minecraft.core.BlockPos(2, 2, 4));
                    h.assertTrue(p.getHealth() == p.getMaxHealth(), "the blast hurt its owner");
                }).thenSucceed();
    }

    @GameTest(template = ARENA, batch = "platingMergesIntoTheArmorValue")
    public static void platingMergesIntoTheArmorValue(GameTestHelper h) {
        ItemStack chest = charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE);
        Modules.install(chest, List.of(module(ModuleType.PLATING), module(ModuleType.PLATING)));
        int armorLines = 0, toughnessLines = 0;
        double armor = 0, toughness = 0;
        for (var entry : chest.getAttributeModifiers(EquipmentSlot.CHEST).entries()) {
            if (entry.getKey().equals(Attributes.ARMOR)) { armorLines++; armor += entry.getValue().getAmount(); }
            if (entry.getKey().equals(Attributes.ARMOR_TOUGHNESS)) { toughnessLines++; toughness += entry.getValue().getAmount(); }
        }
        h.assertTrue(armorLines == 1 && toughnessLines == 1, "armor shown on several lines: " + armorLines + ", " + toughnessLines);
        h.assertTrue(armor == 10 + 2 * Rules.PLATING_ARMOR && toughness == ArmorSet.COLLAPSAR.toughness + 2 * Rules.PLATING_TOUGHNESS, "merged totals: " + armor + ", " + toughness);
        h.succeed();
    }

    // ---- Module Integrator -------------------------------------------------------------------------------------------

    private static ItemStack module(ModuleType type) {
        return new ItemStack(ModuleContent.MODULES.get(type).get());
    }

    private static IntegratorBlockEntity integrator(GameTestHelper h) {
        h.setBlock(2, 2, 2, ModuleContent.INTEGRATOR.get());
        IntegratorBlockEntity entity = (IntegratorBlockEntity) h.getBlockEntity(new net.minecraft.core.BlockPos(2, 2, 2));
        entity.energy().receiveEnergy(Rules.INTEGRATOR_CAPACITY, false);
        return entity;
    }

    private static void run(IntegratorBlockEntity entity) {
        entity.start();
        for (int i = 0; i < Rules.INTEGRATE_TICKS; i++) entity.serverTick();
    }

    @GameTest(template = ARENA, batch = "integrationInstallsAndPaysForModules")
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

    @GameTest(template = ARENA, batch = "placingGearUnpacksItsModulesExactlyOnce")
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

    @GameTest(template = ARENA, batch = "legacyIntegratorSaveGainsFiveSocketsWithoutLosingModules")
    public static void legacyIntegratorSaveGainsFiveSocketsWithoutLosingModules(GameTestHelper h) {
        IntegratorBlockEntity entity = integrator(h);
        entity.items().setStackInSlot(0, charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE));
        for (int slot = 1; slot <= 3; slot++) entity.items().setStackInSlot(slot, module(ModuleType.CAPACITOR));
        var saved = entity.saveWithoutMetadata();
        saved.getCompound("items").putInt("Size", 4);
        entity.load(saved);
        h.assertTrue(entity.items().getSlots() == 6, "a 0.1.0 save must expand to five sockets");
        for (int slot = 1; slot <= 3; slot++) h.assertTrue(entity.items().getStackInSlot(slot).is(module(ModuleType.CAPACITOR).getItem()), "old socket lost its module");
        entity.items().setStackInSlot(4, module(ModuleType.INDUCTION));
        entity.items().setStackInSlot(5, module(ModuleType.COLLECTOR));
        run(entity);
        ItemStack gear = entity.items().getStackInSlot(0);
        h.assertTrue(Modules.installed(gear).size() == 5 && Modules.count(gear, ModuleType.CAPACITOR) == 3, "all five modules must survive the migration and integrate");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "integrationRefusesWrongOrExcessModules")
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

    @GameTest(template = ARENA, batch = "bitsPayEnergyAndHitHostiles")
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

    @GameTest(template = ARENA, batch = "fluxPinConsumesTheLastPartialCharge")
    public static void fluxPinConsumesTheLastPartialCharge(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack boots = charged(ArmorSet.MEISSNER, ArmorItem.Type.BOOTS);
        Fe.set(boots, 1);
        p.setItemSlot(EquipmentSlot.FEET, boots);
        p.setOnGround(false);
        p.setShiftKeyDown(true);
        p.tickCount = 10;
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.START, p));
        h.assertTrue(Fe.stored(boots) == 0, "partial charge allowed indefinite free pinning");
        h.assertTrue(!SuitEvents.pinned(p), "empty boots still pin");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "balancingDifferentCapacitiesConservesEnergy")
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

    @GameTest(template = ARENA, batch = "integratorChargingMarksTheChunkForSaving")
    public static void integratorChargingMarksTheChunkForSaving(GameTestHelper h) {
        IntegratorBlockEntity entity = integrator(h);
        var chunk = h.getLevel().getChunkAt(entity.getBlockPos());
        entity.energy().deserializeNBT(net.minecraft.nbt.IntTag.valueOf(0));
        chunk.setUnsaved(false);
        entity.energy().receiveEnergy(1000, true);
        h.assertTrue(!chunk.isUnsaved(), "simulated charging dirtied the chunk");
        entity.energy().receiveEnergy(1000, false);
        h.assertTrue(chunk.isUnsaved(), "received FE may disappear after a chunk reload");
        entity.invalidateCaps();
        h.assertTrue(!entity.getCapability(ForgeCapabilities.ENERGY).isPresent(), "invalidated energy capability stayed available");
        entity.reviveCaps();
        var energy = entity.getCapability(ForgeCapabilities.ENERGY).orElseThrow(() -> new AssertionError("energy capability did not revive"));
        h.assertTrue(energy.getEnergyStored() == 1000, "reviving a capability lost stored energy");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "holdingArmedBladeDoesNotModifyDroneDamage")
    public static void holdingArmedBladeDoesNotModifyDroneDamage(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack blade = hold(p, new ItemStack(ArsenalItems.CASIMIR_BLADE.get()));
        Modules.install(blade, List.of(module(ModuleType.AMPLIFIER)));
        long armedUntil = h.getLevel().getGameTime() + 100;
        ArsenalData.set(blade, ArsenalData.ARMED_UNTIL, armedUntil);
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

    @GameTest(template = ARENA, batch = "cannonPulseUsesItsAmplifier")
    public static void cannonPulseUsesItsAmplifier(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack cannon = hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        Modules.install(cannon, List.of(module(ModuleType.AMPLIFIER)));
        Zombie mob = target(h, 2.5, 4.5);
        // Wait for the actual beam query to see the fixture, then measure one pulse only.
        h.startSequence().thenWaitUntil(() -> h.assertTrue(
                io.github.genichimaruo.singuloarsenal.core.ArsenalDamage.along(p, p.getEyePosition(),
                        p.getEyePosition().add(p.getLookAngle().normalize().scale(Rules.LANCE_RANGE)),
                        Rules.LANCE_HIT_WIDTH).contains(mob), "amplifier target not yet visible to beam query"))
                .thenExecute(() -> {
                    h.assertTrue(mob.getHealth() == 100, "amplifier target was damaged before firing");
                    JetLanceItem.pulse(p, h.getLevel());
                    near(h, 100 - mob.getHealth(), Rules.LANCE_DAMAGE * Rules.amplified(1), "cannon amplification");
                }).thenSucceed();
    }

    // ---- Tools ---------------------------------------------------------------------------------------------------------

    private static net.minecraft.core.BlockPos abs(GameTestHelper h, int x, int y, int z) {
        return h.absolutePos(new net.minecraft.core.BlockPos(x, y, z));
    }

    /** Fills the 3x3 face at z = 3 and aims the player at its centre. */
    private static ServerPlayer facingWall(GameTestHelper h, net.minecraft.world.level.block.Block block) {
        ServerPlayer p = player(h, 2.5, 1.5);
        p.setYRot(0);
        p.setXRot(0);
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.setBlock(x, y, 3, block);
        return p;
    }

    private static void strike(GameTestHelper h, ServerPlayer p) {
        p.gameMode.destroyBlock(abs(h, 2, 2, 3));
    }

    @GameTest(template = ARENA, batch = "breakerTakesThreeByThreeUnlessSneaking")
    public static void breakerTakesThreeByThreeUnlessSneaking(GameTestHelper h) {
        ServerPlayer p = facingWall(h, Blocks.STONE);
        ItemStack breaker = hold(p, new ItemStack(ArsenalItems.DEGENERACY_BREAKER.get()));
        p.setShiftKeyDown(true);
        strike(h, p);
        h.assertBlockPresent(Blocks.AIR, new net.minecraft.core.BlockPos(2, 2, 3));
        h.assertBlockPresent(Blocks.STONE, new net.minecraft.core.BlockPos(1, 2, 3));
        h.setBlock(2, 2, 3, Blocks.STONE);
        p.setShiftKeyDown(false);
        int before = Fe.stored(breaker);
        strike(h, p);
        for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++) h.assertBlockPresent(Blocks.AIR, new net.minecraft.core.BlockPos(x, y, 3));
        h.assertTrue(before - Fe.stored(breaker) == 9 * Rules.BREAKER_FE, "nine blocks, nine payments: " + (before - Fe.stored(breaker)));
        h.assertTrue(breaker.getDamageValue() == 0, "powered tools do not wear out");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "breakerLeavesContainersAlone")
    public static void breakerLeavesContainersAlone(GameTestHelper h) {
        ServerPlayer p = facingWall(h, Blocks.STONE);
        hold(p, new ItemStack(ArsenalItems.DEGENERACY_BREAKER.get()));
        h.setBlock(1, 2, 3, Blocks.CHEST);
        strike(h, p);
        h.assertBlockPresent(Blocks.CHEST, new net.minecraft.core.BlockPos(1, 2, 3));
        h.assertBlockPresent(Blocks.AIR, new net.minecraft.core.BlockPos(3, 2, 3));
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "canceledMiningDoesNotBreakNeighborsOrSpendEnergy")
    public static void canceledMiningDoesNotBreakNeighborsOrSpendEnergy(GameTestHelper h) {
        ServerPlayer p = facingWall(h, Blocks.STONE);
        ItemStack breaker = hold(p, new ItemStack(ArsenalItems.DEGENERACY_BREAKER.get()));
        int before = Fe.stored(breaker);
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> protect = event -> {
            if (event.getPlayer() == p && event.getPos().equals(abs(h, 2, 2, 3))) event.setCanceled(true);
        };
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, protect);
        try {
            strike(h, p);
            for (int x = 1; x <= 3; x++) for (int y = 1; y <= 3; y++)
                h.assertBlockPresent(Blocks.STONE, new net.minecraft.core.BlockPos(x, y, 3));
            h.assertTrue(Fe.stored(breaker) == before, "a canceled break must not cost FE");
        } finally {
            bus.unregister(protect);
        }
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "protectedNeighborsDoNotCostMiningEnergy")
    public static void protectedNeighborsDoNotCostMiningEnergy(GameTestHelper h) {
        ServerPlayer p = facingWall(h, Blocks.STONE);
        ItemStack breaker = hold(p, new ItemStack(ArsenalItems.DEGENERACY_BREAKER.get()));
        int before = Fe.stored(breaker);
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> protect = event -> {
            if (event.getPlayer() == p && !event.getPos().equals(abs(h, 2, 2, 3))) event.setCanceled(true);
        };
        var bus = net.minecraftforge.common.MinecraftForge.EVENT_BUS;
        bus.addListener(protect);
        try {
            strike(h, p);
            h.assertBlockPresent(Blocks.AIR, new net.minecraft.core.BlockPos(2, 2, 3));
            h.assertBlockPresent(Blocks.STONE, new net.minecraft.core.BlockPos(1, 2, 3));
            h.assertTrue(before - Fe.stored(breaker) == Rules.BREAKER_FE, "only the permitted block may cost FE: " + (before - Fe.stored(breaker)));
        } finally {
            bus.unregister(protect);
        }
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "sawFellsLivingTreesButNotLogWalls")
    public static void sawFellsLivingTreesButNotLogWalls(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 1.5);
        hold(p, new ItemStack(ArsenalItems.SHEAR_SAW.get()));
        for (int y = 1; y <= 3; y++) h.setBlock(1, y, 3, Blocks.OAK_LOG);
        p.gameMode.destroyBlock(abs(h, 1, 1, 3));
        h.assertBlockPresent(Blocks.OAK_LOG, new net.minecraft.core.BlockPos(1, 2, 3));
        for (int y = 1; y <= 3; y++) h.setBlock(3, y, 3, Blocks.OAK_LOG);
        h.setBlock(3, 4, 3, Blocks.OAK_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, false));
        p.gameMode.destroyBlock(abs(h, 3, 1, 3));
        h.assertBlockPresent(Blocks.AIR, new net.minecraft.core.BlockPos(3, 3, 3));
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "drillFollowsVeinsIntoThePack")
    public static void drillFollowsVeinsIntoThePack(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 1.5);
        ItemStack drill = hold(p, new ItemStack(ArsenalItems.ACCRETION_DRILL.get()));
        ArsenalData.set(drill, ArsenalData.TOOL_MODE, Rules.DRILL_VEIN);
        for (int x = 1; x <= 3; x++) h.setBlock(x, 2, 3, Blocks.IRON_ORE);
        h.setBlock(2, 3, 3, Blocks.STONE);
        p.gameMode.destroyBlock(abs(h, 1, 2, 3));
        h.assertBlockPresent(Blocks.AIR, new net.minecraft.core.BlockPos(3, 2, 3));
        h.assertBlockPresent(Blocks.STONE, new net.minecraft.core.BlockPos(2, 3, 3));
        h.assertTrue(p.getInventory().countItem(net.minecraft.world.item.Items.RAW_IRON) >= 3, "drops did not reach the pack");
        // Exercise the legacy XP redirect through accepted and canceled real breaks.
        ArsenalData.set(drill, ArsenalData.TOOL_MODE, 0);
        var ore = abs(h, 2, 2, 3);
        boolean[] canceled = {true};
        java.util.function.Consumer<net.minecraftforge.event.level.BlockEvent.BreakEvent> listener = event -> {
            if (event.getPlayer() == p && event.getPos().equals(ore)) {
                event.setExpToDrop(7);
                event.setCanceled(canceled[0]);
            }
        };
        var dropsRule = h.getLevel().getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS);
        boolean originalRule = dropsRule.get();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(listener);
        try {
            dropsRule.set(true, h.getLevel().getServer());
            h.getLevel().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
            int beforeXp = p.totalExperience, beforeFe = Fe.stored(drill);
            p.gameMode.destroyBlock(ore);
            h.assertTrue(p.totalExperience == beforeXp && Fe.stored(drill) == beforeFe && h.getLevel().getBlockState(ore).is(Blocks.DIAMOND_ORE),
                    "a canceled drill break must grant no XP and spend no energy");
            canceled[0] = false;
            p.gameMode.destroyBlock(ore);
            h.assertTrue(p.totalExperience == beforeXp + 7, "accepted ore XP must go directly to the drill user");
            int diamonds = p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND);
            h.assertTrue(diamonds == 1, "accepted diamond must reach the pack");
            dropsRule.set(false, h.getLevel().getServer());
            h.getLevel().setBlockAndUpdate(ore, Blocks.DIAMOND_ORE.defaultBlockState());
            p.gameMode.destroyBlock(ore);
            h.assertTrue(p.totalExperience == beforeXp + 7 && p.getInventory().countItem(net.minecraft.world.item.Items.DIAMOND) == diamonds,
                    "doTileDrops=false must suppress both inserted drops and XP");
        } finally {
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(listener);
            dropsRule.set(originalRule, h.getLevel().getServer());
        }
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "toolsWithoutPowerDigAtHandSpeed")
    public static void toolsWithoutPowerDigAtHandSpeed(GameTestHelper h) {
        ItemStack pick = new ItemStack(ArsenalItems.SQUID_PICK.get());
        h.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 1, "an empty tool must not dig at full speed");
        Fe.set(pick, Fe.capacity(pick));
        h.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 10, "a charged pick digs at its tier speed");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "squidPickFindsOresNearestFirst")
    public static void squidPickFindsOresNearestFirst(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        // Legacy GameTest worlds contain natural ores outside the 5x5 arena.
        // Isolate the full scan volume, then restore it even if an assertion fails.
        var naturalOres = new java.util.HashMap<net.minecraft.core.BlockPos, net.minecraft.world.level.block.state.BlockState>();
        int radius = Rules.SCAN_RADIUS;
        for (var pos : net.minecraft.core.BlockPos.betweenClosed(p.blockPosition().offset(-radius, -radius, -radius),
                p.blockPosition().offset(radius, radius, radius))) {
            if (h.getLevel().isLoaded(pos) && h.getLevel().getBlockState(pos).is(net.minecraftforge.common.Tags.Blocks.ORES)) {
                naturalOres.put(pos.immutable(), h.getLevel().getBlockState(pos));
                h.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
            }
        }
        try {
        h.setBlock(4, 2, 4, Blocks.COAL_ORE);
        h.setBlock(2, 2, 3, Blocks.IRON_ORE);
        h.setBlock(1, 2, 1, Blocks.DIAMOND_ORE);
        h.setBlock(3, 2, 1, Blocks.ANCIENT_DEBRIS);
        var low = io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.scan(p, 1);
        h.assertTrue(low.ores().size() == 2 && low.hidden() == 2, "sensitivity 1 sees coal and iron only: " + low);
        h.assertTrue(low.ores().get(0).equals(abs(h, 2, 2, 3)), "nearest ore must come first: " + low.ores().get(0));
        h.assertTrue(io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.scan(p, 2).ores().size() == 3, "sensitivity 2 adds diamond");
        var full = io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.scan(p, 3);
        h.assertTrue(full.ores().size() == 4 && full.hidden() == 0, "sensitivity 3 sees everything: " + full);
        } finally {
            naturalOres.forEach((pos, state) -> h.getLevel().setBlockAndUpdate(pos, state));
        }
        h.succeed();
    }

    private static ItemStack singulo(String path) {
        return new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation("singulo", path)));
    }

    @GameTest(template = ARENA, batch = "sensitivityModulesTuneThePickInTheOtherHand")
    public static void sensitivityModulesTuneThePickInTheOtherHand(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack pick = new ItemStack(ArsenalItems.SQUID_PICK.get());
        p.setItemSlot(EquipmentSlot.OFFHAND, pick);
        p.setItemSlot(EquipmentSlot.MAINHAND, singulo("scanner_module_3"));
        p.gameMode.useItem(p, h.getLevel(), p.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.tier(p.getOffhandItem()) == 3, "Mk3 must raise the pick to 3");
        h.assertTrue(p.getMainHandItem().isEmpty(), "the module is used up");
        p.setItemSlot(EquipmentSlot.MAINHAND, singulo("scanner_module_2"));
        p.gameMode.useItem(p, h.getLevel(), p.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.tier(p.getOffhandItem()) == 3 && !p.getMainHandItem().isEmpty(),
                "a lower module neither lowers the pick nor is spent");
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    @GameTest(template = ARENA, batch = "pickKeepsTheScannersSensitivity")
    public static void pickKeepsTheScannersSensitivity(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack scanner = singulo("neutrino_scanner");
        scanner.getOrCreateTag().putInt("singulo:scanner_tier", 2);
        p.containerMenu = p.inventoryMenu;
        p.inventoryMenu.getSlot(1).set(scanner);
        ItemStack pick = new ItemStack(ArsenalItems.SQUID_PICK.get());
        pick.onCraftedBy(h.getLevel(), p, 1);
        h.assertTrue(io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.tier(pick) == 2, "the scanner's sensitivity must carry over");
        p.inventoryMenu.getSlot(1).set(ItemStack.EMPTY);
        h.succeed();
    }

    @SuppressWarnings("unchecked")
    @GameTest(template = ARENA, batch = "craftingPickPreservesSensitivityForClickAndShiftClick")
    public static void craftingPickPreservesSensitivityForClickAndShiftClick(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        for (boolean shift : new boolean[]{false, true}) {
            p.getInventory().clearContent();
            var menu = new net.minecraft.world.inventory.CraftingMenu(1, p.getInventory(),
                    net.minecraft.world.inventory.ContainerLevelAccess.create(h.getLevel(), abs(h, 2, 1, 2)));
            p.containerMenu = menu;
            ItemStack scanner = singulo("neutrino_scanner");
            scanner.getOrCreateTag().putInt("singulo:scanner_tier", 3);
            menu.getSlot(1).set(new ItemStack(ArsenalItems.FLUX_WEAVE.get()));
            menu.getSlot(2).set(scanner);
            menu.getSlot(3).set(new ItemStack(ArsenalItems.FLUX_WEAVE.get()));
            menu.getSlot(5).set(singulo("superconducting_coil"));
            menu.getSlot(8).set(singulo("white_ceramic_composite"));
            h.assertTrue(menu.getSlot(0).getItem().is(ArsenalItems.SQUID_PICK.get()), "the real recipe must match");
            menu.clicked(0, 0, shift ? net.minecraft.world.inventory.ClickType.QUICK_MOVE : net.minecraft.world.inventory.ClickType.PICKUP, p);
            ItemStack crafted = shift ? p.getInventory().items.stream().filter(s -> s.is(ArsenalItems.SQUID_PICK.get())).findFirst().orElse(ItemStack.EMPTY) : menu.getCarried();
            h.assertTrue(crafted.is(ArsenalItems.SQUID_PICK.get()) && io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.tier(crafted) == 3,
                    "crafted pick lost sensitivity; shift=" + shift);
            h.assertTrue(menu.getSlot(2).getItem().isEmpty() && menu.getSlot(0).getItem().isEmpty(), "crafting must consume the scanner exactly once");
        }
        p.containerMenu = p.inventoryMenu;
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "canisterStoresPelletsAndFeedsGearFirst")
    public static void canisterStoresPelletsAndFeedsGearFirst(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack canister = new ItemStack(ArsenalItems.PELLET_CANISTER.get());
        p.setItemSlot(EquipmentSlot.MAINHAND, canister);
        p.getInventory().add(new ItemStack(Fe.MASS_PELLET.get(), 64));
        p.getInventory().add(new ItemStack(Fe.MASS_PELLET.get(), 36));
        canister.use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(io.github.genichimaruo.singuloarsenal.core.PelletCanisterItem.stored(p.getMainHandItem()) == 100, "all pellets go in");
        h.assertTrue(p.getInventory().countItem(Fe.MASS_PELLET.get()) == 0, "none left loose");
        p.getInventory().add(new ItemStack(Fe.MASS_PELLET.get(), 5));
        ItemStack chest = new ItemStack(ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR).get(ArmorItem.Type.CHESTPLATE).get());
        h.assertTrue(Fe.accrete(p, chest) && Fe.stored(chest) == Rules.PELLET_FE, "accretion must still work");
        h.assertTrue(io.github.genichimaruo.singuloarsenal.core.PelletCanisterItem.stored(p.getMainHandItem()) == 99
                && p.getInventory().countItem(Fe.MASS_PELLET.get()) == 5, "the canister pays before loose pellets");
        p.setShiftKeyDown(true);
        p.getMainHandItem().use(h.getLevel(), p, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(io.github.genichimaruo.singuloarsenal.core.PelletCanisterItem.stored(p.getMainHandItem()) == 35
                && p.getInventory().countItem(Fe.MASS_PELLET.get()) == 69, "sneaking takes one stack back out");
        h.succeed();
    }

    // ---- Utility modules ----------------------------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "smelterCooksOresButNotStone")
    public static void smelterCooksOresButNotStone(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 1.5);
        ItemStack pick = new ItemStack(ArsenalItems.SQUID_PICK.get());
        Modules.install(pick, List.of(module(ModuleType.SMELTER)));
        pick = hold(p, pick);
        h.setBlock(2, 2, 3, Blocks.IRON_ORE);
        h.setBlock(1, 1, 3, Blocks.STONE);
        int before = Fe.stored(pick);
        p.gameMode.destroyBlock(abs(h, 2, 2, 3));
        p.gameMode.destroyBlock(abs(h, 1, 1, 3));
        var at = new net.minecraft.core.BlockPos(2, 2, 3);
        h.assertItemEntityPresent(net.minecraft.world.item.Items.IRON_INGOT, at, 2);
        h.assertItemEntityNotPresent(net.minecraft.world.item.Items.RAW_IRON, at, 3);
        h.assertItemEntityPresent(net.minecraft.world.item.Items.COBBLESTONE, new net.minecraft.core.BlockPos(1, 1, 3), 2);
        h.assertTrue(before - Fe.stored(pick) == 2 * Rules.PICK_FE + Rules.SMELT_FE, "two blocks and one smelt: " + (before - Fe.stored(pick)));
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "smeltingKeepsOutputsThatExceedOneStack")
    public static void smeltingKeepsOutputsThatExceedOneStack(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack pick = hold(p, new ItemStack(ArsenalItems.SQUID_PICK.get()));
        Modules.install(pick, List.of(module(ModuleType.SMELTER)));
        ItemStack input = new ItemStack(net.minecraft.world.item.Items.RAW_IRON, 40);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,
                new net.minecraft.world.SimpleContainer(input), h.getLevel()).orElseThrow();
        // Model a data pack recipe that yields two ingots per raw item, restoring it before any other test runs.
        ItemStack result = recipe.getResultItem(h.getLevel().registryAccess());
        int original = result.getCount();
        int before = Fe.stored(pick);
        try {
            result.setCount(2);
            var drops = io.github.genichimaruo.singuloarsenal.tool.Mining.processDrops(h.getLevel(), abs(h, 2, 2, 3), p, pick.copy(), List.of(input));
            h.assertTrue(drops.stream().allMatch(d -> d.is(net.minecraft.world.item.Items.IRON_INGOT)
                    && d.getCount() <= d.getMaxStackSize()), "smelting must produce valid ingot stacks");
            h.assertTrue(drops.stream().mapToInt(ItemStack::getCount).sum() == 80, "smelting discarded output above the stack limit");
            h.assertTrue(before - Fe.stored(pick) == 40 * Rules.SMELT_FE, "smelting must charge for inputs, not output stacks");
        } finally {
            result.setCount(original);
        }
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "inductionFeedsTheHeldToolFromTheChest")
    public static void inductionFeedsTheHeldToolFromTheChest(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        ItemStack chest = charged(ArmorSet.COLLAPSAR, ArmorItem.Type.CHESTPLATE);
        Modules.install(chest, List.of(module(ModuleType.INDUCTION)));
        Fe.set(chest, Fe.capacity(chest));
        p.setItemSlot(EquipmentSlot.CHEST, chest);
        p.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ArsenalItems.SQUID_PICK.get()));
        int chestBefore = Fe.stored(p.getItemBySlot(EquipmentSlot.CHEST));
        SuitEvents.induct(p);
        int got = Fe.stored(p.getMainHandItem());
        h.assertTrue(got == Rules.INDUCTION_FE_PER_SECOND, "one coil, one second: " + got);
        h.assertTrue(chestBefore - Fe.stored(p.getItemBySlot(EquipmentSlot.CHEST)) == got, "the charge must come out of the chest");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "collectorPullsLooseItemsButNotThrownOnes")
    public static void collectorPullsLooseItemsButNotThrownOnes(GameTestHelper h) {
        ServerPlayer p = player(h, 1.5, 1.5);
        ItemStack boots = charged(ArmorSet.MEISSNER, ArmorItem.Type.BOOTS);
        Modules.install(boots, List.of(module(ModuleType.COLLECTOR)));
        Fe.set(boots, Fe.capacity(boots));
        p.setItemSlot(EquipmentSlot.FEET, boots);
        Vec3 far = h.absoluteVec(new Vec3(4.5, 1, 4.5));
        var loose = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), far.x, far.y, far.z, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
        var thrown = new net.minecraft.world.entity.item.ItemEntity(h.getLevel(), far.x, far.y, far.z, new ItemStack(net.minecraft.world.item.Items.EMERALD));
        for (var item : List.of(loose, thrown)) {
            item.setNoPickUpDelay();
            item.setDeltaMovement(Vec3.ZERO);
            h.getLevel().addFreshEntity(item);
        }
        thrown.setThrower(p.getUUID());
        p.setShiftKeyDown(true);
        SuitEvents.collect(p);
        h.assertTrue(loose.distanceTo(p) > 3, "sneaking must hold the field off");
        p.setShiftKeyDown(false);
        SuitEvents.collect(p);
        h.assertTrue(loose.distanceTo(p) < 1, "a loose item must be brought in: " + loose.distanceTo(p));
        h.assertTrue(thrown.distanceTo(p) > 3, "what the wearer threw away stays put");
        h.succeed();
    }

    // ---- Recipes against the real base mod ---------------------------------------------------------------------------

    @GameTest(template = ARENA, batch = "cannonKeepsOnlyFortyPercentOfArmorReduction")
    public static void cannonKeepsOnlyFortyPercentOfArmorReduction(GameTestHelper h) {
        ServerPlayer p = player(h, 2.5, 2.5);
        hold(p, new ItemStack(ArsenalItems.JET_LANCE.get()));
        Zombie mob = target(h, 2.5, 4.5);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(20);
        float armored = net.minecraft.world.damagesource.CombatRules.getDamageAfterAbsorb(Rules.LANCE_DAMAGE, 20, 0);
        float expected = Rules.LANCE_DAMAGE - (Rules.LANCE_DAMAGE - armored) * Rules.LANCE_ARMOR_KEPT;
        JetLanceItem.pulse(p, h.getLevel());
        near(h, 100 - mob.getHealth(), expected, "cannon armor penetration");
        h.succeed();
    }

    @GameTest(template = ARENA, batch = "everyRecipeLoadsAgainstRealSingulo")
    public static void everyRecipeLoadsAgainstRealSingulo(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager();
        int found = 0;
        for (var entry : ArsenalItems.ITEMS.getEntries()) {
            ResourceLocation id = entry.getId();
            h.assertTrue(recipes.byKey(id).isPresent(), "recipe failed to load: " + id);
            found++;
        }
        h.assertTrue(found == 39, "unexpected item count " + found);
        var lamina = recipes.byKey(SinguloArsenal.id("collapsar_lamina")).orElseThrow();
        h.assertTrue(BuiltInRegistries.RECIPE_SERIALIZER.getKey(lamina.getSerializer()).toString().equals("singulo:machine"), "lamina must be a Singulo machine recipe");
        h.succeed();
    }
}
