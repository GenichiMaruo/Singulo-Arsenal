package io.github.genichimaruo.singuloarsenal.review.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;

/** Opt-in diagnostic only, never part of a normal source set. */
@Mixin(RenderType.class)
abstract class RenderTypeDiagnosticMixin {
    @WrapMethod(method = "draw")
    private void review$identifyFailedDraw(MeshData mesh, Operation<Void> original) {
        try {
            original.call(mesh);
        } catch (RuntimeException | Error failure) {
            System.err.println("ARSENAL_REVIEW_FAILED_RENDER_TYPE " + (Object) this);
            throw failure;
        }
    }
}