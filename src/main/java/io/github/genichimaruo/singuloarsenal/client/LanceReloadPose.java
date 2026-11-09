package io.github.genichimaruo.singuloarsenal.client;

import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.client.model.AnimationUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.IArmPoseTransformer;

/** The two-handed pose during a lance reload: the firing hand keeps the grip while the support hand goes down to the magazine and back. */
public final class LanceReloadPose {
    private static final IArmPoseTransformer TRANSFORM = LanceReloadPose::apply;
    /** Legacy Forge extends arm poses directly through its enum factory. */
    private static final HumanoidModel.ArmPose POSE = HumanoidModel.ArmPose.create("SINGULO_ARSENAL_LANCE_RELOAD", true, TRANSFORM);

    private LanceReloadPose() {}

    private static void apply(HumanoidModel<?> model, LivingEntity entity, HumanoidArm arm) {
        boolean rightHanded = entity.getMainArm() == HumanoidArm.RIGHT;
        AnimationUtils.animateCrossbowHold(model.rightArm, model.leftArm, model.head, rightHanded);
        float partial = net.minecraft.client.Minecraft.getInstance().getFrameTime();
        float progress = Weapons.reloadProgress(entity.getMainHandItem(), entity.level().getGameTime(), partial);
        if (progress < 0) return;
        ModelPart main = rightHanded ? model.rightArm : model.leftArm;
        ModelPart support = rightHanded ? model.leftArm : model.rightArm;
        float tilt = Rules.reloadTilt(progress), reach = Rules.reloadReach(progress), cock = Rules.reloadCock(progress);
        main.xRot += 0.28F * tilt;
        // Down to the magazine well, then across to the charging handle on the far side.
        support.xRot += 0.75F * reach - 0.2F * cock;
        support.yRot += (rightHanded ? -0.35F : 0.35F) * reach + (rightHanded ? -0.55F : 0.55F) * cock;
        support.zRot += (rightHanded ? -0.12F : 0.12F) * reach;
    }

    static HumanoidModel.ArmPose pose() {
        return POSE;
    }
}
