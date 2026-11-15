package io.github.genichimaruo.singuloarsenal.nihonium;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.AxeItem;

import net.minecraft.world.item.Item;

import net.minecraft.world.item.ShovelItem;

import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Plain nihonium gear: vanilla items with no abilities and no FE, worn on the vanilla armor layer. Nihonium is a stage-3
 * Singulo metal, so its numbers sit above diamond and below the powered Degeneracy suit, with far more durability than either.
 */
public final class Nihonium {
    /** Singulo tags its ingot here, so this addon never names one of its classes. */
    public static final TagKey<Item> INGOT = ItemTags.create(Identifier.fromNamespaceAndPath("c", "ingots/nihonium"));

    /** Diamond: 1561 uses, speed 8, +3 damage, enchantability 10. Netherite: 2031, 9, +4, 15. */
    public static final ToolMaterial TIER = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 10F, 4F, 15, INGOT);

    /** Diamond 33 (helmet 363, chestplate 528); Netherite 37. */
    public static final int DURABILITY = 60;
    /** Boots, leggings, chestplate, helmet. Diamond is 3/6/8/3 with toughness 2; Degeneracy 3/6/8/3 with 3 and 0.1. */
    private static final int[] DEFENSE = {3, 7, 8, 3};
    public static final float TOUGHNESS = 2.5F;
    public static final float KNOCKBACK_RESISTANCE = 0.05F;

    public static DeferredItem<Item> SWORD;
    public static DeferredItem<Item> PICKAXE;
    public static DeferredItem<AxeItem> AXE;
    public static DeferredItem<ShovelItem> SHOVEL;
    public static final Map<ArmorType, DeferredItem<Item>> ARMOR = new EnumMap<>(ArmorType.class);

    private Nihonium() {}

    public static void register(DeferredRegister.Items items, List<ArmorType> pieces) {
        SWORD = items.registerItem("nihonium_sword", properties -> new Item(properties.sword(TIER, 3, -2.4F)));
        PICKAXE = items.registerItem("nihonium_pickaxe", properties -> new Item(properties.pickaxe(TIER, 1F, -2.8F)));
        AXE = items.registerItem("nihonium_axe", properties -> new AxeItem(TIER, 5F, -3F, properties));
        SHOVEL = items.registerItem("nihonium_shovel", properties -> new ShovelItem(TIER, 1.5F, -3F, properties));
        var defense = new EnumMap<ArmorType, Integer>(ArmorType.class);
        defense.put(ArmorType.BOOTS, DEFENSE[0]);
        defense.put(ArmorType.LEGGINGS, DEFENSE[1]);
        defense.put(ArmorType.CHESTPLATE, DEFENSE[2]);
        defense.put(ArmorType.HELMET, DEFENSE[3]);
        defense.put(ArmorType.BODY, 11);
        ArmorMaterial material = new ArmorMaterial(DURABILITY, defense, 15, SoundEvents.ARMOR_EQUIP_DIAMOND,
                TOUGHNESS, KNOCKBACK_RESISTANCE, INGOT,
                net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, SinguloArsenal.id("nihonium")));
        for (ArmorType type : pieces) {
            ARMOR.put(type, items.registerItem("nihonium_" + type.getName(),
                    properties -> new Item(properties.humanoidArmor(material, type))));
        }
    }
}