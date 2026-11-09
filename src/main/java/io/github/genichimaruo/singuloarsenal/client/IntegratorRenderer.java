package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.MachineArt;
import io.github.genichimaruo.singuloarsenal.module.IntegratorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** The machine itself, and the gear floating on its bed while the arms work on it. */
public final class IntegratorRenderer implements BlockEntityRenderer<IntegratorBlockEntity> {
    public IntegratorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(IntegratorBlockEntity entity, float partial, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        float t = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partial;
        float work = entity.working() ? entity.progress(partial) : -1;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.pushPose();
        pose.scale(1F / 16, 1F / 16, 1F / 16);
        Canvas.drawFine(pose, buffers, t, light, overlay, true, c -> MachineArt.integrator(c, work, t));
        pose.popPose();
        ItemStack gear = entity.items().getStackInSlot(IntegratorBlockEntity.GEAR);
        if (!gear.isEmpty()) {
            pose.translate(0, 0.28 + 0.03 * Math.sin(t * 0.08), 0);
            pose.mulPose(Axis.YP.rotationDegrees(t * (work >= 0 ? 6 : 1.5F)));
            pose.scale(0.62F, 0.62F, 0.62F);
            Minecraft.getInstance().getItemRenderer().renderStatic(gear, ItemDisplayContext.FIXED, light, overlay, pose, buffers, entity.getLevel(), 0);
        }
        pose.popPose();
    }
}
