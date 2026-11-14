package io.github.genichimaruo.singuloarsenal.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.module.ModuleType;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Real registry/codecs and container serializers; fixtures model 0.2.2's saved item ID. */
@GameTestHolder(SinguloArsenal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CannonMigrationTests {
    private static final String OLD = "singulo_arsenal:jet_lance";
    private static final String NEW = "singulo_arsenal:jet_cannon";
    private CannonMigrationTests() {}

    private static ItemStack fixture() {
        ItemStack stack = new ItemStack(ArsenalItems.JET_CANNON.get());
        Modules.install(stack, List.of(new ItemStack(ModuleContent.MODULES.get(ModuleType.CAPACITOR).get()),
                new ItemStack(ModuleContent.MODULES.get(ModuleType.MELTDOWN).get())));
        Fe.set(stack, 1234567);
        stack.set(ArsenalData.SHOTS.get(), 2);
        stack.set(ArsenalData.TERRAIN_MODE.get(), 1);
        stack.set(ArsenalData.RELOAD_UNTIL.get(), 123456L);
        stack.set(ArsenalData.RELOAD_TICKS.get(), 160);
        stack.set(ArsenalData.CHARGE_UNTIL.get(), 123400L);
        stack.set(ArsenalData.JET_UNTIL.get(), 123440L);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("Migration sentinel"));
        CompoundTag custom = new CompoundTag();
        custom.putString("external_owner", "unchanged");
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(custom));
        return stack;
    }

    /** Only rewrite the item identity, never any component payload. */
    private static void legacyIds(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            if (NEW.equals(compound.getString("id"))) compound.putString("id", OLD);
            for (String key : compound.getAllKeys()) legacyIds(compound.get(key));
        } else if (tag instanceof ListTag list) for (Tag child : list) legacyIds(child);
    }

    private static void same(GameTestHelper h, ItemStack actual, ItemStack expected, String where) {
        h.assertTrue(ItemStack.matches(actual, expected), where + ": item/count/components changed");
        h.assertTrue(Fe.stored(actual) == 1234567, where + ": FE lost");
        h.assertTrue(ArsenalData.get(actual, ArsenalData.SHOTS) == 2, where + ": magazine lost");
        h.assertTrue(Modules.count(actual, ModuleType.MELTDOWN) == 1, where + ": module lost");
        CompoundTag saved = (CompoundTag) actual.save(h.getLevel().registryAccess());
        h.assertTrue(NEW.equals(saved.getString("id")), where + ": resave not canonical");
        h.assertTrue(ItemStack.matches(ItemStack.parse(h.getLevel().registryAccess(), saved).orElseThrow(), expected),
                where + ": canonical reload changed state");
    }

    @GameTest(template = "empty")
    public static void legacyCannonStackCodec(GameTestHelper h) {
        ItemStack expected = fixture();
        CompoundTag old = (CompoundTag) expected.save(h.getLevel().registryAccess());
        old.putString("id", OLD);
        h.assertTrue(BuiltInRegistries.ITEM.get(ResourceLocation.parse(OLD)) == ArsenalItems.JET_CANNON.get(), "alias lookup failed");
        same(h, ItemStack.parse(h.getLevel().registryAccess(), old).orElseThrow(), expected, "stack");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyCannonPlayerInventory(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        ItemStack expected = fixture();
        player.getInventory().selected = 2;
        player.getInventory().setItem(2, expected.copy());
        player.getInventory().setItem(8, expected.copy());
        player.setItemSlot(EquipmentSlot.OFFHAND, expected.copy());
        ListTag saved = player.getInventory().save(new ListTag());
        legacyIds(saved);
        player.getInventory().clearContent();
        player.getInventory().load(saved);
        same(h, player.getMainHandItem(), expected, "selected hand");
        same(h, player.getOffhandItem(), expected, "offhand");
        same(h, player.getInventory().getItem(8), expected, "inventory slot");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyCannonChest(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1, 1, 1));
        ChestBlockEntity chest = new ChestBlockEntity(pos, Blocks.CHEST.defaultBlockState());
        ItemStack expected = fixture();
        chest.setItem(17, expected.copy());
        CompoundTag saved = chest.saveWithoutMetadata(h.getLevel().registryAccess());
        legacyIds(saved);
        ChestBlockEntity restored = new ChestBlockEntity(pos, Blocks.CHEST.defaultBlockState());
        restored.loadWithComponents(saved, h.getLevel().registryAccess());
        same(h, restored.getItem(17), expected, "chest");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyCannonFrame(GameTestHelper h) {
        ItemStack expected = fixture();
        ItemFrame frame = new ItemFrame(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2)), Direction.NORTH);
        frame.setItem(expected.copy(), false);
        frame.setRotation(5);
        CompoundTag saved = new CompoundTag();
        frame.saveWithoutId(saved);
        legacyIds(saved);
        ItemFrame restored = EntityType.ITEM_FRAME.create(h.getLevel());
        restored.load(saved);
        same(h, restored.getItem(), expected, "frame");
        h.assertTrue(restored.getRotation() == 5 && restored.getDirection() == Direction.NORTH, "frame placement changed");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyCannonDroppedEntity(GameTestHelper h) {
        ItemStack expected = fixture();
        ItemEntity entity = new ItemEntity(h.getLevel(), 0, 0, 0, expected.copy());
        CompoundTag saved = new CompoundTag();
        entity.saveWithoutId(saved);
        legacyIds(saved);
        ItemEntity restored = EntityType.ITEM.create(h.getLevel());
        restored.load(saved);
        same(h, restored.getItem(), expected, "dropped item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void legacyCannonDatapackCodecs(GameTestHelper h) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var parsed = new net.minecraft.commands.arguments.item.ItemParser(h.getLevel().registryAccess())
                .parse(new com.mojang.brigadier.StringReader(OLD + "[singulo_arsenal:energy=1234567,singulo_arsenal:shots=2]"));
        h.assertTrue(parsed.item().value() == ArsenalItems.JET_CANNON.get(), "old command item ID failed");
        ItemStack commandStack = new ItemStack(parsed.item(), 1, parsed.components());
        h.assertTrue(Fe.stored(commandStack) == 1234567 && ArsenalData.get(commandStack, ArsenalData.SHOTS) == 2,
                "old command item components changed");
        var ops = RegistryOps.create(JsonOps.INSTANCE, h.getLevel().registryAccess());
        Ingredient ingredient = Ingredient.CODEC_NONEMPTY.parse(ops,
                JsonParser.parseString("{\"item\":\"" + OLD + "\"}")).getOrThrow();
        h.assertTrue(ingredient.test(fixture()), "old datapack ingredient no longer matches");
        Recipe<?> recipe = Recipe.CODEC.parse(ops, JsonParser.parseString(
                "{\"type\":\"minecraft:crafting_shapeless\",\"ingredients\":[{\"item\":\"" + OLD
                + "\"}],\"result\":{\"id\":\"" + OLD + "\",\"count\":1}}")).getOrThrow();
        h.assertTrue(recipe.getResultItem(h.getLevel().registryAccess()).is(ArsenalItems.JET_CANNON.get()), "old recipe result lost");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ResourceLocation.parse(OLD)).isPresent(), "historical recipe book ID lost");
        h.succeed();
    }
}
