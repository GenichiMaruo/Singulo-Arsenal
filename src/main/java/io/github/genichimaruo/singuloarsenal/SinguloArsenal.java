package io.github.genichimaruo.singuloarsenal;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Singulo の素材と失われた文明の技術を利用した武器・防具アドオン。 */
@Mod(SinguloArsenal.MOD_ID)
public final class SinguloArsenal {
    public static final String MOD_ID = "singulo_arsenal";
    private static final Logger LOGGER = LogUtils.getLogger();

    public SinguloArsenal() {
        LOGGER.info("Singulo Arsenal initialized");
    }
}
