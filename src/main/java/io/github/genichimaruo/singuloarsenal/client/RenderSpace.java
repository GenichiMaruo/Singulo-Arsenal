package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;

/** Converts camera-relative world coordinates to the current renderer's vertex space.
 * Vanilla hands use ModelView=view rotation and pose=inverse view rotation; Iris
 * hands use ModelView=bob and start with an identity pose. Never infer world
 * axes from an incoming item pose.
 */
public final class RenderSpace {
    private RenderSpace() {}

    public static Matrix4f worldToPose(Matrix4fc modelView, Quaternionfc cameraRotation) {
        return new Matrix4f(modelView).invert()
                .rotate(new Quaternionf(cameraRotation).conjugate());
    }

    static Matrix4f worldToPose() {
        return worldToPose(RenderSystem.getModelViewMatrix(),
                Minecraft.getInstance().gameRenderer.getMainCamera().rotation());
    }
}
