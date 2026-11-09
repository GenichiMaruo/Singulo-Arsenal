package io.github.genichimaruo.singuloarsenal;

import com.mojang.logging.LogUtils;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.net.AbilityPayload;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/** Equipment for Singulo, each piece built around a physical phenomenon the base mod already makes real. */
@Mod(SinguloArsenal.MOD_ID)
public final class SinguloArsenal {
    public static final String MOD_ID = "singulo_arsenal";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SinguloArsenal() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModuleContent.register(bus);
        ArsenalItems.register(bus);
        SuitEvents.register();
        Weapons.register();
        io.github.genichimaruo.singuloarsenal.net.ArsenalNetwork.register();
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            io.github.genichimaruo.singuloarsenal.client.ArsenalClient.register();
        }
        LOGGER.info("Singulo Arsenal initialized");
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
