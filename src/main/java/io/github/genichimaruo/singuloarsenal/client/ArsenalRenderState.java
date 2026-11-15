package io.github.genichimaruo.singuloarsenal.client;

import com.google.common.reflect.TypeToken;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;

final class ArsenalRenderState {
    record Data(float time, float reload, boolean pinned, float stretch) {}
    static final ContextKey<Data> DATA = new ContextKey<>(Identifier.fromNamespaceAndPath("singulo_arsenal", "equipment"));
    static void register(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {}, (entity, state) -> {
            long now = entity.level().getGameTime();
            state.setRenderData(DATA, new Data(now + state.partialTick,
                    Weapons.reloadProgress(entity.getMainHandItem(), now, state.partialTick),
                    entity instanceof Player player && SuitEvents.wearing(player, EquipmentSlot.FEET, ArmorSet.MEISSNER) && SuitEvents.pinned(player),
                    FxClient.stretch(entity.getId(), now + state.partialTick)));
        });
    }
}
