package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt.Piece;
import io.github.genichimaruo.singuloarsenal.client.art.WeaponArt;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.weapon.CasimirBladeItem;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import io.github.genichimaruo.singuloarsenal.weapon.QuenchSaberItem;
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
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

    private static void drawWeapon(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, ItemStack stack, Player owner, float partial, ItemDisplayContext context) {
        boolean using = owner != null && owner.isUsingItem() && owner.getUseItem().getItem() == stack.getItem();
        int ticks = using ? owner.getTicksUsingItem() : 0;
        if (stack.getItem() instanceof QuenchSaberItem) {
            float current = ArsenalData.get(stack, ArsenalData.CURRENT) / (float) (Rules.QUENCH_MAX_CURRENT - 1);
            Canvas.draw(pose, buffers, t, light, overlay, c -> WeaponArt.quench(c, 0, Math.min(1, current)));
        } else if (stack.getItem() instanceof CasimirBladeItem) {
            float use = using ? Math.min(1, (ticks + partial) / Rules.CASIMIR_CLOSE_TICKS) : 0;
            boolean armed = owner != null && CasimirBladeItem.armed(stack, owner.level().getGameTime());
            Canvas.draw(pose, buffers, t, light, overlay, c -> WeaponArt.casimir(c, use, armed, t));
        } else if (stack.getItem() instanceof TidalReaverItem) {
            float use = using ? Math.min(1, (ticks + partial) / Rules.REAVER_MAX_HOLD) : 0;
            Canvas.draw(pose, buffers, t, light, overlay, c -> WeaponArt.reaver(c, use, t));
        } else if (stack.getItem() instanceof JetLanceItem) {
            float ignition = using ? Math.min(1, (ticks + partial) / Rules.LANCE_COLLIMATE) : 0;
            // Carried like a staff. While firing it swings to lie along the jets; the display JSON already tilts it part of the way.
            if (using) pose.mulPose(Axis.XP.rotationDegrees((context.firstPerson() ? -32 : 15) * Math.min(1, (ticks + partial) / 5F)));
            Canvas.draw(pose, buffers, t, light, overlay, c -> WeaponArt.lance(c, ignition, t));
        }
    }

    private static void drawSuit(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, SuitItem suit) {
        ArmorItem.Type type = suit.getType();
        // Worn art is Y-down and faces -Z; flip it upright and toward the viewer.
        pose.mulPose(Axis.XP.rotationDegrees(180));
        float fit = type == ArmorItem.Type.CHESTPLATE ? 0.050F : type == ArmorItem.Type.HELMET ? 0.062F : 0.056F;
        pose.scale(fit, fit, fit);
        SuitArt.State idle = SuitArt.State.IDLE;
        switch (type) {
            case HELMET -> {
                pose.translate(0, 4.8, 0);
                Canvas.draw(pose, buffers, t, light, overlay, c -> SuitArt.draw(c, suit.set, Piece.HELMET, idle));
            }
            case CHESTPLATE -> {
                pose.translate(0, -4.5, 0);
                Canvas.draw(pose, buffers, t, light, overlay, c -> {
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
                Canvas.draw(pose, buffers, t, light, overlay, c -> {
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
                Canvas.draw(pose, buffers, t, light, overlay, c -> {
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
