package io.github.genichimaruo.singuloarsenal.gametest;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.weapon.RailgunItem;
import io.github.genichimaruo.singuloarsenal.weapon.WeaponContent;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SinguloArsenal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RailgunGameTests {
    private static final String ARENA = "empty";
    private static ServerPlayer shooter(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        var at = h.absoluteVec(new Vec3(2.5, 1, .2));
        player.moveTo(at.x, at.y, at.z, 0, 0);
        player.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(WeaponContent.RAILGUN.get()));
        // Stop every test shot within its own arena, including shots that miss entities.
        h.setBlock(2, 2, 4, Blocks.IRON_BLOCK);
        return player;
    }
    private static Zombie target(GameTestHelper h, double x, double z) {
        var mob = EntityType.ZOMBIE.create(h.getLevel());
        var at = h.absoluteVec(new Vec3(x, 1, z));
        mob.moveTo(at.x, at.y, at.z, 0, 0);
        mob.setNoAi(true);
        mob.setNoGravity(true);
        mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);
        mob.getAttribute(Attributes.ARMOR).setBaseValue(0);
        mob.setHealth(100);
        h.getLevel().addFreshEntity(mob);
        return mob;
    }
    private static boolean fire(ServerPlayer p, int charge) { return RailgunItem.tryFire(p, p.getMainHandItem(), charge); }

    @GameTest(template = ARENA)
    public static void railgunChargeAndCooldownGateDamage(GameTestHelper h) {
        var p = shooter(h);
        var mob = target(h, 2.5, 2);
        h.assertTrue(!fire(p, 31) && mob.getHealth() == 100, "partial charge caused damage");
        h.assertTrue(fire(p, 32) && mob.getHealth() == 68, "fully charged shot failed");
        h.assertTrue(!fire(p, 32) && mob.getHealth() == 68, "cooldown allowed repeated fire");
        for (int i = 0; i < RailgunItem.COOLDOWN_TICKS; i++) p.getCooldowns().tick();
        mob.invulnerableTime = 0;
        h.assertTrue(fire(p, 32) && mob.getHealth() == 36, "gun did not recover after cooldown");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void railgunCreativeAndMainHandOnly(GameTestHelper h) {
        var p = shooter(h);
        var mob = target(h, 2.5, 2);
        p.setGameMode(GameType.SURVIVAL);
        h.assertTrue(!fire(p, 32) && mob.getHealth() == 100, "survival player fired prototype");
        h.assertTrue(!WeaponContent.RAILGUN.get().use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult().consumesAction(), "survival player began charging");
        p.setGameMode(GameType.CREATIVE);
        var gun = p.getMainHandItem();
        p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        p.setItemSlot(EquipmentSlot.OFFHAND, gun);
        h.assertTrue(!RailgunItem.tryFire(p, gun, 32), "offhand gun fired");
        h.assertTrue(!WeaponContent.RAILGUN.get().use(h.getLevel(), p, InteractionHand.OFF_HAND).getResult().consumesAction(), "offhand gun began charging");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void railgunPiercesFourNearestTargets(GameTestHelper h) {
        var p = shooter(h);
        // Reverse insertion order proves that ray distance, not entity registration order, controls penetration.
        Zombie[] targets = new Zombie[5];
        for (int i = 4; i >= 0; i--) targets[i] = target(h, 2.5, 1 + i * .6);
        var offAxis = target(h, 3.5, 2);
        h.assertTrue(fire(p, 32), "shot failed");
        for (int i = 0; i < 4; i++) h.assertTrue(targets[i].getHealth() == 68, "nearest target not pierced: " + i);
        h.assertTrue(targets[4].getHealth() == 100 && offAxis.getHealth() == 100, "fifth or off-axis target was damaged");
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void railgunCannotShootOrDestroyWalls(GameTestHelper h) {
        var p = shooter(h);
        var before = target(h, 2.5, 1.2);
        var behind = target(h, 2.5, 3.5);
        h.setBlock(2, 2, 2, Blocks.GLASS);
        h.assertTrue(fire(p, 32) && before.getHealth() == 68, "target before wall not damaged");
        h.assertTrue(behind.getHealth() == 100, "shot passed through solid glass");
        h.assertBlockPresent(Blocks.GLASS, 2, 2, 2);
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void railgunAlliesAreSkipped(GameTestHelper h) {
        var p = shooter(h);
        var ally = target(h, 2.5, 1.4);
        var enemy = target(h, 2.5, 3);
        var scoreboard = h.getLevel().getScoreboard();
        String name = "rail-" + p.getId();
        var team = scoreboard.addPlayerTeam(name);
        try {
            scoreboard.addPlayerToTeam(p.getScoreboardName(), team);
            scoreboard.addPlayerToTeam(ally.getScoreboardName(), team);
            h.assertTrue(fire(p, 32) && enemy.getHealth() == 68, "enemy behind ally not hit");
            h.assertTrue(ally.getHealth() == 100, "allied entity damaged");
        } finally { scoreboard.removePlayerTeam(team); }
        h.succeed();
    }

    @GameTest(template = ARENA)
    public static void railgunReleaseUsesElapsedCharge(GameTestHelper h) {
        var p = shooter(h);
        var mob = target(h, 2.5, 2);
        var gun = WeaponContent.RAILGUN.get();
        gun.use(h.getLevel(), p, InteractionHand.MAIN_HAND);
        gun.releaseUsing(p.getMainHandItem(), h.getLevel(), p, 72_000 - 20);
        h.assertTrue(mob.getHealth() == 100, "early release fired");
        gun.releaseUsing(p.getMainHandItem(), h.getLevel(), p, 72_000 - 32);
        h.assertTrue(mob.getHealth() == 68, "release failed to fire");
        p.stopUsingItem();
        h.succeed();
    }
}
