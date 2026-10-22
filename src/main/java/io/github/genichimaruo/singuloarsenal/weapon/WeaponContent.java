package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class WeaponContent {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(SinguloArsenal.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, SinguloArsenal.MOD_ID);
    public static final DeferredItem<RailgunItem> RAILGUN = ITEMS.register("ciel_railgun", RailgunItem::new);
    public static final DeferredHolder<SoundEvent, SoundEvent> CHARGE = sound("railgun_charge");
    public static final DeferredHolder<SoundEvent, SoundEvent> FIRE = sound("railgun_fire");
    public static final DeferredHolder<SoundEvent, SoundEvent> READY = sound("railgun_ready");
    public static final DeferredHolder<SoundEvent, SoundEvent> VENT = sound("railgun_vent");

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String id) {
        return SOUNDS.register(id, () -> SoundEvent.createVariableRangeEvent(SinguloArsenal.id(id)));
    }
    public static void register(IEventBus bus) { ITEMS.register(bus); SOUNDS.register(bus); }
    private WeaponContent() {}
}
