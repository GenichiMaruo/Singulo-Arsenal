# Minecraft 26.1.2 development port

Status: headless and vanilla-renderer client validation checkpoint; **shader compatibility remains blocked, not release-ready**. Existing 1.21.1 and 1.20.1 releases are unchanged.

## Target

- Minecraft 26.1.2, NeoForge 26.1.2.126, Java 25.
- Arsenal 0.3.0; matching Singulo >=0.19.0 is required at runtime.
- No stub or fake Singulo JAR is used. The addon deliberately does not compile against Singulo classes; registry references and the existing reflection bridges are resolved at runtime.

## Implemented

- Identifier, item registration, equipment assets, ArmorType/ToolMaterial, entity and tooltip API changes.
- Transactional item/block energy capability and charging, preserving no-extraction rules for gear.
- ValueInput/ValueOutput serialization. The Integrator retains its historical integer energy field and restores all current socket slots regardless of the historical Size field.
- Special item renderers, extracted immutable geometry/render state, SubmitNodeCollector, armor layers, first-person hands, HUD and machine GUI.
- New item definitions and ingredient/resource formats, generated deterministically.
- All 65 existing GameTests retained and migrated to test functions and test-instance data. Test classes/data remain excluded from production JARs.

## Validation completed locally

- Gradle build succeeds with the real Minecraft/NeoForge 26.1.2 dependencies and JDK 25.
- Rules: 5,331 arithmetic checks.
- Resource suite: 47 items, recipes, both translations, deterministic regeneration.
- JUnit RenderSpaceTest: 1 test, no failures.
- Release-tool tests: 26 passed; review-tool tests: 3 passed.

## Required before merge or publication

1. Headless integration is complete against real Singulo SHA-256 `793CA88A01C0EB57F1D1B3671938049452ECCF75CA7BFA55FF526EE78F9DA298`: all 65 distinct Arsenal tests, including six legacy cannon-ID migration tests and SMES charge conservation, passed (66 required including vanilla baseline). Keep this gate in CI.
2. Vanilla client checks are recorded below. Shader-enabled validation remains blocked by the reproduced vanilla-item renderer failure in the dependency-only control environment. Lensing/reflection bridges must remain aligned with the completed Singulo renderer.
3. Update CI Java/toolchain and pin a validated 26.1.2 Singulo commit/artifact; the current integration workflow still targets the established 1.21.1 release. Configure release metadata deliberately only after these gates pass. Publishing remains restricted to the existing release branch.

The build output is a development artifact, not a verified distributable release. No PR, merge, remote push, or publication was performed for this checkpoint.
## First real integration run

The server loaded Singulo and Arsenal and started 66 tests (65 Arsenal plus the vanilla baseline). It failed 47 tests at the shared mock-player setup because MC26 removed `spawnInvulnerableTime`. The fixture now uses the supported `connection.markClientLoaded()` handshake, matching the new damage-immunity condition. A new run is required; this is not a passing integration result. The completion guard now requires all 65 distinct Arsenal test IDs to execute and the global required-test success marker, rather than mistaking the added vanilla test for a count mismatch.

Second real run with the recipe-corrected Singulo artifact executed all 65 Arsenal IDs and failed three assertions (cannon's first shot on a fresh world clock, drill depth, SMES charge conservation). Investigation found and corrected the initial-shot timestamp guard, isolated the long drill ray in its own 32-block structure, and fixed Singulo's ItemCharging writeback to read the committed ItemStacksResourceHandler rather than its constructor's copied source list. These fixes still require the next integration run. Offline rules and deterministic resource checks passed again after the fixture change.

## Final headless rerun (2026-11-15)

`real-singulo-gametest3.log`: all 66 required tests passed, 65 distinct Arsenal execution markers; Gradle BUILD SUCCESSFUL. `real-singulo-build.log`: build, 5,331 rule checks, deterministic resources, and JUnit passed with the same real Singulo dependency. No stub dependency was used. UI was not reopened after the user stopped it; visual/shader validation remains pending.

Development JAR SHA-256: `75FF87102630690215441552CA318791EDB193997E3110FE4D1CED38F5CF4229`. Test classes, test instances, and the isolated drill arena are absent from this JAR.

Final verification against Singulo SHA-256 `0B42F241C71ECF36C1DDEBD5D5303DCF79547522C5C2FC5166015A493A04AD25`: `final-singulo-integration.log` confirms all 65 Arsenal IDs, all 66 required tests, and Gradle `runGameTestServer build` succeeded. Arsenal JAR hash remains unchanged. No UI launched.

## Client review, 2026-11-15

Real vanilla-renderer client review completed 68 deterministic captures (dominant hand, main/offhand cannon, six camera angles per arm, charge/fire/reload, eight wall-frame weapons/tools, 24 cannon frame orientations). Images were inspected; orientation, beam alignment and frame centering are consistent. Offhand cannon intentionally has no custom two-arm pose, so its visible vanilla main hand is not a third hand. Main-hand cannon suppresses the other held item.

Client-only startup exposed invalid namespaced String pipeline/shader IDs in MC26. These are now explicit Identifiers. MC26 also rejects the old empty powered equipment layers; valid transparent layers now preserve the custom suit renderer. The review harness models the new ItemTransform internal half-block translation when auditing FIXED centering.

Shader boundary: official SHA-512-verified Iris 1.11.4/Sodium 0.9.2 and Iris 1.11.3/Sodium 0.9.1 both hit `Missing sampler Sampler1` for vanilla `item_cutout` (item atlas, overlay disabled), with Complementary Reimagined r5.9.3 and r5.8. The decisive control run removed BOTH Singulo and Arsenal: vanilla diamond sword in hand rendered, but dropping it reproduced the same crash (`shader-baseline.log`, mod list contains only Minecraft/NeoForge/Iris/Sodium). Therefore a shader pass is NOT claimed. No generic vanilla sampler injection or shader validation bypass was added. Arsenal glow/XRAY are explicitly assigned through Iris's optional public pipeline API while retaining their original depth and blend settings.

All review worlds are disposable. Music and master volume are zero; weather is clear and `advance_weather=false`. OS sound and ordinary play worlds are unchanged.

## Final visual checkpoint

The full vanilla review completed 85 PNG captures with BUILD SUCCESSFUL (`visual-full-review.log`), in addition to the focused 68-capture hand/frame run. Inspected weapon directions, charge/reload effects, powered armor front/back, bits, Integrator block and GUI, and collapse/meltdown effects have no obvious missing geometry or missing textures. Coverage limitations: the scan fixture found zero ores because sensitivity was insufficient, so XRAY ore markers are not visually verified; the final inventory capture is the creative inventory rather than the equipped-player preview.

After restoring the normal production build configuration, `final-visual-build.log` confirms all 65 distinct Arsenal tests and all 66 required tests passed, 5,331 rules and the normal build passed against real Singulo SHA-256 `521AAA4699FDF7CB7306C5047CA09A2AC9141711884C51C40373987CFD2F73A1`. No stub was used. The resulting development JAR SHA-256 is `D6DAE91927180F7970550CAF1446AE08E44953FCA80EDAF2686D0FD21C73E51E`. Archive inspection confirms no review classes, diagnostic mixin/config, GameTests, test instances, or drill arena are packaged. The shader control failure above remains a release limitation, not a successful shader test. Clients have exited; no publish/push/merge performed.

## Release review follow-up

Independent review found that `CreativeModeInventoryScreen` was not classified as GUI space for held gear and suit rendering. Both renderers now treat it like the survival inventory. Added a `paperdoll-only` review fixture that explicitly opens both survival inventory and the creative INVENTORY tab. Both real screenshots show the equipped player and weapon correctly; `paperdoll-review.log` succeeded with sound disabled and clear weather. The previously unverified inventory-preview limitation is resolved; the ore XRAY fixture limitation remains.

After this fix, `pr-review-build.log` again passes all 65 addon tests / 66 required tests and the normal build. Development JAR SHA-256: `4392337E88A49F621CE6279E1B8B9019E05D6F14A1C8D5A26A0A4620C2E57987`, with the same real Singulo `521AAA...F73A1` dependency. The 26.x ingredient-check parser was also corrected: 116 actual Singulo ingredient references now resolve against the base item definitions, and an empty check is rejected. Release regression tests pass all 26 cases.

## Final published-base pin

The dependency CI source is now the immutable Singulo release commit `10f77a56facf7ca9b4e3418116aac03b52810af0` (tag `v0.19.0-26.1.2N`). Local integration also ran against the actual official CI artifact `43703005408`, SHA-256 `891f6d67453bc7bbb55db6003464e0fbbc629f2b6a944192e4657cf95df51f1b`, not a rebuilt substitute. `published-base-integration.log`: all 66 required tests and build passed. Arsenal product bytes are unchanged. CurseForge base file 9627445 was accepted, but addon publication still requires its public availability; no pending-file guard is bypassed.
