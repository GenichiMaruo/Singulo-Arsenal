package io.github.genichimaruo.singuloarsenal;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.minecraft.resources.ResourceLocation;
import io.github.genichimaruo.singuloarsenal.armor.ArsenalContent;
import io.github.genichimaruo.singuloarsenal.armor.ArmorAbilities;
import io.github.genichimaruo.singuloarsenal.network.ArmorActionPayload;
import io.github.genichimaruo.singuloarsenal.network.RailShotPayload;
import io.github.genichimaruo.singuloarsenal.weapon.WeaponContent;
import org.slf4j.Logger;

/** Singulo の素材と失われた文明の技術を利用した武器・防具アドオン。 */
@Mod(SinguloArsenal.MOD_ID)
public final class SinguloArsenal {
    public static final String MOD_ID = "singulo_arsenal";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SinguloArsenal(IEventBus bus) {
        ArsenalContent.register(bus);
        WeaponContent.register(bus);
        ArmorAbilities.register(bus);
        bus.addListener(ArmorActionPayload::register);
        bus.addListener(RailShotPayload::register);
        LOGGER.info("Singulo Arsenal initialized");
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
