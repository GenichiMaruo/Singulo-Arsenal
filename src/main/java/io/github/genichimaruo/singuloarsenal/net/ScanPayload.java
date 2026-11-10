package io.github.genichimaruo.singuloarsenal.net;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** The ores a SQUID pick found. The server does the looking, and only tells the player who paid for it. */
public record ScanPayload(List<BlockPos> ores) implements CustomPacketPayload {
    public static final Type<ScanPayload> TYPE = new Type<>(SinguloArsenal.id("scan"));
    public static final StreamCodec<ByteBuf, ScanPayload> CODEC = BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list(1024)).map(ScanPayload::new, ScanPayload::ores);

    private static Consumer<ScanPayload> clientHandler = scan -> {};

    public static void setClientHandler(Consumer<ScanPayload> handler) { clientHandler = handler; }

    public static void handle(ScanPayload scan, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(scan));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
