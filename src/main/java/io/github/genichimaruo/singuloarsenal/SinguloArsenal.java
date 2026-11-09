package io.github.genichimaruo.singuloarsenal;

import com.mojang.logging.LogUtils;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.net.AbilityPayload;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;

/** Equipment for Singulo, each piece built around a physical phenomenon the base mod already makes real. */
@Mod(SinguloArsenal.MOD_ID)
public final class SinguloArsenal {
    public static final String MOD_ID = "singulo_arsenal";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SinguloArsenal(IEventBus bus) {
        ArsenalData.REGISTER.register(bus);
        ModuleContent.register(bus);
        ArsenalItems.register(bus);
        SuitEvents.register();
        Weapons.register();
        bus.addListener(SinguloArsenal::payloads);
        LOGGER.info("Singulo Arsenal initialized");
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("2");
        registrar.playToServer(AbilityPayload.TYPE, AbilityPayload.CODEC, AbilityPayload::handle);
        registrar.playToClient(FxPayload.TYPE, FxPayload.CODEC, FxPayload::handle);
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
