package io.github.genichimaruo.singuloarsenal.review;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.weapon.WeaponContent;
import java.nio.file.Files;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.CameraType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;

/** Drives real use/release packets in a disposable integrated-server world. */
@EventBusSubscriber(modid = SinguloArsenal.MOD_ID, value = Dist.CLIENT)
public final class RailgunReview {
    private static int total, ticks;
    private static boolean preparing;
    private static volatile boolean prepared;
    private static String capture;
    private static int inventoryFrames;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (++total > 3600) throw new IllegalStateException("Railgun review timed out: " + mc.screen);
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
            mc.getSingleplayerServer().execute(() -> {
                var server = mc.getSingleplayerServer();
                var p = server.getPlayerList().getPlayer(mc.player.getUUID());
                var level = p.serverLevel();
                level.setDayTime(1000);
                level.setWeatherParameters(6000, 0, false, false);
                for (int x = -10; x <= 10; x++) for (int z = -12; z <= 14; z++) {
                    level.setBlock(new BlockPos(x, 80, z), (x%5==0 ? Blocks.SEA_LANTERN : Blocks.SMOOTH_STONE).defaultBlockState(), 3);
                    for (int y = 81; y <= 90; y++) level.setBlock(new BlockPos(x, y, z),
                        (z==12 ? ((x%4==0||y==81||y==88) ? Blocks.QUARTZ_BLOCK : Blocks.DEEPSLATE_TILES) : Blocks.AIR).defaultBlockState(), 3);
                }
                p.setGameMode(GameType.CREATIVE);
                p.getInventory().clearContent();
                p.getInventory().setItem(0, new ItemStack(WeaponContent.RAILGUN.get()));
                p.getInventory().selected = 0;
                p.teleportTo(.5, 81, -5.5);
                p.setYRot(0); p.setXRot(0);
                prepared = true;
            });
            return;
        }
        if (!prepared || mc.getOverlay()!=null || mc.screen!=null) return;
        ticks++;
        mc.player.setYRot(0); mc.player.setXRot(0);
        switch(ticks) {
            case 80 -> capture = "01-idle";
            case 100, 180, 270 -> {
                mc.options.keyUse.setDown(true);
                mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
            }
            case 140 -> capture = "02-charged";
            case 145, 225, 315 -> mc.options.keyUse.setDown(false);
            case 147 -> capture = "03-shot";
            case 151 -> capture = "04-impact";
            case 160 -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            case 215 -> capture = "05-third-person-charged";
            case 227 -> capture = "06-third-person-shot";
            case 240 -> { mc.options.setCameraType(CameraType.FIRST_PERSON); mc.options.mainHand().set(net.minecraft.world.entity.HumanoidArm.LEFT); mc.options.broadcastOptions(); }
            case 310 -> capture = "07-left-handed";
            case 330 -> { mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); }
            case 345 -> capture = "08-front";
            case 360 -> {
                mc.options.mainHand().set(net.minecraft.world.entity.HumanoidArm.RIGHT); mc.options.broadcastOptions();
                mc.options.setCameraType(CameraType.FIRST_PERSON);
                mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
                capture = "09-inventory";
            }
        }
    }
    @SubscribeEvent public static void render(RenderFrameEvent.Post event) throws Exception {
        var mc = Minecraft.getInstance();
        if (capture == null) return;
        // Allow the Creative inventory screen to finish replacing the regular inventory.
        if(capture.equals("09-inventory") && inventoryFrames++ < 5) return;
        var directory = mc.gameDirectory.toPath().resolve("screenshots");
        Files.createDirectories(directory);
        try (var shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) { shot.writeToFile(directory.resolve(capture + ".png")); }
        System.out.println("RAILGUN_REVIEW_CAPTURE " + capture);
        if (capture.equals("09-inventory")) mc.stop();
        capture = null;
    }
}
