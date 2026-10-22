package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorAbilities;
import io.github.genichimaruo.singuloarsenal.armor.ArmorData;
import io.github.genichimaruo.singuloarsenal.armor.ArmorTier;
import io.github.genichimaruo.singuloarsenal.armor.ArsenalArmorItem;
import io.github.genichimaruo.singuloarsenal.network.ArmorActionPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = SinguloArsenal.MOD_ID, dist = Dist.CLIENT)
public final class ArsenalClient {
    // G/H are already used by Singulo. All three keys can be rebound in Controls.
    private static final KeyMapping[] KEYS = {
            key("vision", GLFW.GLFW_KEY_V), key("dodge", GLFW.GLFW_KEY_Z), key("barrier", GLFW.GLFW_KEY_B)
    };

    public ArsenalClient(IEventBus bus) {
        io.github.genichimaruo.singuloarsenal.client.railgun.RailgunClient.register(bus);
        bus.addListener(ArsenalClient::keys);
        NeoForge.EVENT_BUS.addListener(ArsenalClient::tick);
        NeoForge.EVENT_BUS.addListener(ArsenalClient::hud);
    }

    private static KeyMapping key(String name, int key) {
        return new KeyMapping("key.singulo_arsenal." + name, InputConstants.Type.KEYSYM, key, "key.categories.singulo_arsenal");
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        for (var key : KEYS) event.register(key);
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        for (int action = 0; action < KEYS.length; action++) {
            while (KEYS[action].consumeClick()) {
                if (mc.player != null && mc.screen == null && mc.getConnection() != null) {
                    PacketDistributor.sendToServer(new ArmorActionPayload(action));
                }
            }
        }
    }

    private static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        var player = mc.player;
        if (player == null || mc.options.hideGui || player.isSpectator()) return;
        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ArsenalArmorItem armor)) return;
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        Component line = null;
        long now = player.level().getGameTime();
        if (armor.tier == ArmorTier.CATALYTIC && ArmorAbilities.wearing(player, EquipmentSlot.CHEST, ArmorTier.CATALYTIC)) {
            line = Component.translatable("hud.singulo_arsenal.repair", ArmorData.get(chest, ArmorData.REPAIR));
        } else if (armor.tier == ArmorTier.INERTIAL) {
            ItemStack reservoir = ArmorAbilities.wearing(player, EquipmentSlot.CHEST, ArmorTier.INERTIAL) ? chest
                    : player.getItemBySlot(EquipmentSlot.LEGS);
            if (reservoir.getItem() instanceof ArsenalArmorItem piece && piece.tier == ArmorTier.INERTIAL) {
                line = ArmorData.time(reservoir, ArmorData.OVERLOADED_UNTIL) > now
                        ? Component.translatable("hud.singulo_arsenal.overload", seconds(ArmorData.time(reservoir, ArmorData.OVERLOADED_UNTIL) - now))
                        : Component.translatable("hud.singulo_arsenal.impact", ArmorData.get(reservoir, ArmorData.IMPACT),
                                ArsenalArmorItem.energy(player.getItemBySlot(EquipmentSlot.LEGS)).getEnergyStored(),
                                ArsenalArmorItem.energy(chest).getEnergyStored());
            }
        } else if (armor.tier == ArmorTier.BOUNDARY && ArmorAbilities.wearing(player, EquipmentSlot.CHEST, ArmorTier.BOUNDARY)) {
            line = ArmorData.active(chest, now)
                    ? Component.translatable("hud.singulo_arsenal.shield", ArmorData.get(chest, ArmorData.SHIELD), seconds(ArmorData.time(chest, ArmorData.ACTIVE_UNTIL) - now))
                    : Component.translatable("hud.singulo_arsenal.boundary", ArsenalArmorItem.energy(chest).getEnergyStored(), seconds(ArmorData.time(chest, ArmorData.READY_AT) - now));
        }
        if (line != null) event.getGuiGraphics().drawString(mc.font, line, 8, 8, 0xA4E9ED, true);
    }

    private static long seconds(long ticks) { return Math.max(0, (ticks + 19) / 20); }
}
