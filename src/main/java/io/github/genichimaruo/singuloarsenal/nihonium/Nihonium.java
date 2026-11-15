package io.github.genichimaruo.singuloarsenal.nihonium;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.ForgeTier;
import net.minecraftforge.common.TierSortingRegistry;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

/** Plain vanilla-style nihonium equipment: no FE or powered abilities. */
public final class Nihonium {
    public static final TagKey<Item> INGOT = ItemTags.create(new ResourceLocation("forge", "ingots/nihonium"));
    public static final Tier TIER = TierSortingRegistry.registerTier(
            new ForgeTier(4, 4000, 10F, 4F, 15, BlockTags.create(SinguloArsenal.id("needs_nihonium_tool")), () -> Ingredient.of(INGOT)),
            SinguloArsenal.id("nihonium"), List.of(Tiers.NETHERITE), List.of());
    public static final int DURABILITY = 60;
    public static final float TOUGHNESS = 2.5F, KNOCKBACK_RESISTANCE = 0.05F;
    public static RegistryObject<SwordItem> SWORD;
    public static RegistryObject<PickaxeItem> PICKAXE;
    public static RegistryObject<AxeItem> AXE;
    public static RegistryObject<ShovelItem> SHOVEL;
    public static final Map<ArmorItem.Type, RegistryObject<ArmorItem>> ARMOR = new EnumMap<>(ArmorItem.Type.class);
    private Nihonium() {}
    public static void register(DeferredRegister<Item> items, List<ArmorItem.Type> pieces) {
        SWORD = items.register("nihonium_sword", () -> new SwordItem(TIER, 3, -2.4F, new Item.Properties()));
        PICKAXE = items.register("nihonium_pickaxe", () -> new PickaxeItem(TIER, 1, -2.8F, new Item.Properties()));
        AXE = items.register("nihonium_axe", () -> new AxeItem(TIER, 5F, -3F, new Item.Properties()));
        SHOVEL = items.register("nihonium_shovel", () -> new ShovelItem(TIER, 1.5F, -3F, new Item.Properties()));
        ArmorMaterial material = new ArmorMaterial() {
            public int getDurabilityForType(ArmorItem.Type type) { return DURABILITY * switch(type) {
                case HELMET -> 11; case CHESTPLATE -> 16; case LEGGINGS -> 15; case BOOTS -> 13;
            }; }
            public int getDefenseForType(ArmorItem.Type type) { return switch(type) {
                case HELMET, BOOTS -> 3; case CHESTPLATE -> 8; case LEGGINGS -> 7;
            }; }
            public int getEnchantmentValue() { return 15; }
            public SoundEvent getEquipSound() { return SoundEvents.ARMOR_EQUIP_DIAMOND; }
            public Ingredient getRepairIngredient() { return Ingredient.of(INGOT); }
            public String getName() { return "singulo_arsenal:nihonium"; }
            public float getToughness() { return TOUGHNESS; }
            public float getKnockbackResistance() { return KNOCKBACK_RESISTANCE; }
        };
        for (ArmorItem.Type type : pieces) ARMOR.put(type, items.register("nihonium_" + type.getName(),
                () -> new ArmorItem(material, type, new Item.Properties())));
    }
}
