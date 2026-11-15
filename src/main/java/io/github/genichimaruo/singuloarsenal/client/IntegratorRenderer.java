package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.MachineArt;
import io.github.genichimaruo.singuloarsenal.module.IntegratorBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;

/** Extracts the machine and floating gear before deferred rendering. */
public final class IntegratorRenderer implements BlockEntityRenderer<IntegratorBlockEntity, IntegratorRenderer.State> {
    private final ItemModelResolver items;
    public IntegratorRenderer(BlockEntityRendererProvider.Context context) { items = context.itemModelResolver(); }
    public static final class State extends BlockEntityRenderState {
        float time, work;
        GeometrySnapshot machine;
        final ItemStackRenderState gear = new ItemStackRenderState();
    }
    @Override public State createRenderState() { return new State(); }
    @Override public void extractRenderState(IntegratorBlockEntity entity, State state, float partial, Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(entity, state, partial, camera, breaking);
        state.time = (entity.getLevel() == null ? 0 : entity.getLevel().getGameTime()) + partial;
        state.work = entity.working() ? entity.progress(partial) : -1;
        state.machine = new GeometrySnapshot();
        PoseStack pose = new PoseStack();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(1F / 16, 1F / 16, 1F / 16);
        Canvas.drawFine(pose, state.machine, state.time, state.lightCoords, Canvas.noOverlay(), true, c -> MachineArt.integrator(c, state.work, state.time));
        items.updateForTopItem(state.gear, entity.items().getStackInSlot(IntegratorBlockEntity.GEAR), ItemDisplayContext.FIXED, entity.getLevel(), null, 0);
    }
    @Override public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        state.machine.submit(pose, collector);
        pose.pushPose();
        pose.translate(0.5, 0.78 + 0.03 * Math.sin(state.time * 0.08), 0.5);
        pose.mulPose(Axis.YP.rotationDegrees(state.time * (state.work >= 0 ? 6 : 1.5F)));
        pose.scale(0.62F, 0.62F, 0.62F);
        state.gear.submit(pose, collector, state.lightCoords, Canvas.noOverlay(), 0);
        pose.popPose();
    }
}
