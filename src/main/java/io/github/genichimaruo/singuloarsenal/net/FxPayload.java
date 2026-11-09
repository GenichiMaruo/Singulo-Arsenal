package io.github.genichimaruo.singuloarsenal.net;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

/** A server-confirmed visual event. Purely cosmetic; the client never learns anything it could act on. */
public record FxPayload(int kind, Vec3 a, Vec3 b, float value, int entity) {
    public static final int RIPPLE = 0, COLLAPSE = 1, HORIZON = 2, JET_BURST = 3, QUENCH = 4, SLAM = 5, RUPTURE = 6, SHEAR = 7;
    public static final int STRETCH = 8, BIT_SHOT = 9;
    private static final double VIEW_DISTANCE = 128;

    public static void encode(FxPayload fx, FriendlyByteBuf buf) {
        buf.writeVarInt(fx.kind);
        write(buf, fx.a);
        write(buf, fx.b);
        buf.writeFloat(fx.value);
        buf.writeVarInt(fx.entity);
    }
    public static FxPayload decode(FriendlyByteBuf buf) {
        return new FxPayload(buf.readVarInt(), read(buf), read(buf), buf.readFloat(), buf.readVarInt());
    }

    private static Consumer<FxPayload> clientHandler = fx -> {};

    public static void setClientHandler(Consumer<FxPayload> handler) { clientHandler = handler; }

    public static void handle(FxPayload fx, Supplier<NetworkEvent.Context> context) {
        clientHandler.accept(fx);
        context.get().setPacketHandled(true);
    }


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
            if (viewer.distanceToSqr(a) <= VIEW_DISTANCE * VIEW_DISTANCE && ArsenalNetwork.CHANNEL.isRemotePresent(viewer.connection.connection)) {
                ArsenalNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> viewer), fx);
            }
        }
    }

    private static void write(FriendlyByteBuf buf, Vec3 v) {
        buf.writeDouble(v.x);
        buf.writeDouble(v.y);
        buf.writeDouble(v.z);
    }

    private static Vec3 read(FriendlyByteBuf buf) {
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}
