package io.github.genichimaruo.singuloarsenal.tool;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;

/** Mining tiers. Tools run on FE and never wear out, so durability exists only because tiered items must declare one. */
public enum ToolTier implements Tier {
    SQUID(10, BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 16, "superconducting_coil"),
    PRESSURE(16, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 12, "degenerate_matter_shell"),
    ACCRETION(40, BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 22, "collapsar_ingot");

    private final float speed;
    private final TagKey<Block> incorrect;
    private final int enchantment;
    private final String repair;

    ToolTier(float speed, TagKey<Block> incorrect, int enchantment, String repair) {
        this.speed = speed;
        this.incorrect = incorrect;
        this.enchantment = enchantment;
        this.repair = repair;
    }

    @Override public int getUses() { return 1000; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return 0; }
    @Override public TagKey<Block> getIncorrectBlocksForDrops() { return incorrect; }
    @Override public int getEnchantmentValue() { return enchantment; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("singulo", repair)));
    }
}
