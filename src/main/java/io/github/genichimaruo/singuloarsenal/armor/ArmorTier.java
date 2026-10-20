package io.github.genichimaruo.singuloarsenal.armor;

public enum ArmorTier {
    EXPLORER("explorer", 1, 18, new int[]{2, 5, 6, 2}, 0, 0, "steel_plate"),
    CATALYTIC("catalytic", 2, 33, new int[]{3, 6, 8, 3}, 2, 0, "superconducting_coil"),
    INERTIAL("inertial", 3, 37, new int[]{3, 6, 8, 3}, 2, 100_000, "quantum_computing_module"),
    BOUNDARY("boundary", 5, 40, new int[]{3, 6, 8, 3}, 3, 500_000, "gravity_field_stabilizer");

    public final String id;
    public final int stage;
    public final int durability;
    /** Boots, leggings, chestplate, helmet. */
    public final int[] defense;
    public final float toughness;
    public final int capacity;
    public final String repairMaterial;

    ArmorTier(String id, int stage, int durability, int[] defense, float toughness, int capacity, String repairMaterial) {
        this.id = id;
        this.stage = stage;
        this.durability = durability;
        this.defense = defense;
        this.toughness = toughness;
        this.capacity = capacity;
        this.repairMaterial = repairMaterial;
    }
}
