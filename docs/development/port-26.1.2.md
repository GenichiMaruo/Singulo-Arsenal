# Minecraft 26.1.2 development port

Status: compilation checkpoint, **not release-ready**. Existing 1.21.1 and 1.20.1 releases are unchanged.

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
2. Run a client and visually check normal/shader rendering, cannon aim and hands, offhand suppression, beam origin, armor, item-frame centering, and Integrator GUI. Lensing/reflection bridges must agree with the completed Singulo renderer.
3. Update CI Java/toolchain and pin a validated 26.1.2 Singulo commit/artifact; the current integration workflow still targets the established 1.21.1 release. Configure release metadata deliberately only after these gates pass. Publishing remains restricted to the existing release branch.

The build output is a development artifact, not a verified distributable release. No PR, merge, remote push, or publication was performed for this checkpoint.
## First real integration run

The server loaded Singulo and Arsenal and started 66 tests (65 Arsenal plus the vanilla baseline). It failed 47 tests at the shared mock-player setup because MC26 removed `spawnInvulnerableTime`. The fixture now uses the supported `connection.markClientLoaded()` handshake, matching the new damage-immunity condition. A new run is required; this is not a passing integration result. The completion guard now requires all 65 distinct Arsenal test IDs to execute and the global required-test success marker, rather than mistaking the added vanilla test for a count mismatch.

Second real run with the recipe-corrected Singulo artifact executed all 65 Arsenal IDs and failed three assertions (cannon's first shot on a fresh world clock, drill depth, SMES charge conservation). Investigation found and corrected the initial-shot timestamp guard, isolated the long drill ray in its own 32-block structure, and fixed Singulo's ItemCharging writeback to read the committed ItemStacksResourceHandler rather than its constructor's copied source list. These fixes still require the next integration run. Offline rules and deterministic resource checks passed again after the fixture change.

## Final headless rerun (2026-11-15)

`real-singulo-gametest3.log`: all 66 required tests passed, 65 distinct Arsenal execution markers; Gradle BUILD SUCCESSFUL. `real-singulo-build.log`: build, 5,331 rule checks, deterministic resources, and JUnit passed with the same real Singulo dependency. No stub dependency was used. UI was not reopened after the user stopped it; visual/shader validation remains pending.

Development JAR SHA-256: `75FF87102630690215441552CA318791EDB193997E3110FE4D1CED38F5CF4229`. Test classes, test instances, and the isolated drill arena are absent from this JAR.
