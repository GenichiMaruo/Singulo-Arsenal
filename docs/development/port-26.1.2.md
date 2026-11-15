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

1. Obtain the completed matching Singulo 26.1.2 build. Its port currently does not compile; no runtime or GameTest success is claimed here.
2. Align the SMES integration test with Singulo's final transactional energy and charge-slot APIs. The test currently expects EnergyHandler from energy() and the legacy IItemHandlerModifiable from chargeSlots(). Do not bypass this test.
3. Run all 65 required GameTests with `-PsinguloJar=/path/to/the/real/26.1.2/Singulo.jar` and verify their actual count and assertions.
4. Confirm test-function registration and the new test-instance data are loaded, including all six legacy cannon ID migration tests.
5. Run a client and visually check normal/shader rendering, cannon aim and hands, offhand suppression, beam origin, armor, item-frame centering, and Integrator GUI. Lensing/reflection bridges must agree with the completed Singulo renderer.
6. Update CI Java/toolchain and pin a validated 26.1.2 Singulo commit/artifact; the current integration workflow still targets the established 1.21.1 release. Configure release metadata deliberately only after these gates pass. Publishing remains restricted to the existing release branch.

The build output is a development artifact, not a verified distributable release. No PR, merge, remote push, or publication was performed for this checkpoint.