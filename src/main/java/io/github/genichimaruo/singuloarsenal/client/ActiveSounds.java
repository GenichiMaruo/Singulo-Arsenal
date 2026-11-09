package io.github.genichimaruo.singuloarsenal.client;

import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Sounds that belong to something on screen and must end the instant it does: a server-played sample would run to its end after the jet
 * or the focus has gone. Each is a client instance that stops itself on the tick its source disappears, for every player in view.
 */
final class ActiveSounds {
    private record Key(int entity, int kind) {}

    private static final int CHARGE = 0, JET = 1, FOCUS = 2, CHARGE_BODY = 3;
    private static final Map<Key, Live> ACTIVE = new HashMap<>();

    private ActiveSounds() {}

    private static final class Live extends AbstractTickableSoundInstance {
        private final BooleanSupplier alive;
        private final Supplier<Vec3> position;

        Live(SoundEvent event, boolean looping, float volume, float pitch, BooleanSupplier alive, Supplier<Vec3> position) {
            super(event, SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
            this.looping = looping;
            this.delay = 0;
            this.volume = volume;
            this.pitch = pitch;
            this.alive = alive;
            this.position = position;
            follow();
        }

        private void follow() {
            Vec3 at = position.get();
            x = at.x;
            y = at.y;
            z = at.z;
        }

        @Override
        public void tick() {
            if (alive.getAsBoolean()) follow();
            else stop();
        }

        @Override public boolean canStartSilent() { return true; }
    }

    static void tick(Minecraft mc) {
        ACTIVE.values().removeIf(Live::isStopped);
        if (mc.level == null) {
            ACTIVE.clear();
            return;
        }
        for (Player player : mc.level.players()) {
            int id = player.getId();
            ensure(mc, new Key(id, CHARGE), "horizon_warden.beam_charge", false, 2.5F, Rules.LANCE_CHARGE_PITCH, () -> charging(player), player::getEyePosition);
            ensure(mc, new Key(id, CHARGE_BODY), "jet_forge.spool", false, 3F, Rules.LANCE_CHARGE_BODY_PITCH, () -> charging(player), player::getEyePosition);
            ensure(mc, new Key(id, JET), "jet_forge.jet", true, 3F, 1, () -> burning(player), () -> nearestOnJet(mc, player));
            ensure(mc, new Key(id, FOCUS), "tidal_spindle_running", true, 1.2F, 0.8F, () -> focusing(player), () -> TidalReaverItem.focus(player, 1));
        }
    }

    private static void ensure(Minecraft mc, Key key, String sound, boolean looping, float volume, float pitch, BooleanSupplier alive, Supplier<Vec3> position) {
        if (ACTIVE.containsKey(key) || !alive.getAsBoolean()) return;
        SoundEvent event = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.fromNamespaceAndPath("singulo", sound));
        if (event == null) return;
        Live live = new Live(event, looping, volume, pitch, alive, position);
        ACTIVE.put(key, live);
        mc.getSoundManager().play(live);
    }

    private static boolean charging(Player player) {
        return player.isAlive() && JetLanceItem.chargeProgress(player.getMainHandItem(), player.level().getGameTime(), 0) >= 0;
    }

    private static boolean burning(Player player) {
        return player.isAlive() && JetLanceItem.jetAge(player.getMainHandItem(), player.level().getGameTime(), 0) >= 0;
    }

    private static boolean focusing(Player player) {
        return player.isAlive() && TidalReaverItem.focusProgress(player.getMainHandItem(), player.level().getGameTime(), 0) >= 0;
    }

    /** The jet is a long source: it is heard from the point along it closest to the listener, as the Jet Forge's is. */
    private static Vec3 nearestOnJet(Minecraft mc, Player player) {
        Vec3 from = player.getEyePosition();
        Vec3 axis = FxClient.aim(player, 1).getLocation().subtract(from);
        double length = axis.length();
        if (length < 1.0E-3) return from;
        Vec3 listener = mc.gameRenderer.getMainCamera().getPosition();
        double along = Math.max(0, Math.min(length, listener.subtract(from).dot(axis) / length));
        return from.add(axis.scale(along / length));
    }
}
