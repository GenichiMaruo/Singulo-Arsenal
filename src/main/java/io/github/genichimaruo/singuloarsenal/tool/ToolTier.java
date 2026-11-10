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
    SQUID(10, 3, 16, "superconducting_coil"),
    PRESSURE(16, 4, 12, "degenerate_matter_shell"),
    ACCRETION(40, 4, 22, "collapsar_ingot");

    private final float speed;
    private final int level;
    private final int enchantment;
    private final String repair;

    ToolTier(float speed, int level, int enchantment, String repair) {
        this.speed = speed;
        this.level = level;
        this.enchantment = enchantment;
        this.repair = repair;
    }

    @Override public int getUses() { return 1000; }
    @Override public float getSpeed() { return speed; }
    @Override public float getAttackDamageBonus() { return 0; }
    @Override public int getLevel() { return level; }
    @Override public int getEnchantmentValue() { return enchantment; }
    @Override public Ingredient getRepairIngredient() {
        return Ingredient.of(BuiltInRegistries.ITEM.get(new ResourceLocation("singulo", repair)));
    }
}
