package io.github.genichimaruo.singuloarsenal.net;

import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;

public record AbilityPayload() {
    public static final AbilityPayload INSTANCE = new AbilityPayload();
    public static void handle(AbilityPayload packet, Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null) SuitEvents.activate(player);
        context.get().setPacketHandled(true);
    }
}
