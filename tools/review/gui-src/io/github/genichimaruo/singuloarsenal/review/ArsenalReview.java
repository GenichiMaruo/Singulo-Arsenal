package io.github.genichimaruo.singuloarsenal.review;

import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Drives a disposable world through every visual state and saves screenshots. Never part of the distributed JAR. */
@EventBusSubscriber(modid = SinguloArsenal.MOD_ID, value = Dist.CLIENT)
public final class ArsenalReview {
    private static final double[][] STANDS = {{-3, 4}, {0, 4}, {3, 4}};
    private static int total, ticks;
    private static boolean preparing;
    private static volatile boolean prepared;
    private static String capture;
    private static int inventoryFrames;

    private static ItemStack charged(Item item) {
        ItemStack stack = new ItemStack(item);
        Fe.set(stack, Fe.capacity(stack));
        return stack;
    }

    private static void dress(ArmorStand stand, ArmorSet set, int load) {
        var pieces = ArsenalItems.SUITS.get(set);
        stand.setItemSlot(EquipmentSlot.HEAD, charged(pieces.get(ArmorItem.Type.HELMET).get()));
        ItemStack chest = charged(pieces.get(ArmorItem.Type.CHESTPLATE).get());
        chest.set(ArsenalData.LOAD.get(), load);
        stand.setItemSlot(EquipmentSlot.CHEST, chest);
        stand.setItemSlot(EquipmentSlot.LEGS, charged(pieces.get(ArmorItem.Type.LEGGINGS).get()));
        stand.setItemSlot(EquipmentSlot.FEET, charged(pieces.get(ArmorItem.Type.BOOTS).get()));
    }

    private static void prepare(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        ServerPlayer p = server.getPlayerList().getPlayer(mc.player.getUUID());
        var level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (int x = -14; x <= 14; x++) for (int z = -14; z <= 26; z++) {
            level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 81; y <= 95; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        for (int x = -14; x <= 14; x++) for (int y = 81; y <= 90; y++) {
            level.setBlock(new BlockPos(x, y, 26), Blocks.DEEPSLATE_TILES.defaultBlockState(), 3);
        }
        ArmorSet[] sets = ArmorSet.values();
        for (int i = 0; i < 3; i++) {
            ArmorStand stand = EntityType.ARMOR_STAND.create(level);
            stand.moveTo(STANDS[i][0] + 0.5, 81, STANDS[i][1] + 0.5, 180, 0);
            stand.setShowArms(true);
            dress(stand, sets[i], 0);
            level.addFreshEntity(stand);
        }
        ArmorStand gnocchi = EntityType.ARMOR_STAND.create(level);
        gnocchi.moveTo(-3.5, 81, 9.5, 180, 0);
        dress(gnocchi, ArmorSet.COLLAPSAR, 10);
        level.addFreshEntity(gnocchi);
        ArmorStand spaghetti = EntityType.ARMOR_STAND.create(level);
        spaghetti.moveTo(0.5, 81, 9.5, 180, 0);
        dress(spaghetti, ArmorSet.COLLAPSAR, 50);
        level.addFreshEntity(spaghetti);
        ArmorStand lasagna = EntityType.ARMOR_STAND.create(level);
        lasagna.moveTo(4.5, 81, 9.5, 180, 0);
        dress(lasagna, ArmorSet.COLLAPSAR, 95);
        level.addFreshEntity(lasagna);
        ArmorStand hot = EntityType.ARMOR_STAND.create(level);
        hot.moveTo(8.5, 81, 9.5, 180, 0);
        dress(hot, ArmorSet.DEGENERACY, Rules.PRESSURE_MAX);
        level.addFreshEntity(hot);
        for (int i = 0; i < 3; i++) {
            Zombie zombie = EntityType.HUSK.create(level);
            zombie.moveTo(new int[]{-5, 3, 6}[i] + 0.5, 81, 20.5, 180, 0);
            zombie.setNoAi(true);
            zombie.setPersistenceRequired();
            zombie.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            zombie.setHealth(1000);
            level.addFreshEntity(zombie);
        }
        p.setGameMode(GameType.CREATIVE);
        p.getInventory().clearContent();
        List<Item> weapons = List.of(ArsenalItems.QUENCH_SABER.get(), ArsenalItems.CASIMIR_BLADE.get(), ArsenalItems.TIDAL_REAVER.get(), ArsenalItems.JET_LANCE.get());
        for (int i = 0; i < 4; i++) p.getInventory().setItem(i, charged(weapons.get(i)));
        List<Item> all = new ArrayList<>(ArsenalItems.powered());
        for (int i = 0; i < all.size(); i++) p.getInventory().setItem(9 + i, charged(all.get(i)));
        p.getInventory().selected = 3;
        p.teleportTo(0.5, 81, -3.5);
        p.setYRot(0);
        p.setXRot(8);
        prepared = true;
    }

    private static void holdSlot(Minecraft mc, int slot) {
        mc.player.getInventory().selected = slot;
        mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).getInventory().selected = slot);
    }

    private static void teleport(Minecraft mc, double x, double z, float yaw, float pitch) {
        mc.getSingleplayerServer().execute(() -> {
            var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            p.connection.teleport(x, 81, z, yaw, pitch);
        });
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (++total > 4800) throw new IllegalStateException("Arsenal review timed out: " + mc.screen);
        if (mc.screen instanceof net.minecraft.client.gui.screens.BackupConfirmScreen) {
            for (var child : mc.screen.children()) {
                if (child instanceof net.minecraft.client.gui.components.Button b
                        && b.getMessage().getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                        && t.getKey().equals("selectWorld.backupJoinSkipButton")) { b.onPress(); return; }
            }
        }
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        if (!preparing) {
            preparing = true;
            mc.getSingleplayerServer().execute(() -> prepare(mc));
            return;
        }
        if (!prepared || mc.getOverlay() != null) return;
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)) return;
        ticks++;
        switch (ticks) {
            case 100 -> capture = "01-suits-front";
            case 104 -> teleport(mc, 0.5, 13.5, 180, 12);
            case 130 -> capture = "02-suits-back";
            case 134 -> teleport(mc, 2.5, 5.5, 0, 6);
            case 165 -> capture = "03-pasta-phases";
            case 170 -> teleport(mc, 0.5, 15.5, 0, 0);
            case 195 -> holdSlot(mc, 0);
            case 208 -> capture = "04-quench-first-person";
            case 215 -> holdSlot(mc, 1);
            case 228 -> capture = "05-casimir-first-person";
            case 235 -> holdSlot(mc, 2);
            case 248 -> capture = "06-reaver-first-person";
            case 255 -> holdSlot(mc, 3);
            case 268 -> capture = "07-lance-first-person";
            case 275 -> { mc.options.keyUse.setDown(true); mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); }
            case 290 -> capture = "08-lance-collimating";
            case 320 -> capture = "09-lance-firing";
            case 330 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 352 -> capture = "10-lance-third-person";
            case 356 -> { mc.options.keyUse.setDown(false); mc.options.setCameraType(CameraType.FIRST_PERSON); }
            case 530 -> { holdSlot(mc, 2); }
            case 545 -> { mc.options.keyUse.setDown(true); mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND); }
            case 585 -> capture = "11-reaver-focus";
            case 590 -> mc.options.keyUse.setDown(false);
            case 596 -> capture = "12-reaver-shear";
            case 610 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); holdSlot(mc, 3); }
            case 630 -> capture = "13-hold-lance-third-person";
            case 634 -> holdSlot(mc, 2);
            case 650 -> capture = "14-hold-reaver-third-person";
            case 654 -> mc.getSingleplayerServer().execute(() -> {
                ServerPlayer p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var pieces = ArsenalItems.SUITS.get(ArmorSet.COLLAPSAR);
                p.setItemSlot(EquipmentSlot.HEAD, charged(pieces.get(ArmorItem.Type.HELMET).get()));
                p.setItemSlot(EquipmentSlot.CHEST, charged(pieces.get(ArmorItem.Type.CHESTPLATE).get()));
                p.setItemSlot(EquipmentSlot.LEGS, charged(pieces.get(ArmorItem.Type.LEGGINGS).get()));
                p.setItemSlot(EquipmentSlot.FEET, charged(pieces.get(ArmorItem.Type.BOOTS).get()));
            });
            case 680 -> capture = "15-worn-third-person";
            case 684 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); teleport(mc, 0.5, 15.5, 0, -5); }
            case 700 -> net.neoforged.neoforge.network.PacketDistributor.sendToServer(io.github.genichimaruo.singuloarsenal.net.AbilityPayload.INSTANCE);
            case 703 -> capture = "16-jet-burst";
            case 720 -> {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.getSingleplayerServer().execute(() -> mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID()).setGameMode(GameType.SURVIVAL));
            }
            case 740 -> {
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                capture = "17-inventory";
            }
        }
    }

    @SubscribeEvent
    public static void render(RenderFrameEvent.Post event) throws Exception {
        var mc = Minecraft.getInstance();
        if (capture == null) return;
        if (capture.equals("17-inventory") && inventoryFrames++ < 5) return;
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
            shot.writeToFile(directory.resolve(capture + ".png"));
        }
        System.out.println("ARSENAL_REVIEW_CAPTURE " + capture);
        if (capture.equals("17-inventory")) mc.stop();
        capture = null;
    }
}
