package io.github.genichimaruo.singuloarsenal.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.genichimaruo.singuloarsenal.tool.AccretionDrillItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Divert only a successfully broken block's XP; canceled events cannot grant it. Wrapped so other mods can share it. */
@Mixin(ServerPlayerGameMode.class)
public abstract class MiningExperienceMixin {
    @Shadow @Final protected ServerPlayer player;
    @WrapOperation(method = "destroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popExperience(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;I)V"))
    private void arsenalExperience(Block block, ServerLevel level, BlockPos pos, int experience, Operation<Void> original) {
        if (player.getMainHandItem().getItem() instanceof AccretionDrillItem) {
            if (level.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS) && !level.restoringBlockSnapshots) player.giveExperiencePoints(experience);
        } else original.call(block, level, pos, experience);
    }
}
