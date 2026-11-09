package io.github.genichimaruo.singuloarsenal;

import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.weapon.CasimirBladeItem;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ArsenalItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, SinguloArsenal.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SinguloArsenal.MOD_ID);
    public static final List<ArmorItem.Type> PIECES = List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS);
    public static final Map<ArmorSet, Map<ArmorItem.Type, RegistryObject<SuitItem>>> SUITS = new EnumMap<>(ArmorSet.class);

    public static final RegistryObject<Item> FLUX_WEAVE = ITEMS.register("flux_weave", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PRESSURE_LATTICE = ITEMS.register("pressure_lattice", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> COLLAPSAR_LAMINA = ITEMS.register("collapsar_lamina", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> JET_COLLIMATOR = ITEMS.register("jet_collimator", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TIDAL_EDGE = ITEMS.register("tidal_edge", () -> new Item(new Item.Properties()));

    public static final RegistryObject<QuenchSaberItem> QUENCH_SABER = ITEMS.register("quench_saber", QuenchSaberItem::new);
    public static final RegistryObject<CasimirBladeItem> CASIMIR_BLADE = ITEMS.register("casimir_blade", CasimirBladeItem::new);
    public static final RegistryObject<TidalReaverItem> TIDAL_REAVER = ITEMS.register("tidal_reaver", TidalReaverItem::new);
    public static final RegistryObject<JetLanceItem> JET_LANCE = ITEMS.register("jet_lance", JetLanceItem::new);

    static {
        // The suit is drawn by SuitLayer, so the material has no texture layers. Its sound is silent because vanilla
        // replays the equip sound whenever a worn stack's components change, which here is every FE payment.
        for (ArmorSet set : ArmorSet.values()) {
            ArmorMaterial material = new ArsenalArmorMaterial(set);
            var pieces = new EnumMap<ArmorItem.Type, RegistryObject<SuitItem>>(ArmorItem.Type.class);
            for (ArmorItem.Type type : PIECES) {
                pieces.put(type, ITEMS.register(set.id + "_" + type.getName(), () -> new SuitItem(material, type, set)));
            }
            SUITS.put(set, pieces);
        }
        TABS.register("arsenal", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.singulo_arsenal"))
                .icon(() -> new ItemStack(JET_LANCE.get()))
                .displayItems((params, output) -> {
                    for (ArmorSet set : ArmorSet.values()) {
                        for (ArmorItem.Type type : PIECES) output.accept(charged(SUITS.get(set).get(type).get()));
                    }
                    output.accept(charged(QUENCH_SABER.get()));
                    output.accept(charged(CASIMIR_BLADE.get()));
                    output.accept(charged(TIDAL_REAVER.get()));
                    output.accept(charged(JET_LANCE.get()));
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
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addGenericListener(ItemStack.class, ArsenalItems::capabilities);
    }

    private static void capabilities(net.minecraftforge.event.AttachCapabilitiesEvent<ItemStack> event) {
        ItemStack stack = event.getObject();
        if (stack.getItem() instanceof Powered) event.addCapability(SinguloArsenal.id("energy"), new io.github.genichimaruo.singuloarsenal.core.GearEnergy(stack));
    }
}
