package io.github.genichimaruo.singuloarsenal.review;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.event.TickEvent;

/** Opt-in 1.20.1 harness shared unchanged by old/new Arsenal. Never run in a user's world. */
@EventBusSubscriber(modid = "singulo_arsenal", value = Dist.CLIENT)
public final class CannonWorldMigration {
    private static final ResourceLocation OLD = id("jet_lance"), NEW = id("jet_cannon");
    private static final ResourceLocation RECIPE = id("jet_lance"), ADVANCEMENT = id("recipes/jet_lance");
    private static final ResourceLocation PACK_RECIPE = new ResourceLocation("migration_test", "legacy_cannon");
    private static final net.minecraft.tags.TagKey<Item> PACK_TAG = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, new ResourceLocation("migration_test", "legacy_cannons"));
    private static final BlockPos CHEST = new BlockPos(3, 81, 3), FRAME = new BlockPos(4, 82, 4);
    private static final String[] LABELS = {"main", "off", "inventory", "chest", "frame", "drop", "armorstand"};
    private static int ticks, attempts;
    private static volatile boolean queued, finished;
    private static volatile Throwable failure;
    private static Path directory;
    private static String phase;

    private static ResourceLocation id(String path) { return new ResourceLocation("singulo_arsenal", path); }
    private static UUID uuid(String name) { return UUID.nameUUIDFromBytes(("arsenal-migration-" + name).getBytes(StandardCharsets.UTF_8)); }
    private static void require(boolean condition, String message) { if (!condition) throw new IllegalStateException(message); }

    private static Item item(ResourceLocation name) {
        Item found = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(name);
        require(found != null && found != net.minecraft.world.item.Items.AIR, "Missing item " + name);
        return found;
    }

    private static ItemStack fixture(Item item, int index) {
        ItemStack stack = new ItemStack(item);
        Modules.install(stack, List.of(new ItemStack(item(id("capacitor_module")))));
        ArsenalData.set(stack, ArsenalData.ENERGY, 123456 + index);
        ArsenalData.set(stack, ArsenalData.SHOTS, 2);
        ArsenalData.set(stack, ArsenalData.TERRAIN_MODE, 1);
        stack.setHoverName( Component.literal("migration-" + LABELS[index]));
        return stack;
    }

    private static void inspect(ServerPlayer player, ItemStack stack, int index, ResourceLocation canonical) {
        String label = LABELS[index];
        require(!stack.isEmpty() && stack.getCount() == 1, label + " missing/count");
        require(BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(canonical), label + " canonical ID");
        require(ArsenalData.get(stack, ArsenalData.ENERGY) == 123456 + index, label + " FE changed");
        require(ArsenalData.get(stack, ArsenalData.SHOTS) == 2, label + " magazine changed");
        require(ArsenalData.get(stack, ArsenalData.TERRAIN_MODE) == 1, label + " terrain mode changed");
        require(Component.literal("migration-" + label).equals(stack.getHoverName()), label + " name changed");
        var modules = Modules.installed(stack);
        require(modules.size() == 1 && modules.get(0).getCount() == 1 && modules.get(0).is(item(id("capacitor_module"))), label + " modules changed");
        var tag = (net.minecraft.nbt.CompoundTag) stack.save(new net.minecraft.nbt.CompoundTag());
        require(tag.getString("id").equals(canonical.toString()), label + " save did not canonicalize");
        System.out.println("ARSENAL_MIGRATION_STACK " + phase + " " + label + " " + tag);
    }

    private static void seed(ServerPlayer p) {
        var level = p.serverLevel();
        require(!Files.exists(directory.resolve("seed.ok")), "Refusing to reseed an existing migration fixture");
        require(BuiltInRegistries.ITEM.containsKey(OLD) && !BuiltInRegistries.ITEM.containsKey(NEW), "Seed must run against the old item registration");
        for (String name : List.of("frame", "drop", "armorstand")) require(level.getEntity(uuid(name)) == null, "Fixture already exists: " + name);
        p.setGameMode(GameType.CREATIVE);
        p.getInventory().clearContent(); p.getInventory().selected = 0;
        for (int x = 1; x <= 7; x++) for (int z = 1; z <= 7; z++) {
            level.setBlock(new BlockPos(x, 80, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            for (int y = 81; y <= 85; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        }
        p.connection.teleport(2.5, 81, 2.5, 0, 0);
        Item cannon = item(OLD);
        p.setItemSlot(EquipmentSlot.MAINHAND, fixture(cannon, 0));
        p.setItemSlot(EquipmentSlot.OFFHAND, fixture(cannon, 1));
        p.getInventory().setItem(9, fixture(cannon, 2));
        level.setBlock(CHEST, Blocks.CHEST.defaultBlockState(), 3);
        var chest = (ChestBlockEntity) level.getBlockEntity(CHEST);
        require(chest != null, "Chest not created"); chest.setItem(3, fixture(cannon, 3)); chest.setChanged();
        level.setBlock(FRAME.relative(Direction.SOUTH), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        ItemFrame frame = new ItemFrame(level, FRAME, Direction.NORTH);
        frame.setUUID(uuid("frame")); frame.setItem(fixture(cannon, 4)); frame.setInvulnerable(true);
        require(frame.survives() && level.addFreshEntity(frame), "Frame not created");
        ItemEntity drop = new ItemEntity(level, 6.5, 81.5, 4.5, fixture(cannon, 5));
        drop.setUUID(uuid("drop")); drop.setNoGravity(true); drop.setDeltaMovement(0, 0, 0);
        drop.setNeverPickUp(); drop.setUnlimitedLifetime(); drop.setInvulnerable(true);
        require(level.addFreshEntity(drop), "Drop not created");
        ArmorStand stand = EntityType.ARMOR_STAND.create(level);
        require(stand != null, "Stand not created"); stand.setUUID(uuid("armorstand"));
        stand.moveTo(6.5, 81, 6.5, 180, 0); stand.setNoGravity(true); stand.setInvulnerable(true);
        stand.setItemSlot(EquipmentSlot.MAINHAND, fixture(cannon, 6));
        require(level.addFreshEntity(stand), "Stand not added");
        var recipe = p.server.getRecipeManager().byKey(RECIPE).orElseThrow(() -> new IllegalStateException("Missing historical recipe"));
        var packRecipe = p.server.getRecipeManager().byKey(PACK_RECIPE).orElseThrow(() -> new IllegalStateException("Missing test datapack recipe"));
        p.awardRecipes(List.of(recipe, packRecipe));
        var advancement = p.server.getAdvancements().getAdvancement(ADVANCEMENT);
        require(advancement != null, "Missing historical advancement");
        for (String criterion : advancement.getCriteria().keySet()) p.getAdvancements().award(advancement, criterion);
        p.inventoryMenu.broadcastChanges();
    }

    private static boolean entitiesReady(ServerPlayer p) {
        p.serverLevel().getChunk(0, 0);
        for (String name : List.of("frame", "drop", "armorstand")) if (p.serverLevel().getEntity(uuid(name)) == null) return false;
        return true;
    }

    private static void verify(ServerPlayer p, ResourceLocation canonical) {
        var level = p.serverLevel();
        inspect(p, p.getMainHandItem(), 0, canonical);
        inspect(p, p.getOffhandItem(), 1, canonical);
        inspect(p, p.getInventory().getItem(9), 2, canonical);
        require(level.getBlockEntity(CHEST) instanceof ChestBlockEntity, "Chest missing after reload");
        inspect(p, ((ChestBlockEntity) level.getBlockEntity(CHEST)).getItem(3), 3, canonical);
        Entity f = level.getEntity(uuid("frame")), d = level.getEntity(uuid("drop")), a = level.getEntity(uuid("armorstand"));
        require(f instanceof ItemFrame && d instanceof ItemEntity && a instanceof ArmorStand, "Fixture entity types changed");
        inspect(p, ((ItemFrame) f).getItem(), 4, canonical);
        inspect(p, ((ItemEntity) d).getItem(), 5, canonical);
        inspect(p, ((ArmorStand) a).getMainHandItem(), 6, canonical);
        require(p.getRecipeBook().contains(RECIPE), "Historical recipe unlock lost");
        require(p.getRecipeBook().contains(PACK_RECIPE), "Test datapack recipe unlock lost");
        var packRecipe = p.server.getRecipeManager().byKey(PACK_RECIPE).orElseThrow(() -> new IllegalStateException("Test datapack recipe missing"));
        ItemStack probe = new ItemStack(item(canonical));
        require(probe.is(PACK_TAG), "Old item ID did not load into test datapack tag");
        require(packRecipe.getResultItem(p.level().registryAccess()).is(item(canonical)), "Old recipe result did not resolve");
        require(packRecipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(probe)), "Old recipe ingredient did not resolve");
        var advancement = p.server.getAdvancements().getAdvancement(ADVANCEMENT);
        require(advancement != null && p.getAdvancements().getOrStartProgress(advancement).isDone(), "Historical advancement progress lost");
        if (!canonical.equals(OLD)) {
            require(item(OLD) == item(NEW), "Old alias does not resolve to canonical item");
            require(!net.minecraftforge.registries.ForgeRegistries.ITEMS.getKeys().contains(OLD), "Old ID is still a separate registration");
        }
    }

    private static void work(Minecraft mc) {
        try {
            var server = mc.getSingleplayerServer();
            var world = server.getWorldPath(LevelResource.ROOT).toRealPath();
            require(world.equals(directory.resolve("saves/Review").toRealPath()), "Refusing non-Review world: " + world);
            var p = server.getPlayerList().getPlayer(mc.player.getUUID());
            require(p != null && p.serverLevel() == server.overworld(), "Expected local overworld player");
            if (phase.equals("seed")) seed(p);
            else {
                require(Files.exists(directory.resolve("seed.ok")), "Copy seed.ok with the seeded world");
                if (phase.equals("verify-resaved")) require(Files.exists(directory.resolve("verify.ok")), "First verify phase has not passed");
                require(BuiltInRegistries.ITEM.containsKey(NEW), "Verify must run against new registration");
                if (!entitiesReady(p)) {
                    require(++attempts <= 20, "Persisted fixture entities not loaded after 20 attempts");
                    queued = false; return;
                }
            }
            verify(p, phase.equals("seed") ? OLD : NEW);
            server.getPlayerList().saveAll();
            server.saveEverything(true, true, true);
            Files.writeString(directory.resolve(phase + ".ok"), "phase=" + phase + "\nplayer=" + p.getUUID() + "\nfixtures=7\nrecipe=" + RECIPE + "\nadvancement=" + ADVANCEMENT + "\n");
            System.out.println("ARSENAL_MIGRATION_OK " + phase + " fixtures=7 recipe+advancement preserved; saved");
            finished = true;
            mc.execute(mc::stop); // Normal client shutdown also stops/saves the integrated server.
        } catch (VirtualMachineError | ThreadDeath fatal) { throw fatal; }
        catch (Throwable e) { failure = e; }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getInstance();
        if (failure != null) throw new IllegalStateException("ARSENAL_MIGRATION_FAILED " + phase, failure);
        if (finished) return;
        if (++ticks > 2400) throw new IllegalStateException("ARSENAL_MIGRATION_TIMEOUT " + phase);
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null || mc.getOverlay() != null) return;
        if (queued || ticks % 20 != 0) return;
        try {
            directory = mc.gameDirectory.toPath().toRealPath();
            require(directory.getFileName().toString().equals("gui-migration") && directory.getParent().getFileName().toString().equals("build"), "Refusing non-disposable game directory " + directory);
            // Mute only background music in this disposable client, never master/effects volume.
            mc.options.getSoundSourceOptionInstance(net.minecraft.sounds.SoundSource.MUSIC).set(0.0D);
            require(mc.options.getSoundSourceVolume(net.minecraft.sounds.SoundSource.MUSIC) == 0.0F, "Migration BGM must be zero");
            phase = Files.readString(directory.resolve("phase.txt")).trim();
            require(List.of("seed", "verify", "verify-resaved").contains(phase), "Unknown phase " + phase);
            queued = true;
            mc.getSingleplayerServer().execute(() -> work(mc));
        } catch (java.io.IOException e) { throw new java.io.UncheckedIOException(e); }
    }
}
