# Singulo compatibility for Arsenal 0.3.0

## Dependency policy

Both sides require Singulo `[0.19.0,)`: **0.19.0 or newer, without an upper
bound**, with matching Minecraft and loader artifacts. This follows the user's
minimum-version requirement. The exact tested base is 0.19.0. Later versions,
including 0.20 and newer, are admitted by metadata but their compatibility has
not been verified. Admission is not a promise about future API compatibility.
CurseForge declares the Singulo project required; the JAR enforces the minimum.
Singulo classes are never bundled in the add-on.

## Verified 1.21.1 base

- Tagged source: `v0.19.0`, `0f6fcff7645a2a5066a072acce55d4a03884080e`.
- Successful base CI: `43628318212`, synthetic commit
  `245ae8e17a03b9d820715e1c86fb3ca5a5401f44`. CI and tag trees both equal
  `9eb8f4a7ebf43ee9f7ae7a41a7721d4b342c2fe4`.
- CI JAR SHA256:
  `ec45f7d6a6a9dbac654c7a2048df085ca709f055868def39aeee4132d44ed942`.
- [CurseForge file 9617080](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9617080)
  was accepted by workflow `43632052851`. Its actual CDN download independently
  matched that SHA256 at 2026-11-14 19:32 UTC. At the 19:34 author check it was
  still Processing, so general-public approval remains a release gate.

## Minimum-only metadata checks

An earlier checkpoint retained a next-minor upper bound. It was removed before
release to honor the minimum-only requirement. No item/rendering implementation
changed with this metadata correction.

With `[0.19.0,)`, the actual CDN 0.19.0 JAR passed a Java 21 production build,
all 65 GameTests, 5,331 rules assertions, 116 real material references, resource
regeneration checks and 2,106 matrix cases. Release JAR validation passed.
Python validation passed 29 tests (26 release, 3 review preparation), including
a repository-policy assertion and rejection of accidental lower/upper bounds.

A separate startup with published 0.18.0 was refused before a world was created;
the loader reported expected `[0.19.0,)`, actual `0.18.0`, and a fatal mod-loading
error. The development wrapper returned zero despite refusal, so the fatal log
and absent `world/level.dat` were checked instead of trusting only the exit code.
These checks do not simulate or claim support for unreleased future APIs.

## Actual-client rendering and migration

The unchanged implementation was checked with the exact 0.19.0 bytes above.
Separate fresh worlds completed vanilla 68 and shader 68 synchronized hand,
aim, charge/fire/reload and frame captures, then exited normally. All eight
FIXED centers passed (maximum XY midpoint error `5.96e-8` item units).
Representative captures received independent visual review; image generation
alone is not treated as visual approval. Main-hand cannon offhand suppression
is first-person only. Third-person equipment and actual inventories are unchanged.

Shader setup: official Iris 1.8.12 JAR (runtime identifies itself as
`1.8.12-snapshot+mc1.21.1-local`), Sodium 0.6.13 and Complementary Reimagined
r5.9.3, confirmed enabled. Pack uniform/block-property warnings remain. This
is not a warning-free claim or a guarantee for every shader/camera modification.
Music alone was muted in dedicated review directories; effects were retained.

Six final icon captures verified flat equipment sprites, the diagonal cannon,
English brief/detail tooltips, plain nihonium armor preview and held sword.
The review-only InventoryScreen wrapper avoids unrelated hover tooltips while
unfocused. An initial obscured run was retained separately before a clean rerun.
The preparation script also refuses to overwrite review worlds: an earlier
reused-world frame drop invalidated the empty-hand fixture, and a fresh run passed.

An old 0.2.2 + published 0.18.0 world was seeded and backed up, opened with the
verified 0.19.0 base + 0.3.0, saved, and opened again. Both verify phases retained
seven locations (main/off hand, inventory, chest, frame, dropped item, armor
stand), canonical `jet_cannon`, FE 123456-123462, magazine 2, terrain mode 1,
capacitor module and names. Historical recipe/advancement unlocks and old-ID
datapack ingredient/result/tag references were preserved on 1.21.1.

## Upgrade limits and release gate

Back up worlds and datapacks. Downgrading canonical `jet_cannon` saves to 0.2.2
is unsupported; restore the backup instead. Raw-ID scripts, caches and resource
pack overrides are not arbitrary-string-rewritten. The historical cannon recipe
and advancement IDs deliberately remain unchanged.

On **1.20.1 only**, Forge/NeoForge 47 item-tag holder lookup does not follow item
aliases. External datapack item-tag elements using `singulo_arsenal:jet_lance`
must be changed to `singulo_arsenal:jet_cannon`; keep tag names and recipe IDs.
Saved stacks use the official alias/MissingMappings remap, and direct old-ID
recipe ingredients/results remain supported. The separate legacy migration
report and release notes describe the tested manual tag update, not an automatic
tag conversion. No broad registry Mixin or user-pack rewrite is installed.

Before merge/publication, confirm general-public approval of the matched base
file and successful checks for the final PR head. This is a pre-publication
verification report, not an Arsenal upload receipt.
