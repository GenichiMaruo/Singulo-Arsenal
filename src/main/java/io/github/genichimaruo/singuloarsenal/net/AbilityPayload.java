package io.github.genichimaruo.singuloarsenal.net;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** "The ability key was pressed." Carries nothing a client could lie about. */
public record AbilityPayload() implements CustomPacketPayload {
    public static final AbilityPayload INSTANCE = new AbilityPayload();
    public static final Type<AbilityPayload> TYPE = new Type<>(SinguloArsenal.id("ability"));
    public static final StreamCodec<ByteBuf, AbilityPayload> CODEC = StreamCodec.unit(INSTANCE);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(AbilityPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) SuitEvents.activate(player);
        });
    }
}
