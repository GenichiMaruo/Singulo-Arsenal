package io.github.genichimaruo.singuloarsenal;

import com.mojang.logging.LogUtils;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.net.AbilityPayload;
import io.github.genichimaruo.singuloarsenal.net.FirePayload;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import io.github.genichimaruo.singuloarsenal.net.ScanPayload;
import io.github.genichimaruo.singuloarsenal.tool.Mining;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.resources.Identifier;
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
        if (net.neoforged.neoforge.gametest.GameTestHooks.isGametestEnabled()) {
            try {
                Class.forName("io.github.genichimaruo.singuloarsenal.gametest.ArsenalTestRegistration")
                    .getMethod("register", IEventBus.class).invoke(null, bus);
            } catch (ReflectiveOperationException e) { throw new IllegalStateException("Cannot register Arsenal tests", e); }
        }
        ArsenalData.REGISTER.register(bus);
        ModuleContent.register(bus);
        ArsenalItems.register(bus);
        SuitEvents.register();
        Weapons.register();
        Mining.register();
        io.github.genichimaruo.singuloarsenal.tool.SquidPickItem.register();
        bus.addListener(SinguloArsenal::payloads);
        LOGGER.info("Singulo Arsenal initialized");
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToServer(AbilityPayload.TYPE, AbilityPayload.CODEC, AbilityPayload::handle);
        registrar.playToServer(FirePayload.TYPE, FirePayload.CODEC, FirePayload::handle);
        registrar.playToClient(FxPayload.TYPE, FxPayload.CODEC, FxPayload::handle);
        registrar.playToClient(ScanPayload.TYPE, ScanPayload.CODEC, ScanPayload::handle);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
