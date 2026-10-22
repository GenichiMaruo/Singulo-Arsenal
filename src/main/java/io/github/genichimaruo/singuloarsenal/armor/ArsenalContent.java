package io.github.genichimaruo.singuloarsenal.armor;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArsenalContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SinguloArsenal.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, SinguloArsenal.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SinguloArsenal.MOD_ID);
    public static final Map<ArmorTier, Map<ArmorItem.Type, DeferredItem<ArsenalArmorItem>>> ARMOR = new EnumMap<>(ArmorTier.class);
    public static final DeferredItem<Item> CARTRIDGE = ITEMS.registerSimpleItem("circulation_cartridge");
    public static final DeferredItem<Item> INERTIAL_UPGRADE = ITEMS.registerSimpleItem("inertial_upgrade");
    public static final DeferredItem<Item> BOUNDARY_UPGRADE = ITEMS.registerSimpleItem("boundary_upgrade");

    static {
        for (ArmorTier tier : ArmorTier.values()) {
            Holder<ArmorMaterial> material = MATERIALS.register(tier.id, () -> {
                var defense = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
                defense.put(ArmorItem.Type.BOOTS, tier.defense[0]);
                defense.put(ArmorItem.Type.LEGGINGS, tier.defense[1]);
                defense.put(ArmorItem.Type.CHESTPLATE, tier.defense[2]);
                defense.put(ArmorItem.Type.HELMET, tier.defense[3]);
                defense.put(ArmorItem.Type.BODY, tier.defense[2]);
                return new ArmorMaterial(defense, 15, SoundEvents.ARMOR_EQUIP_IRON,
                        () -> Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("singulo", tier.repairMaterial))),
                        List.of(new ArmorMaterial.Layer(SinguloArsenal.id(tier.id))), tier.toughness, 0);
            });
            var pieces = new LinkedHashMap<ArmorItem.Type, DeferredItem<ArsenalArmorItem>>();
            for (ArmorItem.Type type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
                pieces.put(type, ITEMS.register(tier.id + "_" + type.getName(), () -> new ArsenalArmorItem(material, type, tier)));
            }
            ARMOR.put(tier, pieces);
        }
        TABS.register("arsenal", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.singulo_arsenal"))
                .icon(() -> new ItemStack(ARMOR.get(ArmorTier.INERTIAL).get(ArmorItem.Type.CHESTPLATE).get()))
                .displayItems((params, output) -> {
                    ARMOR.values().forEach(pieces -> pieces.values().forEach(item -> output.accept(item.get())));
                    output.accept(CARTRIDGE.get());
                    output.accept(INERTIAL_UPGRADE.get());
                    output.accept(BOUNDARY_UPGRADE.get());
                    output.accept(io.github.genichimaruo.singuloarsenal.weapon.WeaponContent.RAILGUN.get());
                }).build());
    }

    private ArsenalContent() {}

    public static void register(IEventBus bus) {
        ArmorData.REGISTER.register(bus);
        MATERIALS.register(bus);
        ITEMS.register(bus);
        TABS.register(bus);
    }
}
