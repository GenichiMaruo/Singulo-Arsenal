package io.github.genichimaruo.singuloarsenal.gametest;

import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.module.Modules;
import io.github.genichimaruo.singuloarsenal.nihonium.Nihonium;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** The plain metal set must not accidentally acquire powered-gear behavior. */
@GameTestHolder(SinguloArsenal.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NihoniumGameTests {
    private NihoniumGameTests() {}
    private static List<Item> tools() {
        return List.of(Nihonium.SWORD.get(), Nihonium.PICKAXE.get(), Nihonium.AXE.get(), Nihonium.SHOVEL.get());
    }
    private static TagKey<Item> tag(String name) {
        return TagKey.create(Registries.ITEM, ResourceLocation.withDefaultNamespace(name));
    }

    @GameTest(template = "empty")
    public static void nihoniumRepairsWithRealSinguloAndHasNoPower(GameTestHelper h) {
        ItemStack ingot = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("singulo:nihonium_ingot")));
        h.assertTrue(!ingot.isEmpty() && ingot.is(Nihonium.INGOT), "Singulo nihonium repair tag is empty");
        List<Item> all = new ArrayList<>(tools());
        Nihonium.ARMOR.values().forEach(item -> all.add(item.get()));
        for (Item item : all) {
            ItemStack stack = new ItemStack(item);
            h.assertTrue(stack.isDamageableItem() && stack.isEnchantable(), "plain gear must have durability/enchantability");
            h.assertTrue(item.isValidRepairItem(stack, ingot), "nihonium ingot cannot repair " + item);
            h.assertTrue(Fe.capacity(stack) == 0 && stack.getCapability(Capabilities.EnergyStorage.ITEM) == null,
                    "plain gear unexpectedly exposes FE: " + item);
            h.assertTrue(Modules.slots(stack) == 0, "plain gear unexpectedly accepts modules: " + item);
            h.assertTrue(stack.is(tag("enchantable/durability")), "plain gear cannot receive durability enchantments: " + item);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nihoniumToolsKeepTheirTierAndDurability(GameTestHelper h) {
        for (Item item : tools()) h.assertTrue(new ItemStack(item).getMaxDamage() == 4000, "tool durability changed");
        ItemStack pick = new ItemStack(Nihonium.PICKAXE.get());
        h.assertTrue(pick.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()), "pick cannot harvest diamond-tier blocks");
        h.assertTrue(pick.getDestroySpeed(Blocks.STONE.defaultBlockState()) == 10F, "pick mining speed changed");
        h.assertTrue(!new ItemStack(Nihonium.SHOVEL.get()).isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState()),
                "shovel incorrectly mines obsidian");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void nihoniumArmorRetainsVanillaTrimsAndStats(GameTestHelper h) {
        int defense = 0;
        for (var entry : Nihonium.ARMOR.entrySet()) {
            ArmorItem item = entry.getValue().get();
            ItemStack stack = new ItemStack(item);
            h.assertTrue(stack.getMaxDamage() == entry.getKey().getDurability(60), "armor durability changed");
            h.assertTrue(stack.is(tag("trimmable_armor")), "plain armor lost vanilla trim eligibility");
            h.assertTrue(item.getToughness() == 2.5F, "armor toughness changed");
            defense += item.getDefense();
        }
        h.assertTrue(defense == 21, "full plain armor defense changed");
        h.succeed();
    }
}
