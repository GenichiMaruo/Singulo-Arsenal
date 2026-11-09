package io.github.genichimaruo.singuloarsenal.module;

import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModuleContent {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(SinguloArsenal.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SinguloArsenal.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SinguloArsenal.MOD_ID);

    public static final Map<ModuleType, DeferredItem<ModuleItem>> MODULES = new EnumMap<>(ModuleType.class);
    public static final DeferredBlock<IntegratorBlock> INTEGRATOR = BLOCKS.register("module_integrator", () -> new IntegratorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.5F, 6F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion()));
    public static final DeferredItem<net.minecraft.world.item.BlockItem> INTEGRATOR_ITEM = ArsenalItems.ITEMS.registerSimpleBlockItem(INTEGRATOR);
    @SuppressWarnings("DataFlowIssue")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IntegratorBlockEntity>> INTEGRATOR_ENTITY = BLOCK_ENTITIES.register("module_integrator",
            () -> BlockEntityType.Builder.of(IntegratorBlockEntity::new, INTEGRATOR.get()).build(null));
    public static final DeferredHolder<MenuType<?>, MenuType<IntegratorMenu>> INTEGRATOR_MENU = MENUS.register("module_integrator",
            () -> IMenuTypeExtension.create(IntegratorMenu::new));

    static {
        for (ModuleType type : ModuleType.values()) {
            MODULES.put(type, ArsenalItems.ITEMS.register(type.id, () -> new ModuleItem(type)));
        }
    }

    private ModuleContent() {}

    /** Must run before {@code ArsenalItems.register}, so these items are in the register when it is attached to the bus. */
    public static void register(IEventBus bus) {
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(ModuleContent::capabilities);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, INTEGRATOR_ENTITY.get(), (entity, side) -> entity.energy());
    }
}
