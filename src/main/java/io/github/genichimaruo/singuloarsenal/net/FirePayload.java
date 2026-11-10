package io.github.genichimaruo.singuloarsenal.net;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
/** The server decides whether the held cannon may fire. */
public record FirePayload() {
    public static final FirePayload INSTANCE = new FirePayload();
    public static void handle(FirePayload packet, Supplier<NetworkEvent.Context> context) {
        var player = context.get().getSender();
        if (player != null) JetLanceItem.tryFire(player);
        context.get().setPacketHandled(true);
    }
}
