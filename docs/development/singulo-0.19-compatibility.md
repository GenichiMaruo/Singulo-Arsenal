# Singulo 0.19 compatibility for Arsenal 0.3.0

## Required dependency

Both sides require Singulo `[0.19.0,0.20.0)`, with the matching Minecraft and
loader artifact. This raises the minimum to 0.19.0 and retains the established
next-minor upper bound: future untested API changes are not silently accepted.
CurseForge also declares Singulo as a required project dependency. The JAR's
loader metadata enforces the version interval; a CurseForge project relation
alone cannot express it.

The add-on does not bundle Singulo classes and accesses its ingredients, tags,
FE capabilities and optional rendering interface by their existing contracts.
The plain nihonium set consumes `singulo:nihonium_ingot`, uses
`c:ingots/nihonium` for repair, and has no FE capability or module slots.

## Release gate

CI now pins the tagged Singulo 0.19.0 source
`0f6fcff7645a2a5066a072acce55d4a03884080e` (`v0.19.0`). Base PR #74 was
validated by CI `43628318212` before merging. This source pin alone is not the
final distribution gate: before Arsenal merge/publication, record the base
CurseForge ID and compare its downloaded JAR to the CI artifact, then rerun
integration and client verification against that exact dependency.
The migration and rendering checks from earlier 0.3.0 work remain relevant but
do not replace validation against the final 0.19 binaries. Preserve the old
cannon recipe/advancement IDs while remapping its item ID. Backups are required
before upgrades; new canonical IDs are not downgrade-compatible with 0.2.2.

## Preliminary checkpoint (not the final release gate)

- Source `7708a6d`; draft PR #11, CI runs `43626822561` and `43626863818`:
  build and integration successful against candidate `d12d72c0`.
- Candidate dependency SHA256:
  `9152f87437b98e3c5a7d1c244f9a027cf9e9732d19cbe83574b24f9359fd8ae5`.
- Java 21 build, 5,331 rules assertions, 2,106 matrix cases, 116 real Singulo
  ingredient references, 23 release-validation tests and 65 GameTests passed.
- The vanilla actual client completed all 68 synchronized hand/aim/fire/reload/
  frame captures and exited normally. Music alone was muted. The eight FIXED
  centers passed, with maximum midpoint error `5.96e-8` item units. Representative
  left/right hand, bare main hand with offhand cannon, firing and floor-frame
  captures were visually inspected; image generation alone is not a pass.
- A separate negative startup with the published Singulo 0.18.0 JAR was rejected
  before any world started. The loader reported the expected `[0.19.0,0.20.0)`
  interval and a fatal mod-loading error. The development Gradle server wrapper
  returned zero despite that refusal, so the fatal log and absent world were
  explicitly checked instead of relying on the wrapper exit code.
- Independent read-only review found no P1/P2 modern implementation issue.
  It requested an explicit 0.18 seed / 0.19 verify dependency switch in the
  old-world migration instructions; that clarification is included.

- The candidate Iris 1.8.12 + Sodium 0.6.13 client, with Complementary Reimagined
  r5.9.3 actually enabled, also completed all 68 captures and exited normally.
  English HUD text, firing/beam alignment, offhand suppression and ceiling/wall
  frames were visually inspected. All eight FIXED-center assertions passed.
  An initial reused-world attempt was rejected by the empty-hand fixture after
  picking up dropped frame items. Its evidence was retained; a fresh-world
  rerun passed. The preparation script now refuses to overwrite an existing
  review world, with three regression tests, and the instructions require a
  fresh world for every rendering run. Migration resave tests intentionally
  retain their own separate world.

This checkpoint does **not** claim final-dependency or current old-world resave
verification. Those remain mandatory before merge/publication.

## Tagged-source verification (2026-11-15 JST; distribution gate pending)

- Base `v0.19.0` points to `0f6fcff7645a2a5066a072acce55d4a03884080e`.
  Successful base CI `43628318212` used synthetic commit `245ae8e17a03b9d820715e1c86fb3ca5a5401f44`;
  both trees are `9eb8f4a7ebf43ee9f7ae7a41a7721d4b342c2fe4`.
- The actual base CI JAR SHA256 is
  `ec45f7d6a6a9dbac654c7a2048df085ca709f055868def39aeee4132d44ed942`.
  CurseForge file [9617080](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9617080)
  was accepted by workflow `43632052851`; its CDN
  download matched this hash, including an independent download at 19:32 UTC.
  It was still Processing at the 19:34 UTC author check,
  so general-public approval remains a release gate.
- Arsenal `5ad54849` passed CI `43630011917` and `43630007872` with the final
  source pin. The actual CI base JAR also passed a local production build and
  all 65 GameTests. Python release/review validation passed all 26 tests.
- Separate fresh-world actual clients completed vanilla 68 and shader 68
  rendering fixtures, exited normally, and passed all eight FIXED-center
  assertions. Representative hand/aim/fire/frame images received independent
  visual review with no new issue. Music alone was muted.
- Shader setup: official Iris 1.8.12 JAR (runtime reports
  `1.8.12-snapshot+mc1.21.1-local`), Sodium 0.6.13 and Complementary Reimagined
  r5.9.3, with the pack confirmed enabled. Pack uniform/block-property warnings
  remain; this is not a warning-free claim or a promise for every shader pack.
- The old 0.2.2 + published 0.18.0 world was seeded, backed up, opened with the
  final CI base JAR + 0.3.0, saved, and opened again. Both verify phases passed
  all seven persisted locations: main/off hand, inventory, chest, frame, dropped
  item and armor stand. Canonical `jet_cannon`, FE 123456-123462, magazine 2,
  terrain mode 1, capacitor module and custom names were retained. Historical
  recipe/advancement unlocks and old-ID datapack ingredient/result/tag resolved.
- The six icon captures showed the plain nihonium armor preview, four tool
  icons and held sword correctly. An unrelated native hover tooltip obscured
  part of the inventory captures while unfocused. A QA-only InventoryScreen
  wrapper now supplies offscreen mouse coordinates. Its fresh six-image rerun
  exited normally and showed unobscured icons and English tooltips, plus the
  nihonium worn preview and held sword. Production rendering is unchanged.
  QA-only follow-up `39b1bb11` also passed CI `43631710539` and `43631706813`.

Before merge/publication, confirm general-public approval of the matched base
CurseForge file and successful checks for the final PR head.
No Arsenal 0.3.0 upload is claimed by this report.
