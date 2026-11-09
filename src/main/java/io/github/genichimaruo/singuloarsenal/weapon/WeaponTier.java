package io.github.genichimaruo.singuloarsenal.weapon;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Melee tiers. The damage bonus is zero because each weapon states its full damage directly. */
public enum WeaponTier implements Tier {
    QUENCH(1800, 16, "superconducting_coil"),
    CASIMIR(3200, 12, "exotic_matter"),
    REAVER(6400, 22, "collapsar_ingot");

    private final int uses;
    private final int enchantment;
    private final String repair;

    WeaponTier(int uses, int enchantment, String repair) {
        this.uses = uses;
        this.enchantment = enchantment;
        this.repair = repair;
    }

    @Override public int getUses() { return uses; }
    @Override public float getSpeed() { return 8; }
    @Override public float getAttackDamageBonus() { return 0; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_NETHERITE_TOOL; }
    @Override public int getEnchantmentValue() { return enchantment; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("singulo", repair)));
    }
}
