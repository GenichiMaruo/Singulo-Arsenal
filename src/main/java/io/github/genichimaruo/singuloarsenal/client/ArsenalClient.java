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
import io.github.genichimaruo.singuloarsenal.core.Tips;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.net.AbilityPayload;
import io.github.genichimaruo.singuloarsenal.net.FirePayload;
import io.github.genichimaruo.singuloarsenal.tool.AccretionDrillItem;
import io.github.genichimaruo.singuloarsenal.weapon.CasimirBladeItem;
import io.github.genichimaruo.singuloarsenal.weapon.JetCannonItem;
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
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
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

    private static boolean triggerPulled, triggerHeld;
    private static int triggerWait;

    public ArsenalClient(IEventBus bus) {
        bus.addListener(ArsenalClient::keys);
        bus.addListener(ArsenalClient::extensions);
        bus.addListener(ArsenalClient::layers);
        bus.addListener(JetTube::registerShaders);
        bus.addListener(ArsenalClient::machineRenderers);
        bus.addListener(ArsenalClient::screens);
        FxClient.register();
        Tips.detail = net.minecraft.client.gui.screens.Screen::hasShiftDown;
        NeoForge.EVENT_BUS.addListener(ArsenalClient::tick);
        NeoForge.EVENT_BUS.addListener(ArsenalClient::hud);
        NeoForge.EVENT_BUS.addListener(ArsenalClient::trigger);
        NeoForge.EVENT_BUS.addListener(ArsenalClient::hand);
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
                if (stack.getItem() instanceof JetCannonItem) {
                    boolean reloading = Weapons.reloadProgress(stack, entity.level().getGameTime(), 0) >= 0;
                    return reloading ? CannonReloadPose.pose() : HumanoidModel.ArmPose.CROSSBOW_HOLD;
                }
                if (stack.getItem() instanceof TidalReaverItem && TidalReaverItem.focusProgress(stack, entity.level().getGameTime(), 0) >= 0) return HumanoidModel.ArmPose.THROW_SPEAR;
                if (entity.isUsingItem() && stack.getItem() instanceof CasimirBladeItem) return HumanoidModel.ArmPose.BLOCK;
                return null;
            }
        };
        event.registerItem(gear, ArsenalItems.powered().toArray(new Item[0]));
        event.registerItem(gear, ModuleContent.INTEGRATOR_ITEM.get());
    }

    private static void machineRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModuleContent.INTEGRATOR_ENTITY.get(), IntegratorRenderer::new);
    }

    private static void screens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
        event.register(ModuleContent.INTEGRATOR_MENU.get(), IntegratorScreen::new);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void layers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            if (event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new SuitLayer<>(renderer));
        }
        if (event.getRenderer(EntityType.ARMOR_STAND) instanceof ArmorStandRenderer renderer) renderer.addLayer(new SuitLayer(renderer));
    }

    /** A main-hand cannon already draws its support arm. Keep the other held item out
     * of that first-person pass, without changing inventory or gameplay. */
    private static void hand(RenderHandEvent event) {
        if (event.getHand() == InteractionHand.OFF_HAND && holdingCannon(Minecraft.getInstance())) {
            event.setCanceled(true);
        }
    }

    private static boolean holdingCannon(Minecraft mc) {
        return mc.player != null && mc.player.getMainHandItem().getItem() instanceof JetCannonItem;
    }

    /** With the cannon in hand, left click is the trigger: it neither swings, breaks blocks nor hits at arm's length. */
    private static void trigger(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isAttack() || !holdingCannon(Minecraft.getInstance())) return;
        event.setCanceled(true);
        event.setSwingHand(false);
        // Vanilla repeats this event every tick while the button is held on a block.
        if (!triggerHeld) triggerPulled = true;
    }

    private static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        while (ABILITY.consumeClick()) {
            if (mc.player != null && mc.screen == null && mc.getConnection() != null) PacketDistributor.sendToServer(AbilityPayload.INSTANCE);
        }
        ActiveSounds.tick(mc);
        // One click, one shot: holding the button down does not fire again.
        boolean down = mc.options.keyAttack.isDown();
        boolean wanted = triggerPulled || (down && !triggerHeld);
        triggerPulled = false;
        triggerHeld = down;
        if (triggerWait > 0) triggerWait--;
        if (wanted && triggerWait == 0 && mc.screen == null && holdingCannon(mc) && mc.level != null && mc.getConnection() != null
                && JetCannonItem.ready(mc.player.getMainHandItem(), mc.level.getGameTime())) {
            PacketDistributor.sendToServer(FirePayload.INSTANCE);
            // Long enough for the server's answer to reach this client, so one click is one packet.
            triggerWait = 4;
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
            y += 14;
            if (held.getItem() instanceof AccretionDrillItem) {
                gui.drawString(mc.font, Component.translatable("tool.singulo_arsenal.drill_mode." + AccretionDrillItem.mode(held)), 8, y, 0xFFE2A0, true);
            }
            if (held.getItem() instanceof JetCannonItem) {
                Component mode = Component.translatable(JetCannonItem.breaksTerrain(held) ? "hud.singulo_arsenal.terrain_break" : "hud.singulo_arsenal.terrain_protect");
                gui.drawString(mc.font, Component.translatable("hud.singulo_arsenal.magazine", JetCannonItem.shotsLeft(held), Rules.CANNON_MAGAZINE, mode), 8, y, 0xFFE2A0, true);
            }
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
