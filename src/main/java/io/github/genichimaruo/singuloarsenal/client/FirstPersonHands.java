package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Matrix3f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Vanilla hides the arm whenever an item is held in first person, which is right for swords but not for a two-handed gun. These draw it: the real arm model, skin and worn sleeve, reaching
 * from below the screen to a point on the weapon. Positions come from the weapon's own pose, so the hand stays on the grip through
 * swings, recoil and reloads.
 */
final class FirstPersonHands {
    /** From the model's shoulder pivot to the heel of the hand, in blocks. */
    private static final float ARM_LENGTH = 10F / 16;

    private FirstPersonHands() {}

    /** Both hands: the firing hand on the pistol grip, the support hand on the fore grip or wherever the reload has taken it. */
    static void drawCannon(Hands hands, PoseStack art, MultiBufferSource buffers, int light, ItemDisplayContext context, float reload) {
        if (hands == null || !context.firstPerson()) return;
        boolean right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;

        arm(hands, art, buffers, light, right, 0, -0.5F, -6);
        float x = 0, y = 12.5F, z = -4.5F;
        if (reload >= 0) {
            float reach = Rules.reloadReach(reload), cock = Rules.reloadCock(reload);
            // Under the magazine, following it as it comes out.
            y += (7.25F - y) * reach;
            z += (-9.5F - Rules.reloadMagazine(reload) * 6 - z) * reach;
            // Out to the side first and only then along the gun, so the hand travels round the chamber and never through it.
            x += ((right ? -6F : 6F) - x) * (float) Math.sqrt(cock);
            y += (3F - y) * cock;
            z += (1 - z) * cock;
        }
        arm(hands, art, buffers, light, !right, x, y, z);
    }

    record Hands(GeometrySnapshot right, GeometrySnapshot left) {}
    static Hands capture(Player owner) {
        if (!(owner instanceof AbstractClientPlayer player) || player.isInvisible()) return null;
        return new Hands(captureArm(player, true), captureArm(player, false));
    }
    private static GeometrySnapshot captureArm(AbstractClientPlayer player, boolean right) {
        GeometrySnapshot out = new GeometrySnapshot();
        PoseStack pose = new PoseStack();
        pose.pushPose();
        pose.translate((right ? 5F : -5F) / 16, -2F / 16, 0);
        AvatarRenderer<?> renderer = Minecraft.getInstance().getEntityRenderDispatcher().getPlayerRenderer(player);
        var model = renderer.getModel();
        var arm = right ? model.rightArm : model.leftArm;
        var previous = arm.storePose();
        float sx = arm.xScale, sy = arm.yScale, sz = arm.zScale;
        boolean visible = arm.visible, leftSleeve = model.leftSleeve.visible, rightSleeve = model.rightSleeve.visible;
        arm.resetPose(); arm.visible = true;
        model.leftSleeve.visible = player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.LEFT_SLEEVE);
        model.rightSleeve.visible = player.isModelPartShown(net.minecraft.world.entity.player.PlayerModelPart.RIGHT_SLEEVE);
        arm.zRot = right ? 0.1F : -0.1F;
        arm.render(pose, out.getBuffer(net.minecraft.client.renderer.rendertype.RenderTypes.entityTranslucent(player.getSkin().body().texturePath())), -1, Canvas.noOverlay());
        arm.loadPose(previous); arm.xScale = sx; arm.yScale = sy; arm.zScale = sz; arm.visible = visible;
        model.leftSleeve.visible = leftSleeve; model.rightSleeve.visible = rightSleeve;
        pose.popPose();
        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof SuitItem suit) {
            pose.scale(1F / 16, 1F / 16, 1F / 16);
            Canvas.drawMaterial(pose, out, player.level().getGameTime(), -1, Canvas.noOverlay(), SuitArt.material(suit.set), false,
                c -> SuitArt.draw(c, suit.set, right ? SuitArt.Piece.ARM_R : SuitArt.Piece.ARM_L, SuitArt.State.IDLE));
        }
        return out;
    }
    private static void arm(Hands hands, PoseStack art, MultiBufferSource buffers, int light, boolean right, float x, float y, float z) {        // Solve the arm in view space, independent of how the pipeline splits the
        // camera/bob transforms between ModelView and the item pose.
        Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix());
        Vector3f hand = modelView.transformPosition(art.last().pose().transformPosition(x, y, z, new Vector3f()));
        Vector3f screenRight = new Vector3f(1, 0, 0);
        Vector3f screenUp = new Vector3f(0, 1, 0);
        Vector3f look = new Vector3f(0, 0, -1);
        Vector3f shoulder = new Vector3f(screenRight).mul(right ? 0.72F : -0.5F).add(new Vector3f(screenUp).mul(-1.0F)).add(new Vector3f(look).mul(0.05F));
        Vector3f along = new Vector3f(hand).sub(shoulder).normalize();
        // Entity models are drawn upside down and mirrored, so the model's +X is screen-left for both arms.
        Vector3f across = new Vector3f(screenRight).negate();
        across.sub(new Vector3f(along).mul(across.dot(along))).normalize();
        Vector3f depth = new Vector3f(across).cross(along).normalize();

        PoseStack pose = new PoseStack();
        pose.mulPose(modelView.invert());
        Vector3f pivot = new Vector3f(hand).sub(new Vector3f(along).mul(ARM_LENGTH));
        pose.translate(pivot.x, pivot.y, pivot.z);
        pose.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(across, along, depth)));
        // The arm is close to the camera while the weapon is drawn small, so a full-width arm would swamp it.
        pose.scale(0.68F, 1, 0.68F);

        (right ? hands.right : hands.left).replay(pose, buffers, light, Canvas.noOverlay());
    }
}
