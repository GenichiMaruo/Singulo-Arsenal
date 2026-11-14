# Jet cannon identity migration (1.21.1 development)

## Persistent contract

The canonical item ID changes from `singulo_arsenal:jet_lance` to
`singulo_arsenal:jet_cannon`. `DeferredRegister.Items.addAlias(old, new)` is
registered before the item RegisterEvent. This is one item with a legacy lookup
alias, not two registered items or a ticking inventory converter. Reading a
legacy stack resolves its item; the existing components are not rewritten.
Subsequent normal saves emit the canonical new ID.

Java names (`JetCannonItem`, `JET_CANNON`, cannon rendering helpers and constants),
model path and current translation keys now use cannon. Display text remains
**Accretion Jet Cannon / 降着ジェット砲**. Historical translation keys remain
available for saved text components that explicitly refer to them.

Recipe `singulo_arsenal:jet_lance` and advancement
`singulo_arsenal:recipes/jet_lance` deliberately retain their historical IDs.
The recipe produces the new item. This preserves recipe-book unlocks, advancement
completion and existing datapack overrides without introducing duplicate recipes.

## Compatibility boundaries

- Back up the entire world before upgrading. This is forward migration, not a
  downgrade feature. After a new save, 0.2.2 does not know `jet_cannon`; restore
  the pre-upgrade backup if reverting.
- Standard registry lookups, saved stacks, ingredient/result codecs and command
  item parsing accept the old ID. Aliases are not separate registry entries:
  enumeration, autocomplete and `containsKey(old)` need not show/accept it.
- Scripts comparing raw saved NBT strings must accept the canonical new ID.
  Arbitrary third-party code that bypasses registry resolution is not covered.
- Resource packs overriding the old model path or only the old translation key
  should update to `jet_cannon`. Registry aliases do not alias asset paths.
- Clients and server must use matching mod versions. This does not promise that
  an old server's network registry snapshot works with a new client.
- Other mods compiled against the renamed Java implementation classes/constants
  need to update. No duplicate legacy item is registered as an API shim.
- This implementation and its tests are for Minecraft 1.21.1 / NeoForge 21.1.256.
  Forge/NeoForge 1.20.1 is not ported or validated by this branch.

## Automated regression coverage

Six additional GameTests run alongside the existing 56. They exercise the real
registry and serializers using legacy-ID data: stack load/canonical save/reload;
selected inventory slot and offhand; chest; frame facing/rotation; dropped entity;
command parsing; datapack ingredient/recipe codecs and stable recipe identity.
All component data are compared, including FE, magazine, modules, mode, charge,
jet/reload times, custom name and third-party custom data. These codec fixtures
are separate from the real old-version world upgrade exercise below.

## Actual world upgrade exercise

The opt-in migration client uses a disposable world, never the user's worlds.
Run the same version-neutral harness against 0.2.2 to seed and save, then against
0.3.0 to verify/save and start once more to verify the canonical resave.
Only music is muted; effects and ordinary game/OS settings are untouched.
The completed run results are recorded after verification, not inferred from
codec success.

### Reproduce

1. Prepare a new copy with `python tools/review/prepare_migration.py
   --source-world=<old-world-copy>`. The script refuses to overwrite a prior run.
2. In a separate 0.2.2 checkout, run `gradlew -I <new-checkout>/tools/review/migration.gradle
   runClient -PmigrationSourceDir=<new-checkout>/tools/review/migration-src
   -PmigrationGameDir=<new-checkout>/build/gui-migration
   -PsinguloJar=<released-Singulo-0.18.0-1.21.1N.jar>`.
3. Require `ARSENAL_MIGRATION_OK seed`, `seed.ok` and successful normal exit.
   Back up `saves/Review` before continuing. Keep the phase receipts.
4. Change `phase.txt` to `verify` and run the same init script/client against
   0.3.0 using the same game directory. Require its successful receipt/exit.
5. Change the phase to `verify-resaved` and run 0.3.0 once more. Require all
   seven fixture checks and both saved recipe/advancement checks again.

Do not combine `migration.gradle` with the ordinary `gui.gradle` renderer
harness. Neither opt-in harness is included in production JARs.

### Verified local result

On 2026-11-14, the actual 0.2.2 checkout at `921dc7e` seeded and saved the world.
The 0.3.0 development code then completed both `verify` and `verify-resaved`
client runs with normal successful exits. All seven locations retained their
individual FE values (123456–123462), two spent rounds, terrain mode, capacitor
module and custom name. The logged saves changed only the item identity from
`jet_lance` to `jet_cannon`; the old-ID datapack tag, ingredient and result loaded,
and both recipe unlocks plus the historical advancement completion survived.
The pre-upgrade test-world backup was retained. This is a test world, not a
claim that any user's personal world was upgraded.

The separate 62 GameTests, 5,331 rules assertions, 2,106 matrix cases, 108 real
Singulo material references and 23 release-validation tests passed. The dependency
was the released Singulo 0.18.0 1.21.1 NeoForge JAR (SHA256
`1ebf3ffcc50c73a6ac6e4cbcf35e6ed8b1fc98fefcf52c5b6202205bafc7ff7a`).
