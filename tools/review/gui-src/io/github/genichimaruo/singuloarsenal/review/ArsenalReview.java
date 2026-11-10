package io.github.genichimaruo.singuloarsenal.review;

import com.mojang.authlib.GameProfile;
import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.armor.ArmorSet;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.WeaponArt;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.core.Tips;
import io.github.genichimaruo.singuloarsenal.net.FxPayload;
import io.github.genichimaruo.singuloarsenal.weapon.JetLanceItem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;

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
    private static ItemStack tip;
    private static boolean bare;

    private static ItemStack charged(Item item) {
        ItemStack stack = new ItemStack(item);
        Fe.set(stack, Fe.capacity(stack));
        return stack;
    }

    private static List<Item> weapons() {
        return List.of(ArsenalItems.QUENCH_SABER.get(), ArsenalItems.CASIMIR_BLADE.get(), ArsenalItems.TIDAL_REAVER.get(), ArsenalItems.JET_LANCE.get());
    }

    private static List<Item> tools() {
        return List.of(ArsenalItems.SQUID_PICK.get(), ArsenalItems.DEGENERACY_BREAKER.get(), ArsenalItems.SHEAR_SAW.get(), ArsenalItems.ACCRETION_DRILL.get());
    }

    private static void dress(LivingEntity who, ArmorSet set, int load) {
        var pieces = ArsenalItems.SUITS.get(set);
        who.setItemSlot(EquipmentSlot.HEAD, charged(pieces.get(ArmorItem.Type.HELMET).get()));
        ItemStack chest = charged(pieces.get(ArmorItem.Type.CHESTPLATE).get());
        ArsenalData.set(chest, ArsenalData.LOAD, load);
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
        ServerPlayer o = new ServerPlayer(server, level, new GameProfile(UUID.randomUUID(), "Other"));
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, o);
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
                for (int i = 0; i < all.size(); i++) p.getInventory().setItem(i, charged(all.get(i)));
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
        step(2, () -> bare = true);
        at += 3;
        shot("icons-bare");
        step(2, () -> bare = false);
        step(2, () -> { audit(); tip = new ItemStack(ArsenalItems.JET_LANCE.get()); Fe.set(tip, Fe.capacity(tip)); });
        at += 3;
        shot("tooltip-brief");
        step(2, () -> Tips.detail = () -> true);
        at += 3;
        shot("tooltip-detail");
        // Check the actual scan packet and overlay against ore hidden under the floor.
        step(2, () -> {
            Tips.detail = net.minecraft.client.gui.screens.Screen::hasShiftDown;
            mc.setScreen(null);
            local(mc, p -> {
                ItemStack pick = charged(ArsenalItems.SQUID_PICK.get());
                ArsenalData.set(pick, ArsenalData.SCAN_TIER, 3);
                p.getInventory().setItem(0, pick);
                p.getInventory().selected = 0;
                for (int x = -3; x <= 3; x += 2) p.serverLevel().setBlock(new BlockPos(x, 79, -2), Blocks.DIAMOND_ORE.defaultBlockState(), 3);
                p.connection.teleport(OX, 81, -5, 0, 25);
            });
        });
        step(10, () -> selectSlot(mc, 0));
        step(10, () -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        at += 10;
        shot("scan-through-floor");
        step(4, () -> local(mc, p -> p.getInventory().setItem(0, charged(ArsenalItems.JET_LANCE.get()))));
        step(10, () -> { selectSlot(mc, 0); teleport(mc, OX, OZ - 4, 0, 4); });
        step(10, () -> mc.options.keyAttack.setDown(true));
        step(2, () -> mc.options.keyAttack.setDown(false));
        at += Rules.LANCE_CHARGE_TICKS + 12;
        shot("jet-translucency");
        step(4, () -> done = true);
    }

    /** Reports faces of the weapons that lie in the same plane and overlap, which is what shimmers at a distance. */
    private static void audit() {
        record Quad(int axis, float plane, float a0, float a1, float b0, float b1) {}
        java.util.Map<String, java.util.function.Consumer<Canvas>> art = new java.util.LinkedHashMap<>();
        art.put("quench", c -> WeaponArt.quench(c, 0, 0.5F, -1));
        art.put("casimir", c -> WeaponArt.casimir(c, 0.5F, false, 3));
        art.put("reaver", c -> WeaponArt.reaver(c, 0, -1, 0));
        art.put("lance", c -> WeaponArt.lance(c, 0, -1, 0, -1));
        art.put("pick", io.github.genichimaruo.singuloarsenal.client.art.ToolArt::squidPick);
        art.put("breaker", io.github.genichimaruo.singuloarsenal.client.art.ToolArt::degeneracyBreaker);
        art.put("saw", c -> io.github.genichimaruo.singuloarsenal.client.art.ToolArt.shearSaw(c, false, 0));
        art.put("drill", c -> io.github.genichimaruo.singuloarsenal.client.art.ToolArt.accretionDrill(c, false, 0));
        StringBuilder out = new StringBuilder();
        art.forEach((name, draw) -> {
            List<float[]> points = new ArrayList<>();
            List<float[]> normals = new ArrayList<>();
            VertexConsumer sink = new VertexConsumer() {
                @Override public VertexConsumer vertex(double x, double y, double z) { points.add(new float[]{(float)x, (float)y, (float)z}); normals.add(new float[3]); return this; }
                @Override public VertexConsumer color(int r, int g, int b, int a) { return this; }
                @Override public VertexConsumer uv(float u, float v) { return this; }
                @Override public VertexConsumer overlayCoords(int u, int v) { return this; }
                @Override public VertexConsumer uv2(int u, int v) { return this; }
                @Override public VertexConsumer normal(float x, float y, float z) { normals.set(normals.size() - 1, new float[]{x, y, z}); return this; }
                @Override public void endVertex() {}
                @Override public void defaultColor(int r, int g, int b, int a) {}
                @Override public void unsetDefaultColor() {}
            };
            Canvas.drawFine(new PoseStack(), type -> sink, 0, 0, Canvas.noOverlay(), false, draw);
            List<Quad> quads = new ArrayList<>();
            for (int i = 0; i + 3 < points.size(); i += 4) {
                for (int axis = 0; axis < 3; axis++) {
                    int u = (axis + 1) % 3, v = (axis + 2) % 3;
                    float[][] q = {points.get(i), points.get(i + 1), points.get(i + 2), points.get(i + 3)};
                    if (Math.abs(q[0][axis] - q[1][axis]) + Math.abs(q[0][axis] - q[2][axis]) + Math.abs(q[0][axis] - q[3][axis]) > 1e-4F) continue;
                    float a0 = 1e9F, a1 = -1e9F, b0 = 1e9F, b1 = -1e9F;
                    for (float[] p : q) { a0 = Math.min(a0, p[u]); a1 = Math.max(a1, p[u]); b0 = Math.min(b0, p[v]); b1 = Math.max(b1, p[v]); }
                    // A pair facing opposite ways is a joint between two parts, hidden from every side.
                    float facing = normals.get(i)[axis];
                    if (facing != 0) quads.add(new Quad(axis, facing > 0 ? q[0][axis] : -q[0][axis] - 1000, a0, a1, b0, b1));
                }
            }
            java.util.Set<String> seen = new java.util.TreeSet<>();
            for (int i = 0; i < quads.size(); i++) for (int j = i + 1; j < quads.size(); j++) {
                Quad p = quads.get(i), q = quads.get(j);
                if (p.axis != q.axis || Math.abs(p.plane - q.plane) > 0.06F) continue;
                if (Math.min(p.a1, q.a1) - Math.max(p.a0, q.a0) < 0.02F || Math.min(p.b1, q.b1) - Math.max(p.b0, q.b0) < 0.02F) continue;
                seen.add("xyz".charAt(p.axis) + "=" + p.plane + " near " + "xyz".charAt((p.axis + 1) % 3) + Math.max(p.a0, q.a0) + " " + "xyz".charAt((p.axis + 2) % 3) + Math.max(p.b0, q.b0));
            }
            out.append(name).append(": ").append(seen.size()).append('\n');
            seen.forEach(line -> out.append("  ").append(line).append('\n'));
        });
        try {
            Files.writeString(Minecraft.getInstance().gameDirectory.toPath().resolve("screenshots/zfight.txt"), out);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    @SubscribeEvent
    public static void screen(net.minecraftforge.client.event.ScreenEvent.Render.Post event) {
        var mc = Minecraft.getInstance();
        if (bare) {
            // Every gear icon on a plain ground and without its charge bar, so it can be measured against the slot.
            var g = event.getGuiGraphics();
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF8B8B8B);
            List<Item> gear = ArsenalItems.powered();
            for (int i = 0; i < gear.size(); i++) g.renderItem(new ItemStack(gear.get(i)), 8 + 18 * (i % 9), 8 + 18 * (i / 9));
        }
        if (tip == null) return;
        event.getGuiGraphics().renderComponentTooltip(mc.font, net.minecraft.client.gui.screens.Screen.getTooltipFromItem(mc, tip), 40, 20);
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
        // Through use(), as a right click arrives on the server.
        step(12, () -> server(mc, o -> o.getMainHandItem().getItem().use(o.serverLevel(), o, InteractionHand.MAIN_HAND)));
        at += 35;
        shot("other-reaver-focus");
        at += 15;
        shot("other-reaver-focus-late");
        step(2, () -> server(mc, ServerPlayer::releaseUsingItem));
        at += 3;
        shot("other-reaver-shear");

        step(40, () -> server(mc, o -> hold(o, weapons().get(3))));
        // Four shots, each seen from another side; the fourth empties the magazine and starts the reload.
        step(2, () -> watch(mc, -100, 6));
        step(10, () -> server(mc, JetLanceItem::tryFire));
        at += 14;
        shot("other-lance-charging");
        at += 13;
        shot("other-lance-firing-lit");
        at += 12;
        shot("other-lance-firing-side");
        step(40, () -> watch(mc, -40, 6));
        step(4, () -> server(mc, JetLanceItem::tryFire));
        at += 42;
        shot("other-lance-firing-right");
        step(40, () -> watch(mc, 165, 6));
        step(4, () -> server(mc, JetLanceItem::tryFire));
        at += 42;
        shot("other-lance-firing-behind");
        step(40, () -> watch(mc, 30, 5));
        step(4, () -> server(mc, JetLanceItem::tryFire));
        at += 42;
        shot("other-lance-firing-front");
        at += 26;
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
        // Through the real left-click path: key, client tick, packet.
        step(2, () -> mc.options.keyAttack.setDown(true));
        step(2, () -> mc.options.keyAttack.setDown(false));
        at += 12;
        shot("self-lance-charging");
        at += 15;
        shot("self-lance-firing-lit");
        at += 12;
        shot("self-lance-firing");
        step(40, () -> { local(mc, JetLanceItem::tryFire); mc.options.setCameraType(CameraType.THIRD_PERSON_BACK); });
        at += 42;
        shot("self-lance-firing-third-back");
        step(40, () -> { local(mc, JetLanceItem::tryFire); mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT); });
        at += 42;
        shot("self-lance-firing-third-front");
        step(40, () -> { local(mc, JetLanceItem::tryFire); mc.options.setCameraType(CameraType.FIRST_PERSON); });
        at += 70;
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
        shot("other-bits-behind");
        at += 4;
        shot("other-bits-behind-2");
        step(2, () -> watch(mc, 180, 3.5));
        at += 8;
        shot("other-bits-rear");
        step(2, () -> local(mc, p -> {
            var level = p.serverLevel();
            BlockPos at = new BlockPos(3, 81, 3);
            level.setBlock(at, ModuleContent.INTEGRATOR.get().defaultBlockState(), 3);
            var entity = (IntegratorBlockEntity) level.getBlockEntity(at);
            entity.energy().receiveEnergy(400_000, false);
            entity.items().setStackInSlot(0, charged(ArsenalItems.JET_LANCE.get()));
            entity.items().setStackInSlot(1, new ItemStack(ModuleContent.MODULES.get(ModuleType.MELTDOWN).get()));
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
        step(4, () -> local(mc, p -> net.minecraftforge.network.NetworkHooks.openScreen(p, (IntegratorBlockEntity) p.serverLevel().getBlockEntity(new BlockPos(3, 81, 3)), new BlockPos(3, 81, 3))));
        at += 14;
        shot("integrator-screen");
        step(2, () -> mc.setScreen(null));
        step(2, () -> { selectSlot(mc, 3); teleport(mc, OX, OZ - 4, 0, 4); });
        at += 24;

        // A meltdown shot through a mixed hill: tunnel, deepest-point blast, stone melting and planks blowing apart.
        step(2, () -> server(mc, o -> {
            var level = o.serverLevel();
            for (int x = -6; x <= 6; x++) for (int y = 81; y <= 90; y++) for (int z = 22; z <= 32; z++) {
                level.setBlock(new BlockPos(x, y, z), (z < 27 ? Blocks.OAK_PLANKS : Blocks.STONE).defaultBlockState(), 3);
            }
            hold(o, weapons().get(3));
            ItemStack cannon = o.getMainHandItem();
            Modules.install(cannon, List.of(new ItemStack(ModuleContent.MODULES.get(ModuleType.MELTDOWN).get())));
            Fe.set(cannon, Fe.capacity(cannon));
            ArsenalData.set(cannon, ArsenalData.TERRAIN_MODE, 1);
        }));
        step(2, () -> teleport(mc, OX + 18, 36, 90, 2));
        step(20, () -> server(mc, JetLanceItem::tryFire));
        at += 30;
        shot("meltdown-1");
        at += 3;
        shot("meltdown-2");
        at += 6;
        shot("meltdown-3");
        at += 30;
        step(2, () -> teleport(mc, OX, 14, 0, 0));
        at += 20;
        shot("meltdown-tunnel");

        // The tools: held by another player from two sides, in the wearer's own hand, and the ore scan through the floor.
        String[] toolNames = {"pick", "breaker", "saw", "drill"};
        step(2, () -> { selectSlot(mc, 0); teleport(mc, OX, OZ - 4, 0, 4); });
        for (int i = 0; i < 4; i++) {
            int index = i;
            step(2, () -> server(mc, o -> hold(o, tools().get(index))));
            step(2, () -> watch(mc, -55, 3.2));
            at += 10;
            shot("other-tool-" + toolNames[i] + "-right");
            step(2, () -> watch(mc, 60, 3.2));
            at += 8;
            shot("other-tool-" + toolNames[i] + "-left");
        }
        step(2, () -> local(mc, p -> {
            for (int i = 0; i < 4; i++) p.getInventory().setItem(5 + i, charged(tools().get(i)));
            ArsenalData.set(p.getInventory().getItem(5), ArsenalData.SCAN_TIER, 3);
            for (int x = -3; x <= 3; x += 2) p.serverLevel().setBlock(new BlockPos(x, 80, -2), Blocks.DIAMOND_ORE.defaultBlockState(), 3);
            p.connection.teleport(OX, 81, -5, 0, 25);
        }));
        for (int i = 0; i < 4; i++) {
            int slot = 5 + i;
            step(4, () -> selectSlot(mc, slot));
            at += 26;
            shot("self-tool-" + toolNames[i]);
        }
        step(4, () -> selectSlot(mc, 5));
        at += 20;
        step(2, () -> mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
        at += 10;
        shot("self-tool-scan");

        // Every effect, on open ground in front of the camera.
        step(2, () -> teleport(mc, OX + 5, 6, 0, 8));
        at += 10;
        int[][] effects = {{FxPayload.RIPPLE, 1, 6}, {FxPayload.COLLAPSE, 5, 8}, {FxPayload.QUENCH, 4, 6}, {FxPayload.HORIZON, 8, 16}, {FxPayload.SHEAR, 8, 7}, {FxPayload.MELTDOWN, 7, 6}, {FxPayload.MELTDOWN, 7, 12}};
        for (int[] effect : effects) {
            step(30, () -> server(mc, o -> FxPayload.send(o.serverLevel(), effect[0], new net.minecraft.world.phys.Vec3(OX, 82.5, 18), new net.minecraft.world.phys.Vec3(0, 0, -1), effect[1])));
            at += effect[2];
            shot("fx-" + effect[0] + "-" + effect[2]);
        }

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
    public static void serverTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        // A mock connection has nothing to pump this player, so step it the way a real connection would.
        if (other != null) other.doTick();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
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
    public static void render(TickEvent.RenderTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (capture != null) {
            var directory = mc.gameDirectory.toPath().resolve("screenshots");
            Files.createDirectories(directory);
            try (var shot = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                shot.writeToFile(directory.resolve(capture + ".png").toFile());
            }
            System.out.println("ARSENAL_REVIEW_CAPTURE " + capture);
            capture = null;
        }
        if (done) mc.stop();
    }
}
