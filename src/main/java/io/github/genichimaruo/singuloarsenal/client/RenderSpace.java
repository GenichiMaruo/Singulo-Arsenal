package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

/** Converts camera-relative world coordinates to the current renderer's vertex space.
 * In 1.20.1 Camera.rotation() is +Z-forward, not the 1.21 camera convention.
 * GameRenderer publishes the actual inverse view rotation after Forge camera
 * angles (including roll); use it rather than conjugating the legacy camera.
 */
public final class RenderSpace {
    private RenderSpace() {}

    public static Matrix4f worldToPose(Matrix4fc modelView, Matrix3fc inverseViewRotation) {
        return new Matrix4f(modelView).invert()
                .mul(new Matrix4f().set(new Matrix3f(inverseViewRotation).invert()));
    }

    static void mulPose(com.mojang.blaze3d.vertex.PoseStack pose, Matrix4f matrix) {
        pose.mulPoseMatrix(matrix);
        pose.last().normal().mul(new org.joml.Matrix3f(matrix).invert().transpose());
    }

    static Matrix4f worldToPose() {
        return worldToPose(RenderSystem.getModelViewMatrix(),
                RenderSystem.getInverseViewRotationMatrix());
    }
}
