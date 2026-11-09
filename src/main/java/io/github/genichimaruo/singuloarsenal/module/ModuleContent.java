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
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;

public final class ModuleContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, SinguloArsenal.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, SinguloArsenal.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, SinguloArsenal.MOD_ID);

    public static final Map<ModuleType, RegistryObject<ModuleItem>> MODULES = new EnumMap<>(ModuleType.class);
    public static final RegistryObject<IntegratorBlock> INTEGRATOR = BLOCKS.register("module_integrator", () -> new IntegratorBlock(
            BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(3.5F, 6F).sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion()));
    public static final RegistryObject<net.minecraft.world.item.BlockItem> INTEGRATOR_ITEM = ArsenalItems.ITEMS.register("module_integrator", () -> new IntegratorItem(INTEGRATOR.get()));
    @SuppressWarnings("DataFlowIssue")
    public static final RegistryObject<BlockEntityType<IntegratorBlockEntity>> INTEGRATOR_ENTITY = BLOCK_ENTITIES.register("module_integrator",
            () -> BlockEntityType.Builder.of(IntegratorBlockEntity::new, INTEGRATOR.get()).build(null));
    public static final RegistryObject<MenuType<IntegratorMenu>> INTEGRATOR_MENU = MENUS.register("module_integrator",
            () -> IForgeMenuType.create(IntegratorMenu::new));

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
    }

}
