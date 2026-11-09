package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.armor.SuitEvents;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt.Piece;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** Draws the suits on players and armor stands, one piece per body part, so they follow every animation of the model. */
public final class SuitLayer<T extends LivingEntity, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public SuitLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;
        M model = getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0);
        float t = entity.level().getGameTime() + partial;
        SuitArt.State state = state(entity);
        for (EquipmentSlot slot : SuitEvents.SLOTS) {
            if (!(entity.getItemBySlot(slot).getItem() instanceof SuitItem suit)) continue;
            ArmorSet set = suit.set;
            switch (slot) {
                case HEAD -> part(pose, buffers, light, overlay, t, model.head, set, Piece.HELMET, state);
                case CHEST -> {
                    part(pose, buffers, light, overlay, t, model.body, set, Piece.TORSO, state);
                    part(pose, buffers, light, overlay, t, model.rightArm, set, Piece.ARM_R, state);
                    part(pose, buffers, light, overlay, t, model.leftArm, set, Piece.ARM_L, state);
                }
                case LEGS -> {
                    part(pose, buffers, light, overlay, t, model.body, set, Piece.WAIST, state);
                    part(pose, buffers, light, overlay, t, model.rightLeg, set, Piece.LEG_R, state);
                    part(pose, buffers, light, overlay, t, model.leftLeg, set, Piece.LEG_L, state);
                }
                default -> {
                    part(pose, buffers, light, overlay, t, model.rightLeg, set, Piece.BOOT_R, state);
                    part(pose, buffers, light, overlay, t, model.leftLeg, set, Piece.BOOT_L, state);
                }
            }
        }
    }

    private static SuitArt.State state(LivingEntity entity) {
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        int load = chest.getItem() instanceof SuitItem suit && suit.set != ArmorSet.MEISSNER ? ArsenalData.get(chest, ArsenalData.LOAD) : 0;
        boolean degeneracy = chest.getItem() instanceof SuitItem suit && suit.set == ArmorSet.DEGENERACY;
        float normal = degeneracy ? load / (float) Rules.PRESSURE_MAX : load / (float) Rules.LOAD_MAX;
        boolean active = entity instanceof Player player && SuitEvents.wearing(player, EquipmentSlot.FEET, ArmorSet.MEISSNER) && SuitEvents.pinned(player);
        return new SuitArt.State(normal, Rules.phaseOf(load), active);
    }

    private static void part(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float t, ModelPart anchor, ArmorSet set, Piece piece, SuitArt.State state) {
        pose.pushPose();
        anchor.translateAndRotate(pose);
        pose.scale(1F / 16, 1F / 16, 1F / 16);
        Canvas.draw(pose, buffers, t, light, overlay, canvas -> SuitArt.draw(canvas, set, piece, state));
        pose.popPose();
    }
}
