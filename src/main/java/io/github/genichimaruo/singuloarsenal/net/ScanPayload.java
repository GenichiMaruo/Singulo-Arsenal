package io.github.genichimaruo.singuloarsenal.net;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
/** Server-selected ore positions, sent only to the player who paid for the scan. */
public record ScanPayload(List<BlockPos> ores) {
    private static Consumer<ScanPayload> clientHandler = scan -> {};
    public static void encode(ScanPayload scan, FriendlyByteBuf buffer) {
        if (scan.ores.size() > 1024) throw new IllegalArgumentException("Too many scan positions");
        buffer.writeVarInt(scan.ores.size());
        scan.ores.forEach(buffer::writeBlockPos);
    }
    public static ScanPayload decode(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        if (count < 0 || count > 1024) throw new IllegalArgumentException("Invalid scan position count");
        List<BlockPos> positions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) positions.add(buffer.readBlockPos());
        return new ScanPayload(List.copyOf(positions));
    }
    public static void setClientHandler(Consumer<ScanPayload> handler) { clientHandler = handler; }
    public static void handle(ScanPayload scan, Supplier<NetworkEvent.Context> context) {
        clientHandler.accept(scan);
        context.get().setPacketHandled(true);
    }
}
