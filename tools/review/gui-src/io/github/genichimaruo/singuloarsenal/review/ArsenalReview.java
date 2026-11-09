package io.github.genichimaruo.singuloarsenal.review;

import com.mojang.authlib.GameProfile;
import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.module.IntegratorBlockEntity;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import io.netty.channel.embedded.EmbeddedChannel;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Husk;
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
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Drives a disposable world and saves screenshots of the gear as the wearer and as another player sees it. The "other player" is a
 * real server player on a mock connection, so this client receives and renders it exactly as it would a remote person.
 */
@EventBusSubscriber(modid = SinguloArsenal.MOD_ID, value = Dist.CLIENT)
public final class ArsenalReview {
    private static final double OX = 0.5, OZ = 8.5;
    private record Step(int tick, Runnable action) {}
    private static final List<Step> STEPS = new ArrayList<>();
    private static int total, ticks, cursor, at;
    private static boolean preparing;
    private static volatile boolean prepared;
    private static volatile ServerPlayer other;
    private static Husk dummy;
    private static String capture;
    private static boolean done;

    private static ItemStack charged(Item item) {
        ItemStack stack = new ItemStack(item);
        Fe.set(stack, Fe.capacity(stack));
        return stack;
    }

    private static List<Item> weapons() {
        return List.of(ArsenalItems.QUENCH_SABER.get(), ArsenalItems.CASIMIR_BLADE.get(), ArsenalItems.TIDAL_REAVER.get(), ArsenalItems.JET_LANCE.get());
    }

    private static void dress(LivingEntity who, ArmorSet set, int load) {
        var pieces = ArsenalItems.SUITS.get(set);
        who.setItemSlot(EquipmentSlot.HEAD, charged(pieces.get(ArmorItem.Type.HELMET).get()));
        ItemStack chest = charged(pieces.get(ArmorItem.Type.CHESTPLATE).get());
        chest.set(ArsenalData.LOAD.get(), load);
        who.setItemSlot(EquipmentSlot.CHEST, chest);
        who.setItemSlot(EquipmentSlot.LEGS, charged(pieces.get(ArmorItem.Type.LEGGINGS).get()));
        who.setItemSlot(EquipmentSlot.FEET, charged(pieces.get(ArmorItem.Type.BOOTS).get()));
    }

    private static void prepare(Minecraft mc) {
        var server = mc.getSingleplayerServer();
        ServerPlayer p = server.getPlayerList().getPlayer(mc.player.getUUID());
        ServerLevel level = p.serverLevel();
        level.setDayTime(6000);
        level.setWeatherParameters(6000, 0, false, false);
        for (int x = -22; x <= 22; x++) for (int z = -22; z <= 40; z++) {
            level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 81; y <= 95; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        for (int x = -22; x <= 22; x++) for (int y = 81; y <= 92; y++) level.setBlock(new BlockPos(x, y, 40), Blocks.DEEPSLATE_TILES.defaultBlockState(), 3);
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        stand.moveTo(-6.5, 81, 4.5, 180, 0);
        dress(stand, ArmorSet.COLLAPSAR, 50);
        level.addFreshEntity(stand);
        for (int i = 0; i < 3; i++) {
            Husk husk = EntityType.HUSK.create(level);
            husk.moveTo(new double[]{OX + 2.3, OX - 2.3, OX}[i], 81, OZ + 5.5, 180, 0);
            husk.setNoAi(true);
            husk.setPersistenceRequired();
            husk.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
            husk.setHealth(1000);
            level.addFreshEntity(husk);
            if (i == 0) dummy = husk;
        }
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "Other"), false);
        ServerPlayer o = new ServerPlayer(server, level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, o, cookie);
        o.setGameMode(GameType.SURVIVAL);
        o.teleportTo(level, OX, 81, OZ, 0, 0);
        o.setYHeadRot(0);
        o.setInvulnerable(true);
        other = o;
        p.setGameMode(GameType.CREATIVE);
        p.getInventory().clearContent();
        for (int i = 0; i < 4; i++) p.getInventory().setItem(i, charged(weapons().get(i)));
        p.getInventory().setItem(4, new ItemStack(net.minecraft.world.item.Items.DIAMOND_SWORD));
        p.teleportTo(OX, 81, OZ - 10);
        prepared = true;
    }

    private static void teleport(Minecraft mc, double x, double z, float yaw, float pitch) {
        mc.getSingleplayerServer().execute(() -> {
            var p = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
            p.connection.teleport(x, 81, z, yaw, pitch);
        });
    }

    /** Stand {@code distance} away from the other player at {@code bearing} degrees off their facing (+Z), and look at them. */
    private static void watch(Minecraft mc, double bearing, double distance) {
        double a = Math.toRadians(bearing);
        double x = OX + Math.sin(a) * distance, z = OZ + Math.cos(a) * distance;
        float yaw = (float) Math.toDegrees(Math.atan2(-(OX - x), OZ - z));
        teleport(mc, x, z, yaw, 6);
    }

    private static void server(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        mc.getSingleplayerServer().execute(() -> action.accept(other));
    }

    private static void local(Minecraft mc, java.util.function.Consumer<ServerPlayer> action) {
        mc.getSingleplayerServer().execute(() -> action.accept(mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID())));
    }

    private static void hold(ServerPlayer who, Item item) {
        who.setItemSlot(EquipmentSlot.MAINHAND, charged(item));
    }

    private static void step(int wait, Runnable action) {
        at += wait;
        STEPS.add(new Step(at, action));
    }

    private static void shot(String name) {
        step(0, () -> capture = name);
        at += 3;
    }

    private static void selectSlot(Minecraft mc, int slot) {
        mc.player.getInventory().selected = slot;
        local(mc, p -> p.getInventory().selected = slot);
    }

    private static void press(Minecraft mc) {
        mc.options.keyUse.setDown(true);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
    }

    /** Enlarged inventory for inspecting every icon against its slot. Enabled by the marker file build/gui-review/icons-only. */
    private static void iconScript(Minecraft mc) {
        step(60, () -> {
            local(mc, p -> {
                p.setGameMode(GameType.SURVIVAL);
                p.getInventory().clearContent();
                List<Item> all = new ArrayList<>(ArsenalItems.powered());
                all.add(ArsenalItems.FLUX_WEAVE.get());
                all.add(ArsenalItems.PRESSURE_LATTICE.get());
                all.add(ArsenalItems.COLLAPSAR_LAMINA.get());
                all.add(ArsenalItems.JET_COLLIMATOR.get());
                all.add(ArsenalItems.TIDAL_EDGE.get());
                all.add(ModuleContent.INTEGRATOR_ITEM.get());
                for (var module : ModuleContent.MODULES.values()) all.add(module.get());
                for (int i = 0; i < all.size(); i++) p.getInventory().setItem(9 + i, charged(all.get(i)));
            });
        });
        step(20, () -> {
            mc.options.guiScale().set(4);
            mc.resizeDisplay();
            mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
            // Park the cursor in the corner so no tooltip covers the icons.
            org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), 0, 0);
        });
        at += 10;
        shot("icons");
        step(4, () -> done = true);
    }

    private static void script(Minecraft mc) {
        if (Files.exists(mc.gameDirectory.toPath().resolve("icons-only"))) {
            iconScript(mc);
            return;
        }
        String[] names = {"quench", "casimir", "reaver", "lance"};
        step(90, () -> mc.options.setCameraType(CameraType.FIRST_PERSON));

        // Another player, idle, from three sides.
        for (int w = 0; w < 4; w++) {
            int index = w;
            step(2, () -> server(mc, o -> hold(o, weapons().get(index))));
            step(2, () -> watch(mc, -55, 3.2));
            at += 12;
            shot("other-" + names[w] + "-idle-right");
            step(2, () -> watch(mc, -100, 3.2));
            at += 8;
            shot("other-" + names[w] + "-idle-side");
            step(2, () -> watch(mc, 60, 3.2));
            at += 8;
            shot("other-" + names[w] + "-idle-left");
        }

        // A vanilla sword from the same angles, to compare the grip against.
        step(2, () -> server(mc, o -> hold(o, net.minecraft.world.item.Items.DIAMOND_SWORD)));
        step(2, () -> watch(mc, -55, 3.2));
        at += 12;
        shot("other-vanilla-idle-right");
        step(2, () -> watch(mc, -100, 3.2));
        at += 8;
        shot("other-vanilla-idle-side");

        // Another player's effects.
        step(2, () -> server(mc, o -> hold(o, weapons().get(1))));
        step(2, () -> watch(mc, -55, 3.2));
        step(6, () -> server(mc, o -> o.startUsingItem(InteractionHand.MAIN_HAND)));
        at += 12;
        shot("other-casimir-closing");
        at += 12;
        step(2, () -> server(mc, ServerPlayer::stopUsingItem));

        step(2, () -> server(mc, o -> hold(o, weapons().get(0))));
        step(10, () -> server(mc, o -> {
            ItemStack saber = o.getMainHandItem();
            for (int i = 0; i < 5; i++) saber.getItem().hurtEnemy(saber, dummy, o);
            o.swing(InteractionHand.MAIN_HAND, true);
        }));
        at += 3;
        shot("other-quench-burst");

        step(30, () -> server(mc, o -> hold(o, weapons().get(2))));
        step(2, () -> watch(mc, -100, 5));
        step(12, () -> server(mc, o -> o.startUsingItem(InteractionHand.MAIN_HAND)));
        at += 35;
        shot("other-reaver-focus");
        step(2, () -> server(mc, ServerPlayer::releaseUsingItem));
        at += 3;
        shot("other-reaver-shear");

        step(40, () -> server(mc, o -> hold(o, weapons().get(3))));
        step(2, () -> watch(mc, -100, 6));
        step(10, () -> server(mc, o -> o.startUsingItem(InteractionHand.MAIN_HAND)));
        at += 16;
        shot("other-lance-collimating");
        at += 22;
        shot("other-lance-firing-side");
        step(2, () -> watch(mc, -40, 6));
        at += 8;
        shot("other-lance-firing-right");
        step(2, () -> watch(mc, 165, 6));
        at += 8;
        shot("other-lance-firing-behind");
        step(2, () -> watch(mc, 30, 5));
        at += 8;
        shot("other-lance-firing-front");
        step(2, () -> server(mc, ServerPlayer::releaseUsingItem));
        step(2, () -> watch(mc, -70, 3.2));
        for (int[] cue : new int[][]{{20, 1}, {22, 2}, {26, 3}, {30, 4}, {22, 5}}) {
            at += cue[0];
            shot("other-lance-reload-" + cue[1]);
        }
        at += 45;

        // Left-handed other player.
        step(10, () -> server(mc, o -> { o.setMainArm(HumanoidArm.LEFT); hold(o, weapons().get(2)); }));
        step(2, () -> watch(mc, 55, 3.2));
        at += 12;
        shot("other-left-handed-reaver");
        step(2, () -> server(mc, o -> { o.setMainArm(HumanoidArm.RIGHT); o.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY); }));

        // The player's own view.
        step(4, () -> teleport(mc, OX, OZ - 4, 0, 4));
        step(2, () -> server(mc, o -> o.setGameMode(GameType.CREATIVE)));
        for (int w = 0; w < 4; w++) {
            int slot = w;
            step(4, () -> selectSlot(mc, slot));
            at += 26;
            shot("self-" + names[w] + "-first-person");
        }
        step(4, () -> selectSlot(mc, 4));
        at += 26;
        shot("self-vanilla-first-person");
        step(2, () -> selectSlot(mc, 1));
        at += 26;
        step(2, () -> press(mc));
        at += 18;
        shot("self-casimir-closing");
        step(10, () -> mc.options.keyUse.setDown(false));

        step(2, () -> selectSlot(mc, 2));
        at += 26;
        step(2, () -> press(mc));
        at += 30;
        shot("self-reaver-focus");
        step(2, () -> mc.options.keyUse.setDown(false));
        at += 3;
        shot("self-reaver-shear");

        step(10, () -> selectSlot(mc, 3));
        at += 26;
        step(2, () -> press(mc));
        at += 16;
        shot("self-lance-collimating");
        at += 22;
        shot("self-lance-firing");
        step(2, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK));
        at += 8;
        shot("self-lance-firing-third-back");
        step(2, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT));
        at += 8;
        shot("self-lance-firing-third-front");
        step(2, () -> { mc.options.keyUse.setDown(false); mc.options.setCameraType(CameraType.FIRST_PERSON); });
        for (int[] cue : new int[][]{{20, 1}, {22, 2}, {26, 3}, {30, 4}, {22, 5}}) {
            at += cue[0];
            shot("self-lance-reload-" + cue[1]);
        }
        at += 45;

        // Left-handed self.
        step(4, () -> { mc.options.mainHand().set(HumanoidArm.LEFT); mc.options.broadcastOptions(); });
        step(4, () -> selectSlot(mc, 0));
        at += 26;
        shot("self-left-handed-quench");
        step(2, () -> selectSlot(mc, 3));
        at += 26;
        shot("self-left-handed-lance");
        step(2, () -> { mc.options.mainHand().set(HumanoidArm.RIGHT); mc.options.broadcastOptions(); });

        // Bit drones and the Module Integrator, seen as another player.
        step(10, () -> server(mc, o -> {
            dress(o, ArmorSet.COLLAPSAR, 10);
            Modules.install(o.getItemBySlot(EquipmentSlot.CHEST), List.of(new ItemStack(ModuleContent.MODULES.get(ModuleType.BIT_DRONE).get()), new ItemStack(ModuleContent.MODULES.get(ModuleType.BIT_DRONE).get())));
            o.setGameMode(GameType.SURVIVAL);
        }));
        step(2, () -> watch(mc, -40, 4.5));
        at += 14;
        shot("other-bits-orbit");
        at += 4;
        shot("other-bits-orbit-2");
        step(2, () -> local(mc, p -> {
            var level = p.serverLevel();
            BlockPos at = new BlockPos(3, 81, 3);
            level.setBlock(at, ModuleContent.INTEGRATOR.get().defaultBlockState(), 3);
            var entity = (IntegratorBlockEntity) level.getBlockEntity(at);
            entity.energy().receiveEnergy(400_000, false);
            entity.items().setStackInSlot(0, charged(ArsenalItems.JET_LANCE.get()));
            entity.items().setStackInSlot(1, new ItemStack(ModuleContent.MODULES.get(ModuleType.COOLANT).get()));
            entity.items().setStackInSlot(2, new ItemStack(ModuleContent.MODULES.get(ModuleType.AMPLIFIER).get()));
            p.connection.teleport(3.5, 81, 0.6, 0, 18);
            p.getInventory().selected = 8;
        }));
        at += 20;
        shot("integrator-idle");
        step(2, () -> local(mc, p -> ((IntegratorBlockEntity) p.serverLevel().getBlockEntity(new BlockPos(3, 81, 3))).start()));
        at += 30;
        shot("integrator-working");
        at += 70;
        step(4, () -> local(mc, p -> p.openMenu((IntegratorBlockEntity) p.serverLevel().getBlockEntity(new BlockPos(3, 81, 3)), new BlockPos(3, 81, 3))));
        at += 14;
        shot("integrator-screen");
        step(2, () -> mc.setScreen(null));
        step(2, () -> { selectSlot(mc, 3); teleport(mc, OX, OZ - 4, 0, 4); });
        at += 24;

        // Black holes on the suits, close up, front and back.
        step(10, () -> local(mc, p -> dress(p, ArmorSet.COLLAPSAR, 10)));
        step(2, () -> server(mc, o -> dress(o, ArmorSet.COLLAPSAR, 50)));
        step(2, () -> watch(mc, 0, 2.6));
        at += 14;
        shot("suit-hole-front");
        step(2, () -> watch(mc, 180, 2.6));
        at += 10;
        shot("suit-hole-back");
        step(2, () -> watch(mc, 130, 2.6));
        at += 10;
        shot("suit-hole-back-oblique");
        step(6, () -> mc.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player)));
        at += 6;
        shot("zz-inventory");
        step(4, () -> done = true);
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post event) {
        // A mock connection has nothing to pump this player, so step it the way a real connection would.
        if (other != null) other.doTick();
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        var mc = Minecraft.getInstance();
        if (++total > 9000) throw new IllegalStateException("Arsenal review timed out: " + mc.screen);
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
        if (STEPS.isEmpty()) script(mc);
        if (mc.screen != null && !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) return;
        ticks++;
        while (cursor < STEPS.size() && STEPS.get(cursor).tick <= ticks) STEPS.get(cursor++).action.run();
    }

    @SubscribeEvent
    public static void render(RenderFrameEvent.Post event) throws Exception {
        var mc = Minecraft.getInstance();
        if (capture != null) {
            var directory = mc.gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(directory);
            try (var shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                shot.writeToFile(directory.resolve(capture + ".png"));
            }
            System.out.println("ARSENAL_REVIEW_CAPTURE " + capture);
            capture = null;
        }
        if (done) mc.stop();
    }
}
