package io.github.genichimaruo.singuloarsenal.net;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ArsenalNetwork {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(SinguloArsenal.id("main"), () -> "1", "1"::equals, "1"::equals);
    private ArsenalNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(AbilityPayload.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((packet, buffer) -> {}).decoder(buffer -> AbilityPayload.INSTANCE)
                .consumerMainThread(AbilityPayload::handle).add();
        CHANNEL.messageBuilder(FxPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(FxPayload::encode).decoder(FxPayload::decode)
                .consumerMainThread(FxPayload::handle).add();
    }
}
