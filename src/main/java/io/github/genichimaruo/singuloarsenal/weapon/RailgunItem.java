package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.network.RailShotPayload;
import java.util.Comparator;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

/** A creative-only prototype. The server owns aim, obstruction, damage and the firing cooldown. */
public final class RailgunItem extends Item {
    public static final int CHARGE_TICKS = 32;
    public static final int COOLDOWN_TICKS = 24;
    public static final double RANGE = 96;
    public static final float DAMAGE = 32;
    public static final int MAX_TARGETS = 4;
    public static final ResourceKey<DamageType> DAMAGE_TYPE = ResourceKey.create(Registries.DAMAGE_TYPE,
            ResourceLocation.fromNamespaceAndPath("singulo_arsenal", "railgun"));

    public RailgunItem() { super(new Properties().stacksTo(1).rarity(Rarity.EPIC)); }

    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 72_000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.getAbilities().instabuild || hand != InteractionHand.MAIN_HAND) {
            if (!level.isClientSide) player.displayClientMessage(Component.translatable("railgun.singulo_arsenal.creative_only"), true);
            return InteractionResultHolder.fail(stack);
        }
        if (player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!level.isClientSide && getUseDuration(stack, entity) - remaining == CHARGE_TICKS) {
            level.playSound(null, entity.blockPosition(), WeaponContent.READY.get(), SoundSource.PLAYERS, 0.7F, 1);
        }
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (entity instanceof ServerPlayer player) {
            int charge = getUseDuration(stack, entity) - timeLeft;
            if (!tryFire(player, stack, charge) && charge > 3 && charge < CHARGE_TICKS) {
                level.playSound(null, entity.blockPosition(), WeaponContent.VENT.get(), SoundSource.PLAYERS, 0.35F, 1.4F);
            }
        }
    }

    /** Public for real server GameTests; no client-to-server "fire" packet exists. */
    public static boolean tryFire(ServerPlayer player, ItemStack stack, int chargeTicks) {
        if (!player.isAlive() || player.isSpectator() || !player.getAbilities().instabuild
                || !(stack.getItem() instanceof RailgunItem) || player.getMainHandItem() != stack
                || chargeTicks < CHARGE_TICKS || player.getCooldowns().isOnCooldown(stack.getItem())) return false;
        var level = player.serverLevel();
        Vec3 from = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        var wall = level.clip(new ClipContext(from, from.add(direction.scale(RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = wall.getLocation();
        DamageSource source = new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DAMAGE_TYPE), player);
        // Sort by actual intersection with the ray; only the first four eligible targets are penetrated.
        record Target(LivingEntity entity, double distance) {}
        var targets = level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(0.2),
                e -> e != player && e.isAlive() && !e.isSpectator() && !e.isAlliedTo(player)
                        && (!(e instanceof Player other) || player.canHarmPlayer(other)))
                .stream().map(e -> new Target(e, e.getBoundingBox().inflate(0.12).clip(from, end)
                        .map(point -> point.distanceToSqr(from)).orElse(Double.POSITIVE_INFINITY)))
                .filter(t -> Double.isFinite(t.distance())).sorted(Comparator.comparingDouble(Target::distance)).limit(MAX_TARGETS).toList();
        int hits = 0;
        for (Target target : targets) {
            if (target.entity().hurt(source, DAMAGE)) {
                hits++;
                target.entity().push(direction.x * 0.35, 0.08, direction.z * 0.35);
                target.entity().hurtMarked = true;
            }
        }
        player.getCooldowns().addCooldown(stack.getItem(), COOLDOWN_TICKS);
        // The visual muzzle is offset from the eye, but cannot protrude through a solid wall.
        Vec3 right = direction.cross(new Vec3(0, 1, 0)).normalize();
        double side = player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT ? 1 : -1;
        Vec3 muzzle = from.add(direction.scale(0.85)).add(right.scale(0.22 * side)).add(0, -0.18, 0);
        var muzzleWall = level.clip(new ClipContext(from, muzzle, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (muzzleWall.getType() != HitResult.Type.MISS || end.distanceTo(from) < 1) muzzle = from;
        Vec3 middle = from.lerp(end, 0.5);
        var shot = new RailShotPayload(player.getId(), muzzle, end, wall.getDirection(), wall.getType() == HitResult.Type.BLOCK, hits);
        // Fake players have no negotiated rendering channel; they must not interrupt the shot.
        for (ServerPlayer viewer : level.players()) {
            if (viewer.distanceToSqr(middle) <= 160 * 160 && viewer.connection.hasChannel(RailShotPayload.TYPE)) {
                PacketDistributor.sendToPlayer(viewer, shot);
            }
        }
        level.playSound(null, player.blockPosition(), WeaponContent.FIRE.get(), SoundSource.PLAYERS, 3, 1);
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("railgun.singulo_arsenal.prototype").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("railgun.singulo_arsenal.controls").withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("railgun.singulo_arsenal.stats").withStyle(ChatFormatting.DARK_GRAY));
    }
}
