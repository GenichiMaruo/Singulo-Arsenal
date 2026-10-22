package io.github.genichimaruo.singuloarsenal.network;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.phys.Vec3;

/** Server-confirmed shot geometry. Registration/handling live on separate physical sides. */
public record RailShotPayload(int shooter, Vec3 from, Vec3 to, Direction normal, boolean hitBlock, int hits) implements CustomPacketPayload {
    public static void register(net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, RailShotPayload::handle);
    }
    private static java.util.function.Consumer<RailShotPayload> clientHandler = shot -> {};
    public static void setClientHandler(java.util.function.Consumer<RailShotPayload> handler) { clientHandler = handler; }
    public static void handle(RailShotPayload shot, net.neoforged.neoforge.network.handling.IPayloadContext context) {
        context.enqueueWork(() -> clientHandler.accept(shot));
    }
    public static final Type<RailShotPayload> TYPE = new Type<>(SinguloArsenal.id("rail_shot"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RailShotPayload> CODEC = StreamCodec.of((buf, shot) -> {
        buf.writeInt(shot.shooter());
        write(buf, shot.from()); write(buf, shot.to());
        buf.writeEnum(shot.normal()); buf.writeBoolean(shot.hitBlock()); buf.writeVarInt(shot.hits());
    }, buf -> new RailShotPayload(buf.readInt(), read(buf), read(buf), buf.readEnum(Direction.class), buf.readBoolean(), buf.readVarInt()));
    private static void write(RegistryFriendlyByteBuf buf, Vec3 v) { buf.writeDouble(v.x); buf.writeDouble(v.y); buf.writeDouble(v.z); }
    private static Vec3 read(RegistryFriendlyByteBuf buf) { return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble()); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
