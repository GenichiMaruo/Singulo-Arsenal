package io.github.genichimaruo.singuloarsenal;

import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.nihonium.Nihonium;
import io.github.genichimaruo.singuloarsenal.tool.AccretionDrillItem;
import io.github.genichimaruo.singuloarsenal.tool.DegeneracyBreakerItem;
import io.github.genichimaruo.singuloarsenal.tool.ShearSawItem;
import io.github.genichimaruo.singuloarsenal.tool.SquidPickItem;
import io.github.genichimaruo.singuloarsenal.weapon.CasimirBladeItem;
import io.github.genichimaruo.singuloarsenal.weapon.JetCannonItem;
import io.github.genichimaruo.singuloarsenal.weapon.QuenchSaberItem;
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ArsenalItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SinguloArsenal.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SinguloArsenal.MOD_ID);
    public static final List<ArmorType> PIECES = List.of(ArmorType.HELMET, ArmorType.CHESTPLATE, ArmorType.LEGGINGS, ArmorType.BOOTS);
    public static final Map<ArmorSet, Map<ArmorType, DeferredItem<SuitItem>>> SUITS = new EnumMap<>(ArmorSet.class);

    public static final DeferredItem<Item> FLUX_WEAVE = ITEMS.registerSimpleItem("flux_weave");
    public static final DeferredItem<Item> PRESSURE_LATTICE = ITEMS.registerSimpleItem("pressure_lattice");
    public static final DeferredItem<Item> COLLAPSAR_LAMINA = ITEMS.registerSimpleItem("collapsar_lamina");
    public static final DeferredItem<Item> JET_COLLIMATOR = ITEMS.registerSimpleItem("jet_collimator");
    public static final DeferredItem<Item> TIDAL_EDGE = ITEMS.registerSimpleItem("tidal_edge");

    public static final DeferredItem<QuenchSaberItem> QUENCH_SABER = ITEMS.registerItem("quench_saber", QuenchSaberItem::new);
    public static final DeferredItem<CasimirBladeItem> CASIMIR_BLADE = ITEMS.registerItem("casimir_blade", CasimirBladeItem::new);
    public static final DeferredItem<TidalReaverItem> TIDAL_REAVER = ITEMS.registerItem("tidal_reaver", TidalReaverItem::new);
    public static final DeferredItem<JetCannonItem> JET_CANNON = ITEMS.registerItem("jet_cannon", JetCannonItem::new);

    public static final DeferredItem<SquidPickItem> SQUID_PICK = ITEMS.registerItem("squid_pick", SquidPickItem::new);
    public static final DeferredItem<DegeneracyBreakerItem> DEGENERACY_BREAKER = ITEMS.registerItem("degeneracy_breaker", DegeneracyBreakerItem::new);
    public static final DeferredItem<ShearSawItem> SHEAR_SAW = ITEMS.registerItem("shear_saw", ShearSawItem::new);
    public static final DeferredItem<AccretionDrillItem> ACCRETION_DRILL = ITEMS.registerItem("accretion_drill", AccretionDrillItem::new);
    public static final DeferredItem<io.github.genichimaruo.singuloarsenal.core.PelletCanisterItem> PELLET_CANISTER =
            ITEMS.registerItem("pellet_canister", io.github.genichimaruo.singuloarsenal.core.PelletCanisterItem::new);

    static {
        // The registry alias resolves legacy stacks and datapack item references to the same item.
        // Components stay untouched; subsequent saves use the canonical jet_cannon ID.
        ITEMS.addAlias(Identifier.fromNamespaceAndPath(SinguloArsenal.MOD_ID, "jet_lance"),
                Identifier.fromNamespaceAndPath(SinguloArsenal.MOD_ID, "jet_cannon"));
        // The suit is drawn by SuitLayer, so the material has no texture layers. Its sound is silent because vanilla
        // replays the equip sound whenever a worn stack's components change, which here is every FE payment.
        Holder<net.minecraft.sounds.SoundEvent> silent = BuiltInRegistries.SOUND_EVENT.wrapAsHolder(SoundEvents.EMPTY);
        for (ArmorSet set : ArmorSet.values()) {
            var defense = new EnumMap<ArmorType, Integer>(ArmorType.class);
            defense.put(ArmorType.BOOTS, set.defense[0]);
            defense.put(ArmorType.LEGGINGS, set.defense[1]);
            defense.put(ArmorType.CHESTPLATE, set.defense[2]);
            defense.put(ArmorType.HELMET, set.defense[3]);
            defense.put(ArmorType.BODY, set.defense[2]);
            ArmorMaterial material = new ArmorMaterial(set.durability, defense, set.enchantability, silent,
                    set.toughness, set.knockbackResistance,
                    net.minecraft.tags.TagKey.create(Registries.ITEM, SinguloArsenal.id("repair/" + set.repairMaterial)),
                    net.minecraft.resources.ResourceKey.create(net.minecraft.world.item.equipment.EquipmentAssets.ROOT_ID, SinguloArsenal.id("powered")));
            var pieces = new EnumMap<ArmorType, DeferredItem<SuitItem>>(ArmorType.class);
            for (ArmorType type : PIECES) {
                pieces.put(type, ITEMS.registerItem(set.id + "_" + type.getName(), properties -> new SuitItem(properties, material, type, set)));
            }
            SUITS.put(set, pieces);
        }
        Nihonium.register(ITEMS, PIECES);
        TABS.register("arsenal", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.singulo_arsenal"))
                .icon(() -> new ItemStack(ModuleContent.INTEGRATOR_ITEM.get()))
                .displayItems((params, output) -> {
                    for (ArmorSet set : ArmorSet.values()) {
                        for (ArmorType type : PIECES) output.accept(charged(SUITS.get(set).get(type).get()));
                    }
                    output.accept(charged(QUENCH_SABER.get()));
                    output.accept(charged(CASIMIR_BLADE.get()));
                    output.accept(charged(TIDAL_REAVER.get()));
                    output.accept(charged(JET_CANNON.get()));
                    output.accept(charged(SQUID_PICK.get()));
                    output.accept(charged(DEGENERACY_BREAKER.get()));
                    output.accept(charged(SHEAR_SAW.get()));
                    output.accept(charged(ACCRETION_DRILL.get()));
                    for (ArmorType type : PIECES) output.accept(Nihonium.ARMOR.get(type).get());
                    output.accept(Nihonium.SWORD.get());
                    output.accept(Nihonium.PICKAXE.get());
                    output.accept(Nihonium.AXE.get());
                    output.accept(Nihonium.SHOVEL.get());
                    output.accept(PELLET_CANISTER.get());
                    output.accept(FLUX_WEAVE.get());
                    output.accept(PRESSURE_LATTICE.get());
                    output.accept(COLLAPSAR_LAMINA.get());
                    output.accept(JET_COLLIMATOR.get());
                    output.accept(TIDAL_EDGE.get());
                    output.accept(ModuleContent.INTEGRATOR_ITEM.get());
                    for (var module : ModuleContent.MODULES.values()) output.accept(module.get());
                }).build());
    }

    private ArsenalItems() {}

    private static ItemStack charged(Item item) {
        ItemStack stack = new ItemStack(item);
        Fe.set(stack, Fe.capacity(stack));
        return stack;
    }

    public static List<Item> powered() {
        List<Item> out = new ArrayList<>();
        for (var entry : ITEMS.getEntries()) {
            if (entry.get() instanceof Powered) out.add(entry.get());
        }
        return out;
    }

    public static void register(IEventBus bus) {

        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(ArsenalItems::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        for (Item item : powered()) {
            event.registerItem(Capabilities.Energy.ITEM, Fe::handler, item);
        }
    }
}
