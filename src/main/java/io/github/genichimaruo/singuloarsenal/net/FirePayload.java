package io.github.genichimaruo.singuloarsenal.net;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** "The trigger was pulled." Carries nothing a client could lie about. */
public record FirePayload() implements CustomPacketPayload {
    public static final FirePayload INSTANCE = new FirePayload();
    public static final Type<FirePayload> TYPE = new Type<>(SinguloArsenal.id("fire"));
    public static final StreamCodec<ByteBuf, FirePayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(FirePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) JetLanceItem.tryFire(player);
        });
    }
}
