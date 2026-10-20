package io.github.genichimaruo.singuloarsenal.network;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorAbilities;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ArmorActionPayload(int action) implements CustomPacketPayload {
    public static final Type<ArmorActionPayload> TYPE = new Type<>(SinguloArsenal.id("armor_action"));
    public static final StreamCodec<ByteBuf, ArmorActionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ArmorActionPayload::action, ArmorActionPayload::new);

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ArmorActionPayload::handle);
    }

    private static void handle(ArmorActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) ArmorAbilities.activate(player, payload.action());
        });
    }
}
