package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
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
    static void drawLance(Player owner, PoseStack art, MultiBufferSource buffers, int light, ItemDisplayContext context, float reload) {
        if (!visible(owner, context)) return;
        boolean right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND;
        AbstractClientPlayer player = (AbstractClientPlayer) owner;
        arm(player, art, buffers, light, right, 0, -0.5F, -6);
        float x = 0, y = 12.5F, z = -4.5F;
        if (reload >= 0) {
            float reach = Rules.reloadReach(reload), cock = Rules.reloadCock(reload);
            y += (7.25F - y) * reach;
            z += (-9 - z) * reach;
            x += (4.5F - x) * cock;
            y += (3F - y) * cock;
            z += (1 - z) * cock;
        }
        arm(player, art, buffers, light, !right, x, y, z);
    }

    private static boolean visible(Player owner, ItemDisplayContext context) {
        return context.firstPerson() && owner instanceof AbstractClientPlayer && !owner.isInvisible();
    }

    private static void arm(AbstractClientPlayer player, PoseStack art, MultiBufferSource buffers, int light, boolean right, float x, float y, float z) {
        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        // The item pose in first person has world axes centred on the camera.
        Vector3f hand = art.last().pose().transformPosition(x, y, z, new Vector3f());
        Vector3f screenRight = new Vector3f(camera.getLeftVector()).negate();
        Vector3f screenUp = new Vector3f(camera.getUpVector());
        Vector3f look = new Vector3f(camera.getLookVector());
        Vector3f shoulder = new Vector3f(screenRight).mul(right ? 0.72F : -0.5F).add(new Vector3f(screenUp).mul(-1.0F)).add(new Vector3f(look).mul(0.05F));
        Vector3f along = new Vector3f(hand).sub(shoulder).normalize();
        // Entity models are drawn upside down and mirrored, so the model's +X is screen-left for both arms.
        Vector3f across = new Vector3f(screenRight).negate();
        across.sub(new Vector3f(along).mul(across.dot(along))).normalize();
        Vector3f depth = new Vector3f(across).cross(along).normalize();

        PoseStack pose = new PoseStack();
        Vector3f pivot = new Vector3f(hand).sub(new Vector3f(along).mul(ARM_LENGTH));
        pose.translate(pivot.x, pivot.y, pivot.z);
        pose.mulPose(new Quaternionf().setFromNormalized(new Matrix3f(across, along, depth)));
        // The arm is close to the camera while the weapon is drawn small, so a full-width arm would swamp it.
        pose.scale(0.68F, 1, 0.68F);

        pose.pushPose();
        // The model part applies its own offset from the body; cancel it so the pivot is where it was just put.
        pose.translate((right ? 5F : -5F) / 16, -2F / 16, 0);
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(player);
        if (right) renderer.renderRightHand(pose, buffers, light, player);
        else renderer.renderLeftHand(pose, buffers, light, player);
        pose.popPose();

        if (player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof SuitItem suit) {
            pose.scale(1F / 16, 1F / 16, 1F / 16);
            float t = player.level().getGameTime();
            Canvas.drawMaterial(pose, buffers, t, light, Canvas.noOverlay(), SuitArt.material(suit.set), false,
                    c -> SuitArt.draw(c, suit.set, right ? SuitArt.Piece.ARM_R : SuitArt.Piece.ARM_L, SuitArt.State.IDLE));
        }
    }
}
