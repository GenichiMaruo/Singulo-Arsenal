package io.github.genichimaruo.singuloarsenal.weapon;

import io.github.genichimaruo.singuloarsenal.core.ArsenalDamage;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A shouldered gun built around a contained black hole. A magnetic mirror seals the rear pole, so the whole jet leaves through the
 * barrel. One click spools the rings, then the jet goes off once at full length and burns for two seconds. A magazine holds four shots.
 */
public final class JetLanceItem extends Item implements ArsenalWeapon {
    public JetLanceItem() {
        super(new Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant());
    }

    @Override public int capacity() { return Rules.LANCE_CAPACITY; }
    @Override public boolean accretes() { return true; }
    @Override public int stage() { return 5; }
    @Override public InteractionResult useOn(UseOnContext context) { return Fe.chargeFromBlock(context); }
    @Override public boolean shouldCauseReequipAnimation(ItemStack from, ItemStack to, boolean changed) { return !ItemStack.isSameItem(from, to); }
    @Override public boolean isBarVisible(ItemStack stack) { return true; }
    @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * Fe.stored(stack) / Math.max(1, Fe.capacity(stack))); }
    @Override public int getBarColor(ItemStack stack) { return 0xFFC36B; }

    public static boolean breaksTerrain(ItemStack stack) {
        return ArsenalData.get(stack, ArsenalData.TERRAIN_MODE) != 0;
    }

    public static int shotsLeft(ItemStack stack) {
        return Math.max(0, Rules.LANCE_MAGAZINE - ArsenalData.get(stack, ArsenalData.SHOTS));
    }

    /** Right click switches between sparing and melting the terrain. */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            boolean breaking = !breaksTerrain(stack);
            stack.set(ArsenalData.TERRAIN_MODE.get(), breaking ? 1 : 0);
            player.displayClientMessage(Component.translatable(breaking ? "message.singulo_arsenal.terrain_break" : "message.singulo_arsenal.terrain_protect"), true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.DISPENSER_DISPENSE, SoundSource.PLAYERS, 0.6F, breaking ? 0.7F : 1.4F);
        }
        return InteractionResultHolder.consume(stack);
    }

    /** Spool-up from 0 to 1, or -1 when the gun is not charging. */
    public static float chargeProgress(ItemStack stack, long now, float partial) {
        long remaining = ArsenalData.time(stack, ArsenalData.CHARGE_UNTIL) - now;
        return remaining <= 0 || remaining > Rules.LANCE_CHARGE_TICKS ? -1 : 1 - (remaining - partial) / Rules.LANCE_CHARGE_TICKS;
    }

    /** Ticks since this shot was lit, including the frame fraction, or -1 when the jet is off. */
    public static float jetAge(ItemStack stack, long now, float partial) {
        long remaining = ArsenalData.time(stack, ArsenalData.JET_UNTIL) - now;
        return remaining <= 0 || remaining > Rules.LANCE_SHOT_TICKS ? -1 : Rules.LANCE_SHOT_TICKS - remaining + partial;
    }

    /** Whether the gun could start a shot right now. Run on the client as well, to avoid sending clicks the server would refuse. */
    public static boolean ready(ItemStack stack, long now) {
        return shotsLeft(stack) > 0 && now >= ArsenalData.time(stack, ArsenalData.JET_UNTIL) + Rules.LANCE_SHOT_GAP
                && ArsenalData.time(stack, ArsenalData.CHARGE_UNTIL) <= now && Weapons.reloadProgress(stack, now, 0) < 0;
    }

    /** The left-click packet lands here and starts the spool-up. It carries no values; everything is decided from server state. */
    public static boolean tryFire(ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof JetLanceItem item) || !player.isAlive() || player.isSpectator()) return false;
        long now = player.serverLevel().getGameTime();
        if (!ready(stack, now) || player.getCooldowns().isOnCooldown(item)) return false;
        if (Fe.stored(stack) < Rules.LANCE_SHOT_FE) {
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.no_energy"), true);
            return false;
        }
        stack.set(ArsenalData.CHARGE_UNTIL.get(), now + Rules.LANCE_CHARGE_TICKS);
        return true;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(level instanceof ServerLevel server) || !(entity instanceof ServerPlayer player)) return;
        if (player.getMainHandItem() != stack) {
            // Switching away cuts the jet and drops the charge.
            stack.remove(ArsenalData.CHARGE_UNTIL.get());
            if (jetAge(stack, level.getGameTime(), 0) >= 0) stack.remove(ArsenalData.JET_UNTIL.get());
            finish(stack, player, server);
            return;
        }
        tickShot(stack, player, server);
        reloadCues(stack, player, level);
    }

    /** Public so GameTests exercise the real path. */
    public static void tickShot(ItemStack stack, ServerPlayer player, ServerLevel level) {
        long now = level.getGameTime();
        long chargeUntil = ArsenalData.time(stack, ArsenalData.CHARGE_UNTIL);
        if (chargeUntil != 0 && now >= chargeUntil) {
            stack.remove(ArsenalData.CHARGE_UNTIL.get());
            // A charge left over from before a relog must not go off by itself.
            if (now - chargeUntil <= 2) ignite(stack, player, level);
            return;
        }
        float age = jetAge(stack, now, 0);
        if (age < 0) {
            // Resume after a missed final tick, for example when the cannon was stored or its owner logged out.
            finish(stack, player, level);
            return;
        }
        if (age > 0 && (int) age % Rules.LANCE_PULSE == 0) pulse(player, level);
        if ((int) age == Rules.LANCE_SHOT_TICKS - 1) finish(stack, player, level);
    }

    /**
     * The one moment the shot goes off: everything the jet does to the world happens here, at once. In breaking mode it drills to the
     * far end of what is in front of it; a meltdown goes up at the deepest point it reached. The two seconds of burn that follow only burn.
     */
    private static void ignite(ItemStack stack, ServerPlayer player, ServerLevel level) {
        if (!Fe.spend(stack, Rules.LANCE_SHOT_FE)) {
            player.displayClientMessage(Component.translatable("message.singulo_arsenal.no_energy"), true);
            return;
        }
        stack.set(ArsenalData.JET_UNTIL.get(), level.getGameTime() + Rules.LANCE_SHOT_TICKS);
        stack.set(ArsenalData.SHOTS.get(), ArsenalData.get(stack, ArsenalData.SHOTS) + 1);
        Vec3 deepest = breaksTerrain(stack) ? Terrain.drill(player, level, player.getEyePosition(), player.getLookAngle().normalize(), Rules.LANCE_RANGE) : null;
        Vec3 contact = pulse(player, level);
        boom(level, player.position());
        Vec3 impact = deepest != null ? deepest : contact;
        if (impact == null) return;
        int meltdown = Modules.count(stack, ModuleType.MELTDOWN);
        if (meltdown > 0) detonate(stack, player, level, impact, meltdown);
        else boom(level, impact);
    }

    private static void boom(ServerLevel level, Vec3 at) {
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 5F, 0.7F);
        Weapons.soundAt(level, at, "jet_forge.pulse", 5F, 0.8F);
    }

    private static void finish(ItemStack stack, ServerPlayer player, ServerLevel level) {
        if (ArsenalData.get(stack, ArsenalData.SHOTS) < Rules.LANCE_MAGAZINE) return;
        int ticks = Weapons.cooled(stack, Rules.LANCE_COOLDOWN);
        stack.remove(ArsenalData.SHOTS.get());
        player.getCooldowns().addCooldown(stack.getItem(), ticks);
        Weapons.beginReload(stack, level.getGameTime(), ticks);
        Weapons.sound(player, "jet_forge.cool", 0.7F, 1.3F);
    }

    /** Timed sound cues for the reload: one per step of the animation. */
    private static void reloadCues(ItemStack stack, Player player, Level level) {
        float progress = Weapons.reloadProgress(stack, level.getGameTime(), 0);
        if (progress < 0) return;
        // Cues are placed on the base 160-tick timeline. A coolant module shortens the run, so ticks can skip over a cue.
        int tick = Math.round(progress * Rules.LANCE_COOLDOWN);
        int previous = Math.round(Math.max(0, Weapons.reloadProgress(stack, level.getGameTime() - 1, 0)) * Rules.LANCE_COOLDOWN);
        if (crossed(previous, tick, 8)) Weapons.sound(player, "sealed_container.vent", 0.9F, 1.1F);
        else if (crossed(previous, tick, 34)) Weapons.sound(player, "drone_port.hatch_open", 0.9F, 1.5F);
        else if (crossed(previous, tick, 92)) Weapons.sound(player, "sealed_container.insert", 1, 0.9F);
        else if (crossed(previous, tick, 110)) Weapons.sound(player, "drone_port.clamp", 1, 0.8F);
        else if (crossed(previous, tick, 118)) Weapons.sound(player, "drone_port.clamp", 1, 1.4F);
        else if (crossed(previous, tick, 140)) Weapons.sound(player, "sealed_container.unlock", 0.8F, 1.3F);
    }

    private static boolean crossed(int previous, int now, int cue) {
        return previous < cue && now >= cue;
    }

    /** One damage pulse along the jet. Returns the first thing it touched, or null. Public so GameTests exercise the real path. */
    public static Vec3 pulse(ServerPlayer player, ServerLevel level) {
        ItemStack stack = player.getMainHandItem();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        BlockHitResult block = level.clip(new ClipContext(eye, eye.add(look.scale(Rules.LANCE_RANGE)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 end = block.getLocation();
        var source = ArsenalDamage.source(level, ArsenalDamage.JET, player);
        float damage = Weapons.amplified(stack, Rules.LANCE_DAMAGE);
        Vec3 contact = block.getType() == HitResult.Type.MISS ? null : end;
        for (LivingEntity target : ArsenalDamage.along(player, eye, end, Rules.LANCE_HIT_WIDTH)) {
            Vec3 touched = touchPoint(target, eye, end);
            if (contact == null || eye.distanceToSqr(touched) < eye.distanceToSqr(contact)) contact = touched;
            burn(target, source, damage);
        }
        return contact;
    }

    private static Vec3 touchPoint(LivingEntity target, Vec3 from, Vec3 to) {
        AABB bounds = target.getBoundingBox().inflate(Rules.LANCE_HIT_WIDTH);
        return bounds.contains(from) ? from : bounds.clip(from, to).orElse(target.getBoundingBox().getCenter());
    }

    /** The meltdown module: where the shot ended up goes up immediately. Only breaking mode leaves a crater. */
    private static void detonate(ItemStack stack, ServerPlayer player, ServerLevel level, Vec3 at, int modules) {
        double radius = Rules.blastRadius(modules);
        var source = ArsenalDamage.source(level, ArsenalDamage.JET, player);
        float damage = Weapons.amplified(stack, Rules.LANCE_BLAST_DAMAGE);
        for (LivingEntity target : ArsenalDamage.around(player, at, radius)) {
            double distance = Math.sqrt(target.getBoundingBox().distanceToSqr(at));
            if (target.hurt(source, damage * Rules.blastFalloff(distance, radius))) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 100));
        }
        if (breaksTerrain(stack)) Terrain.crater(player, level, at, radius);
        FxPayload.send(level, FxPayload.MELTDOWN, at, Vec3.ZERO, (float) radius);
        level.playSound(null, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 8F, 0.55F);
    }

    private static void burn(LivingEntity target, net.minecraft.world.damagesource.DamageSource source, float damage) {
        if (target.hurt(source, damage)) target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), 40));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable(breaksTerrain(stack) ? "tooltip.singulo_arsenal.terrain_break" : "tooltip.singulo_arsenal.terrain_protect").withStyle(ChatFormatting.YELLOW));
        Weapons.describe(BuiltInRegistries.ITEM.getKey(this).getPath(), stack, stage(), lines);
    }
}
