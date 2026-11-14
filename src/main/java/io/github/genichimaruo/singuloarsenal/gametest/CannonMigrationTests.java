package io.github.genichimaruo.singuloarsenal.gametest;

import com.google.gson.JsonParser;
import io.github.genichimaruo.singuloarsenal.ArsenalItems;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.module.*;
import io.github.genichimaruo.singuloarsenal.nihonium.Nihonium;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

/** Legacy 1.20.1 NBT and registry migration regression tests, excluded from production jars. */
@GameTestHolder(SinguloArsenal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CannonMigrationTests {
    private static final String OLD = "singulo_arsenal:jet_lance", NEW = "singulo_arsenal:jet_cannon";
    private static ItemStack fixture() {
        ItemStack stack = new ItemStack(ArsenalItems.JET_CANNON.get());
        Modules.install(stack, List.of(new ItemStack(ModuleContent.MODULES.get(ModuleType.CAPACITOR).get()),
                new ItemStack(ModuleContent.MODULES.get(ModuleType.MELTDOWN).get())));
        Fe.set(stack, 1234567); ArsenalData.set(stack, ArsenalData.SHOTS, 2);
        ArsenalData.set(stack, ArsenalData.TERRAIN_MODE, 1);
        ArsenalData.set(stack, ArsenalData.RELOAD_UNTIL, 123456L);
        ArsenalData.set(stack, ArsenalData.RELOAD_TICKS, 160);
        ArsenalData.set(stack, ArsenalData.CHARGE_UNTIL, 123400L);
        ArsenalData.set(stack, ArsenalData.JET_UNTIL, 123440L);
        stack.setHoverName(Component.literal("Migration sentinel"));
        stack.getOrCreateTag().putString("external_owner", "unchanged");
        return stack;
    }
    private static void legacyIds(Tag tag) {
        if (tag instanceof CompoundTag compound) {
            if (NEW.equals(compound.getString("id"))) compound.putString("id", OLD);
            for (String key : compound.getAllKeys()) legacyIds(compound.get(key));
        } else if (tag instanceof ListTag list) for (Tag child : list) legacyIds(child);
    }
    private static void same(GameTestHelper h, ItemStack actual, ItemStack expected, String where) {
        h.assertTrue(ItemStack.matches(actual, expected), where + ": item/count/NBT changed");
        h.assertTrue(Fe.stored(actual) == 1234567 && ArsenalData.get(actual, ArsenalData.SHOTS) == 2, where + ": FE/magazine lost");
        h.assertTrue(Modules.count(actual, ModuleType.MELTDOWN) == 1, where + ": module lost");
        CompoundTag saved = actual.save(new CompoundTag());
        h.assertTrue(NEW.equals(saved.getString("id")), where + ": resave not canonical");
        h.assertTrue(ItemStack.matches(ItemStack.of(saved), expected), where + ": canonical reload changed NBT");
    }
    @GameTest(template = "empty")
    public static void legacyCannonStack(GameTestHelper h) {
        ItemStack expected = fixture(); CompoundTag saved = expected.save(new CompoundTag()); legacyIds(saved);
        h.assertTrue(ForgeRegistries.ITEMS.getValue(SinguloArsenal.id("jet_lance")) == ArsenalItems.JET_CANNON.get(), "alias lookup failed");
        same(h, ItemStack.of(saved), expected, "stack"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void legacyCannonPlayer(GameTestHelper h) {
        var p = h.makeMockServerPlayerInLevel(); ItemStack expected = fixture();
        p.getInventory().selected = 2; p.getInventory().setItem(2, expected.copy()); p.getInventory().setItem(8, expected.copy());
        p.setItemSlot(EquipmentSlot.OFFHAND, expected.copy());
        ListTag saved = p.getInventory().save(new ListTag()); legacyIds(saved); p.getInventory().clearContent(); p.getInventory().load(saved);
        same(h, p.getMainHandItem(), expected, "main"); same(h,p.getOffhandItem(),expected,"off"); same(h,p.getInventory().getItem(8),expected,"inventory"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void legacyCannonChest(GameTestHelper h) {
        BlockPos pos = h.absolutePos(new BlockPos(1,1,1)); ItemStack expected=fixture();
        ChestBlockEntity chest=new ChestBlockEntity(pos,Blocks.CHEST.defaultBlockState()); chest.setItem(17,expected.copy());
        CompoundTag saved=chest.saveWithoutMetadata(); legacyIds(saved);
        ChestBlockEntity restored=new ChestBlockEntity(pos,Blocks.CHEST.defaultBlockState()); restored.load(saved);
        same(h,restored.getItem(17),expected,"chest"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void legacyCannonFrame(GameTestHelper h) {
        ItemStack expected=fixture(); ItemFrame frame=new ItemFrame(h.getLevel(),h.absolutePos(new BlockPos(2,2,2)),Direction.NORTH);
        frame.setItem(expected.copy(),false); frame.setRotation(5); CompoundTag saved=new CompoundTag(); frame.saveWithoutId(saved); legacyIds(saved);
        ItemFrame restored=EntityType.ITEM_FRAME.create(h.getLevel()); restored.load(saved); same(h,restored.getItem(),expected,"frame");
        h.assertTrue(restored.getRotation()==5 && restored.getDirection()==Direction.NORTH,"frame placement changed"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void legacyCannonDrop(GameTestHelper h) {
        ItemStack expected=fixture(); ItemEntity entity=new ItemEntity(h.getLevel(),0,0,0,expected.copy()); CompoundTag saved=new CompoundTag();
        entity.saveWithoutId(saved); legacyIds(saved); ItemEntity restored=EntityType.ITEM.create(h.getLevel()); restored.load(saved);
        same(h,restored.getItem(),expected,"drop"); h.succeed();
    }
    @GameTest(template = "empty")
    public static void legacyCannonRecipe(GameTestHelper h) {
        var json=JsonParser.parseString("{\"item\":\""+OLD+"\"}").getAsJsonObject();
        h.assertTrue(Ingredient.fromJson(json).test(fixture()),"legacy ingredient failed");
        h.assertTrue(ShapedRecipe.itemStackFromJson(json).is(ArsenalItems.JET_CANNON.get()),"legacy recipe result failed");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(SinguloArsenal.id("jet_lance")).isPresent(),"legacy recipe ID missing"); h.succeed();
    }
}
