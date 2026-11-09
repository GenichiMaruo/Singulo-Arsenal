package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Powered;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.net.AbilityPayload;
import io.github.genichimaruo.singuloarsenal.weapon.CasimirBladeItem;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

@Mod(value = SinguloArsenal.MOD_ID, dist = Dist.CLIENT)
public final class ArsenalClient {
    // G and H are taken by Singulo. Rebindable in Controls.
    private static final KeyMapping ABILITY = new KeyMapping("key.singulo_arsenal.ability", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_V, "key.categories.singulo_arsenal");

    public ArsenalClient(IEventBus bus) {
        bus.addListener(ArsenalClient::keys);
        bus.addListener(ArsenalClient::extensions);
        bus.addListener(ArsenalClient::layers);
        bus.addListener(JetTube::registerShaders);
        FxClient.register();
        NeoForge.EVENT_BUS.addListener(ArsenalClient::tick);
        NeoForge.EVENT_BUS.addListener(ArsenalClient::hud);
    }

    private static void keys(RegisterKeyMappingsEvent event) {
        event.register(ABILITY);
    }

    private static void extensions(RegisterClientExtensionsEvent event) {
        IClientItemExtensions gear = new IClientItemExtensions() {
            private GearRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) renderer = new GearRenderer();
                return renderer;
            }
            @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (hand != InteractionHand.MAIN_HAND) return null;
                if (stack.getItem() instanceof JetLanceItem) {
                    boolean reloading = !entity.isUsingItem() && Weapons.reloadProgress(stack, entity.level().getGameTime(), 0) >= 0;
                    return reloading ? LanceReloadPose.pose() : HumanoidModel.ArmPose.CROSSBOW_HOLD;
                }
                if (entity.isUsingItem() && stack.getItem() instanceof TidalReaverItem) return HumanoidModel.ArmPose.THROW_SPEAR;
                if (entity.isUsingItem() && stack.getItem() instanceof CasimirBladeItem) return HumanoidModel.ArmPose.BLOCK;
                return null;
            }
        };
        event.registerItem(gear, ArsenalItems.powered().toArray(new Item[0]));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void layers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new SuitLayer<>(renderer));
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof ArmorStandRenderer renderer) renderer.addLayer(new SuitLayer(renderer));
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        while (ABILITY.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.getConnection() != null) PacketDistributor.sendToServer(AbilityPayload.INSTANCE);
        }
    }

    // ---- HUD -----------------------------------------------------------------------------------------------------

    private static void hud(RenderGuiEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui || mc.player.isSpectator() || mc.screen != null) return;
        GuiGraphics gui = event.getGuiGraphics();
        ItemStack chest = mc.player.getItemBySlot(EquipmentSlot.CHEST);
        int y = 8;
        if (chest.getItem() instanceof SuitItem suit) {
            drawBar(gui, 8, y, Component.translatable("hud.singulo_arsenal." + suit.set.id), chest, 0x7CE8FF);
            y += 14;
            long now = mc.level.getGameTime();
            int load = ArsenalData.get(chest, ArsenalData.LOAD);
            if (suit.set == ArmorSet.DEGENERACY) {
                long locked = ArsenalData.time(chest, ArsenalData.LOCKED_UNTIL) - now;
                Component line = locked > 0
                        ? Component.translatable("hud.singulo_arsenal.collapsed", seconds(locked))
                        : Component.translatable("hud.singulo_arsenal.pressure", load, Rules.PRESSURE_MAX);
                gui.drawString(mc.font, line, 8, y, locked > 0 ? 0xFF8A5A : 0xFFC36B, true);
                y += 12;
            } else if (suit.set == ArmorSet.COLLAPSAR) {
                gui.drawString(mc.font, Component.translatable("hud.singulo_arsenal.phase." + Rules.phaseOf(load), load, Rules.LOAD_MAX), 8, y, 0xFFE2A0, true);
                y += 12;
                long ready = ArsenalData.time(chest, ArsenalData.HORIZON_READY) - now;
                if (ready > 0) {
                    gui.drawString(mc.font, Component.translatable("hud.singulo_arsenal.horizon_cooling", seconds(ready)), 8, y, 0xB8A8FF, true);
                    y += 12;
                }
            }
        }
        ItemStack held = mc.player.getMainHandItem();
        if (held.getItem() instanceof Powered && !(held.getItem() instanceof SuitItem)) {
            drawBar(gui, 8, y, held.getHoverName(), held, 0xFFC36B);
        }
    }

    private static void drawBar(GuiGraphics gui, int x, int y, Component label, ItemStack stack, int color) {
        var font = Minecraft.getInstance().font;
        gui.drawString(font, label, x, y, 0xE9EEF2, true);
        int barX = x + font.width(label) + 6;
        gui.fill(barX, y + 2, barX + 60, y + 6, 0xA0141820);
        int width = (int) (60 * ((double) Fe.stored(stack) / Math.max(1, Fe.capacity(stack))));
        gui.fill(barX, y + 2, barX + width, y + 6, 0xFF000000 | color);
    }

    private static long seconds(long ticks) {
        return Math.max(0, (ticks + 19) / 20);
    }
}
