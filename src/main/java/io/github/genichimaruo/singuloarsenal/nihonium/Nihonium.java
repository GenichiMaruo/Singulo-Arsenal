package io.github.genichimaruo.singuloarsenal.nihonium;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.SimpleTier;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Plain nihonium gear: vanilla items with no abilities and no FE, worn on the vanilla armor layer. Nihonium is a stage-3
 * Singulo metal, so its numbers sit above diamond and below the powered Degeneracy suit, with far more durability than either.
 */
public final class Nihonium {
    /** Singulo tags its ingot here, so this addon never names one of its classes. */
    public static final TagKey<Item> INGOT = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/nihonium"));

    /** Diamond: 1561 uses, speed 8, +3 damage, enchantability 10. Netherite: 2031, 9, +4, 15. */
    public static final Tier TIER = new SimpleTier(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 4000, 10F, 4F, 15, () -> Ingredient.of(INGOT));

    /** Diamond 33 (helmet 363, chestplate 528); Netherite 37. */
    public static final int DURABILITY = 60;
    /** Boots, leggings, chestplate, helmet. Diamond is 3/6/8/3 with toughness 2; Degeneracy 3/6/8/3 with 3 and 0.1. */
    private static final int[] DEFENSE = {3, 7, 8, 3};
    public static final float TOUGHNESS = 2.5F;
    public static final float KNOCKBACK_RESISTANCE = 0.05F;

    public static DeferredItem<SwordItem> SWORD;
    public static DeferredItem<PickaxeItem> PICKAXE;
    public static DeferredItem<AxeItem> AXE;
    public static DeferredItem<ShovelItem> SHOVEL;
    public static final Map<ArmorItem.Type, DeferredItem<ArmorItem>> ARMOR = new EnumMap<>(ArmorItem.Type.class);

    private Nihonium() {}

    public static void register(DeferredRegister.Items items, DeferredRegister<ArmorMaterial> materials, List<ArmorItem.Type> pieces) {
        // Vanilla attack offsets: sword 3 / -2.4, pickaxe 1 / -2.8, axe 5 / -3.0 (as netherite), shovel 1.5 / -3.0.
        SWORD = items.register("nihonium_sword", () -> new SwordItem(TIER, new Item.Properties().attributes(SwordItem.createAttributes(TIER, 3, -2.4F))));
        PICKAXE = items.register("nihonium_pickaxe", () -> new PickaxeItem(TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TIER, 1F, -2.8F))));
        AXE = items.register("nihonium_axe", () -> new AxeItem(TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TIER, 5F, -3F))));
        SHOVEL = items.register("nihonium_shovel", () -> new ShovelItem(TIER, new Item.Properties().attributes(DiggerItem.createAttributes(TIER, 1.5F, -3F))));

        Holder<ArmorMaterial> material = materials.register("nihonium", () -> {
            var defense = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
            defense.put(ArmorItem.Type.BOOTS, DEFENSE[0]);
            defense.put(ArmorItem.Type.LEGGINGS, DEFENSE[1]);
            defense.put(ArmorItem.Type.CHESTPLATE, DEFENSE[2]);
            defense.put(ArmorItem.Type.HELMET, DEFENSE[3]);
            defense.put(ArmorItem.Type.BODY, 11);
            // textures/models/armor/nihonium_layer_1.png and _layer_2.png, drawn by the vanilla armor layer.
            return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(INGOT),
                    List.of(new ArmorMaterial.Layer(SinguloArsenal.id("nihonium"))), TOUGHNESS, KNOCKBACK_RESISTANCE);
        });
        for (ArmorItem.Type type : pieces) {
            ARMOR.put(type, items.register("nihonium_" + type.getName(),
                    () -> new ArmorItem(material, type, new Item.Properties().durability(type.getDurability(DURABILITY)))));
        }
    }
}
