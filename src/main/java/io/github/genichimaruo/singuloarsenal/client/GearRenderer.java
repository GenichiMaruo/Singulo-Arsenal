package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.MachineArt;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt.Piece;
import io.github.genichimaruo.singuloarsenal.client.art.WeaponArt;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.weapon.CasimirBladeItem;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import io.github.genichimaruo.singuloarsenal.weapon.QuenchSaberItem;
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders gear as items, including inventory icons, from the same geometry that is worn, so an icon can never disagree with the model. */
public final class GearRenderer extends BlockEntityWithoutLevelRenderer {
    public GearRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        var mc = Minecraft.getInstance();
        float partial = mc.getTimer().getGameTimeDeltaPartialTick(true);
        float t = mc.level == null ? 0 : mc.level.getGameTime() + partial;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (stack.getItem() instanceof SuitItem suit) {
            drawSuit(pose, buffers, t, light, overlay, suit);
        } else if (stack.is(ModuleContent.INTEGRATOR_ITEM.get())) {
            pose.scale(1F / 16, 1F / 16, 1F / 16);
            Canvas.drawFine(pose, buffers, t, light, overlay, context != ItemDisplayContext.GUI, c -> MachineArt.integrator(c, -1, t));
        } else {
            pose.scale(1F / 16, 1F / 16, 1F / 16);
            Player owner = owner(mc, stack, context);
            drawWeapon(pose, buffers, t, light, overlay, stack, owner, partial, context);
        }
        pose.popPose();
    }

    private static Player owner(Minecraft mc, ItemStack stack, ItemDisplayContext context) {
        if (mc.level == null || context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) return null;
        if (context.firstPerson()) return mc.player;
        for (Player player : mc.level.players()) if (player.getMainHandItem() == stack) return player;
        return null;
    }

    /**
     * Swords use vanilla's handheld transforms, which centre a sprite whose blade lies on its diagonal. This centres the blade the
     * same way, and turns the cutting edge to face away from the wielder, which a flat sprite never had to decide.
     */
    private static void seatInHand(PoseStack pose, ItemStack stack) {
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(0, -(stack.getItem() instanceof TidalReaverItem ? 1.5F : stack.getItem() instanceof CasimirBladeItem ? 1.75F : 2.25F), 0);
    }

    private static void drawWeapon(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, ItemStack stack, Player owner, float partial, ItemDisplayContext context) {
        boolean using = owner != null && owner.isUsingItem() && owner.getUseItem().getItem() == stack.getItem();
        int ticks = using ? owner.getTicksUsingItem() : 0;
        boolean world = context != ItemDisplayContext.GUI;
        if (owner != null && !(stack.getItem() instanceof JetLanceItem)) seatInHand(pose, stack);
        long now = Minecraft.getInstance().level == null ? 0 : Minecraft.getInstance().level.getGameTime();
        float reload = world ? Weapons.reloadProgress(stack, now, partial) : -1;
        if (stack.getItem() instanceof QuenchSaberItem) {
            float current = ArsenalData.get(stack, ArsenalData.CURRENT) / (float) (Rules.QUENCH_MAX_CURRENT - 1);
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.quench(c, 0, Math.min(1, current), reload));
        } else if (stack.getItem() instanceof CasimirBladeItem) {
            float use = using ? Math.min(1, (ticks + partial) / Rules.CASIMIR_CLOSE_TICKS) : 0;
            boolean armed = owner != null && CasimirBladeItem.armed(stack, now);
            // Armed mirrors stay shut; once spent they part again over the rest of the cooldown.
            float closing = armed ? 1 : Math.max(use, reload >= 0 ? 1 - reload : 0);
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.casimir(c, closing, armed, t));
        } else if (stack.getItem() instanceof TidalReaverItem) {
            float use = using ? Math.min(1, (ticks + partial) / Rules.REAVER_MAX_HOLD) : 0;
            if (context.firstPerson()) {
                // Drawn back while the focus builds, and snapped through on the shear.
                float strike = reload >= 0 && reload < 0.1F ? 1 - reload / 0.1F : 0;
                pose.mulPose(Axis.XP.rotationDegrees(-22 * use + 34 * strike));
            }
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.reaver(c, use, reload, t));
            if (owner != null && world) FxClient.heldHole(pose, context, -2.5F, 6.75F, 0, (1.4F + use * 0.5F) * (reload >= 0 ? Math.min(1, reload * 1.15F) : 1));
        } else if (stack.getItem() instanceof JetLanceItem) {
            float jet = owner != null && world ? JetLanceItem.jetAge(stack, now, partial) : -1;
            float charge = owner != null && world ? JetLanceItem.chargeProgress(stack, now, partial) : -1;
            float ignition = jet >= 0 ? 1 : Math.max(0, charge);
            boolean leftHanded = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            if (owner != null && world) {
                FxClient.aimLance(owner, pose, context, partial);
                boolean firstPerson = context.firstPerson();
                // Recoil runs straight back along the barrel, so the beam stays on its axis.
                pose.translate(0, -(firstPerson ? 1.6F : 0.8F) * Rules.lanceKick(jet), 0);
                if (reload >= 0 && !firstPerson) {
                    // Drawn in toward the chest while the support hand works on it: at arm's length the magazine is out of reach.
                    pose.translate(0, -9 * Rules.reloadReach(reload), 0);
                }
                if (reload >= 0) {
                    // Muzzle up and rolled so the magazine well faces the support hand.
                    float tilt = Rules.reloadTilt(reload) * (firstPerson ? 1 : 0.55F);
                    pose.mulPose(Axis.XP.rotationDegrees(22 * tilt));
                    pose.mulPose(Axis.YP.rotationDegrees((owner.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 30 : -30) * tilt));
                }
            }
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.lance(c, ignition, reload, t, leftHanded ? 1 : -1));
            if (owner != null && world) FxClient.heldHole(pose, context, 0, 2.25F, 0, 1.5F);
            if (owner != null) FxClient.lanceRendered(owner, pose, buffers, context, partial, jet);
            FirstPersonHands.drawLance(owner, pose, buffers, light, context, reload);
        }
    }

    private static void drawSuit(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, SuitItem suit) {
        ArmorItem.Type type = suit.getType();
        // Worn art is Y-down and faces -Z; flip it upright and toward the viewer.
        pose.mulPose(Axis.XP.rotationDegrees(180));
        float fit = type == ArmorItem.Type.CHESTPLATE ? 0.044F : type == ArmorItem.Type.HELMET ? 0.062F : 0.056F;
        pose.scale(fit, fit, fit);
        SuitArt.State idle = SuitArt.State.IDLE;
        switch (type) {
            case HELMET -> {
                pose.translate(0, 4.8, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> SuitArt.draw(c, suit.set, Piece.HELMET, idle));
            }
            case CHESTPLATE -> {
                pose.translate(0, -4.5, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> {
                    SuitArt.draw(c, suit.set, Piece.TORSO, idle);
                    c.pose.pushPose();
                    c.pose.translate(-7, 2, 0);
                    SuitArt.draw(c, suit.set, Piece.ARM_R, idle);
                    c.pose.translate(14, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.ARM_L, idle);
                    c.pose.popPose();
                });
            }
            case LEGGINGS -> {
                pose.translate(0, -16, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> {
                    SuitArt.draw(c, suit.set, Piece.WAIST, idle);
                    c.pose.pushPose();
                    c.pose.translate(-2.3, 12, 0);
                    SuitArt.draw(c, suit.set, Piece.LEG_R, idle);
                    c.pose.translate(4.6, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.LEG_L, idle);
                    c.pose.popPose();
                });
            }
            default -> {
                pose.translate(0, -10, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> {
                    c.pose.pushPose();
                    c.pose.translate(-3.4, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.BOOT_R, idle);
                    c.pose.translate(6.8, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.BOOT_L, idle);
                    c.pose.popPose();
                });
            }
        }
    }
}
