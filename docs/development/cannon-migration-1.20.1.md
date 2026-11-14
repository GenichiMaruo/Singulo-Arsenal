# Cannon migration on Minecraft 1.20.1

Arsenal 0.3.0 registers `singulo_arsenal:jet_cannon`. During the item register event, ForgeRegistry.addAlias maps the historical `singulo_arsenal:jet_lance` to that item. The official MissingMappingsEvent additionally remaps the old saved registry entry to the cannon so old numeric mappings are not discarded. Both Forge 47.4.26 and NeoForge 47.1.106 use the legacy Forge registry API. This is not a second item registration and does not duplicate creative entries or recipes.

ItemStack loading resolves the old item identity without rewriting its `tag` payload. FE, magazine, modules, custom names, and unknown third-party NBT remain attached. Normal saving writes the canonical new ID. The recipe and advancement IDs remain `singulo_arsenal:jet_lance` and `singulo_arsenal:recipes/jet_lance`, preserving unlock history; only their result item changes.

The integration tests cover stack, player inventory/main hand/offhand, chest, frame, dropped entity, legacy recipe ingredient/result, canonical resaving, and complete NBT equality. The opt-in `tools/review/migration.gradle` harness additionally seeds a real 0.2.2 world and checks seven stored fixtures (including an armor stand), saved recipe/advancement progress, and an external old-ID recipe after opening and resaving in 0.3.0. External item tags require the explicit update below; the original failing tag evidence is retained separately, not counted as a successful migration. These test sources never ship in the release JAR.

Back up a world before upgrading. Loading the upgraded save in 0.2.2 is unsupported: 0.2.2 does not know `jet_cannon`, so restore the pre-upgrade backup to downgrade. External mods that compare raw ID strings, bypass the Forge registry lookup, or maintain their own caches may need to update their references. An alias is not a universal rewrite of arbitrary strings or third-party formats. This upgrade does not convert Minecraft 1.20.1 NBT to 1.21.1 data components.

The custom nihonium tool tier uses its own empty `singulo_arsenal:needs_nihonium_tool` requirement tag. Reusing `minecraft:needs_diamond_tool` for a tier sorted after netherite would incorrectly block vanilla diamond/netherite picks. Regression tests explicitly protect diamond/netherite obsidian drops and iron rejection.

## External datapack item-tag limitation (1.20.1 only)

Forge/NeoForge 47 item-tag loading uses NamespacedWrapper.getHolder, which does not follow ForgeRegistry aliases. Thus an item-tag entry containing `singulo_arsenal:jet_lance` is not migrated automatically. Recipes depending on such a failed tag also need the tag fixed. This is separate from direct recipe ingredient/result IDs and saved ItemStack NBT, which follow the alias.

Before upgrading, back up the world and datapacks. In each datapack, change item entries under `data/<namespace>/tags/items/*.json` from `singulo_arsenal:jet_lance` to `singulo_arsenal:jet_cannon` (including the `id` of optional object entries). Keep the tag resource ID and recipe/advancement IDs unchanged. Direct old-ID recipe ingredients/results remain supported. Restart/reload and check that no missing tag reference is reported.

The real-world verification preserves an untouched old-version seed and the initial failed-tag run, then updates only the tag entry in a separate test copy. Seven storage fixtures, FE, magazine, modules, recipe/advancement progress, direct old-ID recipe ingredient/result, and a second normal resave are verified on that copy. No user datapack is rewritten automatically, no duplicate old item is registered, and no global holder-map patch is installed.
