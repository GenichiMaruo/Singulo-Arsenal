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
public final class SuitLayer<T extends net.minecraft.client.renderer.entity.state.HumanoidRenderState, M extends HumanoidModel<T>> extends RenderLayer<T, M> {
    public SuitLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack pose, net.minecraft.client.renderer.SubmitNodeCollector collector, int light, T entity, float yaw, float pitch) {
        if (entity.isInvisible) return;

        GeometrySnapshot buffers = new GeometrySnapshot();
        PoseStack geometryPose = new PoseStack();
        geometryPose.mulPose(pose.last().pose());
        pose = geometryPose;
        M model = getParentModel();
        int overlay = LivingEntityRenderer.getOverlayCoords(entity, 0);
        var data = entity.getRenderData(ArsenalRenderState.DATA);
        float t = data == null ? entity.ageInTicks : data.time();
        SuitArt.State state = state(entity);
        for (EquipmentSlot slot : SuitEvents.SLOTS) {
            if (!(equipment(entity, slot).getItem() instanceof SuitItem suit)) continue;
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
        buffers.submit(new PoseStack(), collector);
    }

    private static ItemStack equipment(net.minecraft.client.renderer.entity.state.HumanoidRenderState state, EquipmentSlot slot) {
        return switch (slot) { case HEAD -> state.headEquipment; case CHEST -> state.chestEquipment; case LEGS -> state.legsEquipment; case FEET -> state.feetEquipment; default -> ItemStack.EMPTY; };
    }
    private static SuitArt.State state(net.minecraft.client.renderer.entity.state.HumanoidRenderState entity) {
        ItemStack chest = entity.chestEquipment;
        int load = chest.getItem() instanceof SuitItem suit && suit.set != ArmorSet.MEISSNER ? ArsenalData.get(chest, ArsenalData.LOAD) : 0;
        boolean degeneracy = chest.getItem() instanceof SuitItem suit && suit.set == ArmorSet.DEGENERACY;
        float normal = degeneracy ? load / (float) Rules.PRESSURE_MAX : load / (float) Rules.LOAD_MAX;
        boolean active = entity.getRenderData(ArsenalRenderState.DATA) != null && entity.getRenderData(ArsenalRenderState.DATA).pinned();
        return new SuitArt.State(normal, Rules.phaseOf(load), active);
    }

    private static void part(PoseStack pose, MultiBufferSource buffers, int light, int overlay, float t, ModelPart anchor, ArmorSet set, Piece piece, SuitArt.State state) {
        pose.pushPose();
        anchor.translateAndRotate(pose);
        pose.scale(1F / 16, 1F / 16, 1F / 16);
        // The inventory's paper doll is drawn in GUI space, where there is no camera to turn toward.
        boolean world = !(net.minecraft.client.Minecraft.getInstance().screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen);
        Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(set), world, canvas -> SuitArt.draw(canvas, set, piece, state));
        pose.popPose();
    }
}
