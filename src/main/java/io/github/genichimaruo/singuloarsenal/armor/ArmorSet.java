package io.github.genichimaruo.singuloarsenal.armor;

import io.github.genichimaruo.singuloarsenal.core.Rules;

/** Each set is one physical idea; its numbers rise with what it costs to make in Singulo. */
public enum ArmorSet {
    MEISSNER("meissner", 2, 24, new int[]{2, 5, 6, 2}, 1, 0, Rules.MEISSNER_CAPACITY, "white_ceramic_composite", 16, false),
    DEGENERACY("degeneracy", 4, 42, new int[]{3, 6, 8, 3}, 3, 0.1F, Rules.DEGENERACY_CAPACITY, "degenerate_matter_shell", 12, true),
    COLLAPSAR("collapsar", 5, 80, new int[]{4, 7, 10, 4}, 5, 0.2F, Rules.COLLAPSAR_CAPACITY, "collapsar_ingot", 22, true);

    public final String id;
    public final int stage;
    public final int durability;
    /** Boots, leggings, chestplate, helmet. */
    public final int[] defense;
    public final float toughness;
    public final float knockbackResistance;
    public final int capacity;
    public final String repairMaterial;
    public final int enchantability;
    public final boolean fireproof;

    ArmorSet(String id, int stage, int durability, int[] defense, float toughness, float knockbackResistance, int capacity,
             String repairMaterial, int enchantability, boolean fireproof) {
        this.id = id;
        this.stage = stage;
        this.durability = durability;
        this.defense = defense;
        this.toughness = toughness;
        this.knockbackResistance = knockbackResistance;
        this.capacity = capacity;
        this.repairMaterial = repairMaterial;
        this.enchantability = enchantability;
        this.fireproof = fireproof;
    }
}
