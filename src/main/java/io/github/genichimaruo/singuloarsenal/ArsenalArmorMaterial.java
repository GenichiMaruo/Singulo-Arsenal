package io.github.genichimaruo.singuloarsenal;

import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

final class ArsenalArmorMaterial implements ArmorMaterial {
    private final ArmorSet set;
    ArsenalArmorMaterial(ArmorSet set) { this.set = set; }
    public int getDurabilityForType(ArmorItem.Type type) { return switch (type) {
        case HELMET -> 11 * set.durability;
        case CHESTPLATE -> 16 * set.durability;
        case LEGGINGS -> 15 * set.durability;
        case BOOTS -> 13 * set.durability;
    }; }
    public int getDefenseForType(ArmorItem.Type type) { return switch (type) {
        case HELMET -> set.defense[3]; case CHESTPLATE -> set.defense[2];
        case LEGGINGS -> set.defense[1]; case BOOTS -> set.defense[0];
    }; }
    public int getEnchantmentValue() { return set.enchantability; }
    public SoundEvent getEquipSound() { return SoundEvents.EMPTY; }
    public Ingredient getRepairIngredient() { return Ingredient.of(BuiltInRegistries.ITEM.get(new ResourceLocation("singulo", set.repairMaterial))); }
    public String getName() { return "singulo_arsenal:" + set.id; }
    public float getToughness() { return set.toughness; }
    public float getKnockbackResistance() { return set.knockbackResistance; }
}
