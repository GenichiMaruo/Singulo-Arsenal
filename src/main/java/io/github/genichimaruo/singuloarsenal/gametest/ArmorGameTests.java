package io.github.genichimaruo.singuloarsenal.gametest;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.damagesource.DamageContainer;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;

/** Integration tests run with the real Singulo JAR. These classes are excluded from distribution. */
@GameTestHolder(SinguloArsenal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArmorGameTests {
    private static final String ARENA = "singulo_arsenal:empty";

    private ArmorGameTests() {}

    private static ItemStack piece(ArmorTier tier, ArmorItem.Type type) {
        return new ItemStack(ArsenalContent.ARMOR.get(tier).get(type).get());
    }

    @GameTest(template = ARENA)
    public static void energyCapabilitySimulatesAndClamps(GameTestHelper h) {
        ItemStack chest = piece(ArmorTier.INERTIAL, ArmorItem.Type.CHESTPLATE);
        var energy = chest.getCapability(Capabilities.EnergyStorage.ITEM);
        h.assertTrue(energy != null, "FE capability missing");
        h.assertTrue(energy.receiveEnergy(200_000, true) == 100_000 && energy.getEnergyStored() == 0, "simulation mutated energy");
        h.assertTrue(energy.receiveEnergy(200_000, false) == 100_000, "capacity overflow");
        h.assertTrue(energy.extractEnergy(-1, false) == 0, "negative extraction accepted");
        h.assertTrue(energy.extractEnergy(200_000, false) == 100_000 && energy.getEnergyStored() == 0, "extraction overflow");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void stateSurvivesSaveAndLoad(GameTestHelper h) {
        ItemStack chest = piece(ArmorTier.BOUNDARY, ArmorItem.Type.CHESTPLATE);
        chest.set(ArmorData.ENERGY.get(), 123_456);
        chest.set(ArmorData.READY_AT.get(), 123_456L);
        chest.set(ArmorData.SHIELD.get(), 7);
        var restored = ItemStack.parse(h.getLevel().registryAccess(), chest.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(ArmorData.get(restored, ArmorData.ENERGY) == 123_456 && ArmorData.get(restored, ArmorData.SHIELD) == 7
                && ArmorData.time(restored, ArmorData.READY_AT) == 123_456L, "item state lost on save/load");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void repairConsumesOnlyWhenDamaged(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        ItemStack chest = piece(ArmorTier.CATALYTIC, ArmorItem.Type.CHESTPLATE);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ItemStack fuel = new ItemStack(ArsenalContent.CARTRIDGE.get(), 2);
        player.getInventory().add(fuel);
        // Ensure the test works even in a fresh world's first ten seconds.
        player.getPersistentData().putLong("singulo_arsenal.last_combat", h.getLevel().getGameTime() - 201);
        ArmorAbilities.repair(player, chest);
        h.assertTrue(player.getInventory().countItem(ArsenalContent.CARTRIDGE.get()) == 2, "fuel wasted on undamaged armor");
        chest.setDamageValue(10);
        ArmorAbilities.repair(player, chest);
        h.assertTrue(chest.getDamageValue() == 9 && ArmorData.get(chest, ArmorData.REPAIR) == 255, "repair did not spend one reserve point");
        h.assertTrue(player.getInventory().countItem(ArsenalContent.CARTRIDGE.get()) == 1, "cartridge count wrong");
        player.getPersistentData().putLong("singulo_arsenal.last_combat", h.getLevel().getGameTime());
        ArmorAbilities.repair(player, chest);
        h.assertTrue(chest.getDamageValue() == 9, "repair occurred during combat cooldown");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void leggingsExtendRepairOnlyToCatalyticArmor(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var chest = piece(ArmorTier.CATALYTIC, ArmorItem.Type.CHESTPLATE);
        var head = piece(ArmorTier.CATALYTIC, ArmorItem.Type.HELMET);
        head.setDamageValue(10);
        chest.set(ArmorData.REPAIR.get(), 10);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        player.setItemSlot(EquipmentSlot.HEAD, head);
        player.getPersistentData().putLong("singulo_arsenal.last_combat", h.getLevel().getGameTime() - 201);
        ArmorAbilities.repair(player, chest);
        h.assertTrue(head.getDamageValue() == 10, "chest alone repaired other pieces");
        player.setItemSlot(EquipmentSlot.LEGS, piece(ArmorTier.CATALYTIC, ArmorItem.Type.LEGGINGS));
        ArmorAbilities.repair(player, chest);
        h.assertTrue(head.getDamageValue() == 9, "leggings did not extend repair");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void barrierRequiresEnergyAndRejectsRepeatedActivation(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var chest = piece(ArmorTier.BOUNDARY, ArmorItem.Type.CHESTPLATE);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ArmorAbilities.activate(player, 2);
        h.assertTrue(ArmorData.get(chest, ArmorData.SHIELD) == 0, "unpowered barrier activated");
        player.getPersistentData().remove("singulo_arsenal.last_request");
        ArsenalArmorItem.energy(chest).receiveEnergy(100_000, false);
        player.setItemSlot(EquipmentSlot.LEGS, piece(ArmorTier.BOUNDARY, ArmorItem.Type.LEGGINGS));
        ArmorAbilities.activate(player, 2);
        h.assertTrue(ArmorData.active(chest, h.getLevel().getGameTime()), "powered barrier did not activate");
        h.assertTrue(ArmorData.time(chest, ArmorData.ACTIVE_UNTIL) == h.getLevel().getGameTime() + 100, "leggings did not extend duration");
        h.assertTrue(ArsenalArmorItem.energy(chest).getEnergyStored() == 80_000, "activation energy wrong");
        ArmorAbilities.activate(player, 2);
        h.assertTrue(ArsenalArmorItem.energy(chest).getEnergyStored() == 80_000, "duplicate request consumed energy");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void invalidActionCannotConsumeResources(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var chest = piece(ArmorTier.BOUNDARY, ArmorItem.Type.CHESTPLATE);
        ArsenalArmorItem.energy(chest).receiveEnergy(100_000, false);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        ArmorAbilities.activate(player, -1);
        ArmorAbilities.activate(player, Integer.MAX_VALUE);
        h.assertTrue(ArsenalArmorItem.energy(chest).getEnergyStored() == 100_000 && ArmorData.get(chest, ArmorData.SHIELD) == 0,
                "invalid packet changed resources");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void airborneDodgeDoesNotSpendImpact(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var legs = piece(ArmorTier.INERTIAL, ArmorItem.Type.LEGGINGS);
        player.setItemSlot(EquipmentSlot.LEGS, legs);
        legs.set(ArmorData.IMPACT.get(), 30);
        ArsenalArmorItem.energy(legs).receiveEnergy(10_000, false);
        player.setOnGround(false);
        ArmorAbilities.activate(player, 1);
        h.assertTrue(ArmorData.get(legs, ArmorData.IMPACT) == 30 && ArsenalArmorItem.energy(legs).getEnergyStored() == 10_000,
                "airborne dodge spent resources");
        player.getPersistentData().remove("singulo_arsenal.last_request");
        player.setOnGround(true);
        ArmorAbilities.activate(player, 1);
        h.assertTrue(ArmorData.get(legs, ArmorData.IMPACT) == 10 && ArsenalArmorItem.energy(legs).getEnergyStored() == 8_000,
                "ground dodge did not spend resources");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void smithingPreservesNameAndResetsStoredResources(GameTestHelper h) {
        var base = piece(ArmorTier.CATALYTIC, ArmorItem.Type.CHESTPLATE);
        base.set(DataComponents.CUSTOM_NAME, Component.literal("Recovered armor"));
        base.setDamageValue(12);
        var unbreaking = h.getLevel().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .getHolderOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING);
        base.enchant(unbreaking, 3);
        base.set(ArmorData.REPAIR.get(), 200);
        var addition = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("singulo", "quantum_computing_module")));
        var input = new SmithingRecipeInput(new ItemStack(ArsenalContent.INERTIAL_UPGRADE.get()), base, addition);
        var recipe = h.getLevel().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, h.getLevel()).orElseThrow();
        var result = recipe.value().assemble(input, h.getLevel().registryAccess());
        h.assertTrue(result.is(ArsenalContent.ARMOR.get(ArmorTier.INERTIAL).get(ArmorItem.Type.CHESTPLATE).get()), "wrong smithing output");
        h.assertTrue(result.getHoverName().getString().equals("Recovered armor") && result.getDamageValue() == 12, "name/damage lost");
        h.assertTrue(result.getEnchantments().getLevel(unbreaking) == 3, "enchantment lost");
        h.assertTrue(ArmorData.get(result, ArmorData.REPAIR) == 0 && ArmorData.get(result, ArmorData.ENERGY) == 0, "stored resources carried over");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void barrierPreservesExcessAndCannotBlockVoid(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var chest = piece(ArmorTier.BOUNDARY, ArmorItem.Type.CHESTPLATE);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        chest.set(ArmorData.SHIELD.get(), 16);
        chest.set(ArmorData.ACTIVE_UNTIL.get(), h.getLevel().getGameTime() + 100);
        var hit = new LivingDamageEvent.Pre(player, new DamageContainer(player.damageSources().generic(), 40));
        NeoForge.EVENT_BUS.post(hit);
        h.assertTrue(hit.getNewDamage() == 24 && ArmorData.get(chest, ArmorData.SHIELD) == 0, "shield removed excess damage");
        chest.set(ArmorData.SHIELD.get(), 16);
        var voidHit = new LivingDamageEvent.Pre(player, new DamageContainer(player.damageSources().fellOutOfWorld(), 40));
        NeoForge.EVENT_BUS.post(voidHit);
        h.assertTrue(voidHit.getNewDamage() == 40 && ArmorData.get(chest, ArmorData.SHIELD) == 16, "shield blocked void damage");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void fallingNeedsEnergyAndTinyFallsCannotFarmImpact(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var boots = piece(ArmorTier.INERTIAL, ArmorItem.Type.BOOTS);
        var chest = piece(ArmorTier.INERTIAL, ArmorItem.Type.CHESTPLATE);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        player.setItemSlot(EquipmentSlot.CHEST, chest);
        var unpowered = new LivingFallEvent(player, 10, 1);
        NeoForge.EVENT_BUS.post(unpowered);
        h.assertTrue(unpowered.getDamageMultiplier() == 1 && ArmorData.get(chest, ArmorData.IMPACT) == 0, "unpowered fall protection");
        ArsenalArmorItem.energy(boots).receiveEnergy(1_000, false);
        var tiny = new LivingFallEvent(player, 2, 1);
        NeoForge.EVENT_BUS.post(tiny);
        h.assertTrue(ArsenalArmorItem.energy(boots).getEnergyStored() == 1_000 && ArmorData.get(chest, ArmorData.IMPACT) == 0, "tiny fall consumed/generated resources");
        var large = new LivingFallEvent(player, 10, 1);
        NeoForge.EVENT_BUS.post(large);
        h.assertTrue(large.getDamageMultiplier() == 0.5F && ArsenalArmorItem.energy(boots).getEnergyStored() == 800
                && ArmorData.get(chest, ArmorData.IMPACT) == 5, "powered fall recovery wrong");
        h.succeed();
    }
}
