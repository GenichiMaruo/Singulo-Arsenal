package io.github.genichimaruo.singuloarsenal.net;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.function.Consumer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** A server-confirmed visual event. Purely cosmetic; the client never learns anything it could act on. */
public record FxPayload(int kind, Vec3 a, Vec3 b, float value, int entity) implements CustomPacketPayload {
    public static final int RIPPLE = 0, COLLAPSE = 1, HORIZON = 2, JET_BURST = 3, QUENCH = 4, SLAM = 5, RUPTURE = 6, SHEAR = 7;
    public static final int STRETCH = 8;
    private static final double VIEW_DISTANCE = 128;

    public static final Type<FxPayload> TYPE = new Type<>(SinguloArsenal.id("fx"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FxPayload> CODEC = StreamCodec.of((buf, fx) -> {
        buf.writeVarInt(fx.kind);
        write(buf, fx.a);
        write(buf, fx.b);
        buf.writeFloat(fx.value);
        buf.writeVarInt(fx.entity);
    }, buf -> new FxPayload(buf.readVarInt(), read(buf), read(buf), buf.readFloat(), buf.readVarInt()));

    private static Consumer<FxPayload> clientHandler = fx -> {};

    public static void setClientHandler(Consumer<FxPayload> handler) { clientHandler = handler; }

    public static void handle(FxPayload fx, IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(fx));
    }

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void send(ServerLevel level, int kind, Vec3 a, Vec3 b, float value) {
        broadcast(level, new FxPayload(kind, a, b, value, -1));
    }

    /** {@code STRETCH}: a positive value is a shear (a violent stretch that snaps back), negative a held stress level. */
    public static void sendEntity(ServerLevel level, int kind, Entity entity, Vec3 at, float value) {
        broadcast(level, new FxPayload(kind, at, Vec3.ZERO, value, entity.getId()));
    }

    private static void broadcast(ServerLevel level, FxPayload fx) {
        Vec3 a = fx.a;
        // GameTest and other fake players have no negotiated channel.
        for (var viewer : level.players()) {
            if (viewer.distanceToSqr(a) <= VIEW_DISTANCE * VIEW_DISTANCE && viewer.connection.hasChannel(TYPE)) {
                PacketDistributor.sendToPlayer(viewer, fx);
            }
        }
    }

    private static void write(RegistryFriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 read(RegistryFriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
