# Singulo 0.18.0 compatibility

Arsenal 0.2.1 declares `[0.16.0,0.18.0)`, so the loader rejects Singulo 0.18.0 before gameplay initialization. Version 0.2.2 expands this to `[0.16.0,0.19.0)`. Previous 0.16.x / 0.17.x support is retained.

No production gameplay, recipes or saved-data format changes are intended. Material IDs, machine recipes, mass-pellet data and the public `GravitationalLensing.add/addDistortion(Vec3,float,float,float)` entry points are checked against Singulo 0.18.0.

Integration CI is pinned to the final Singulo 0.18.0 release commit `49325262cc19c235fccdab9a3f233ceccc52a236`, shared by tags `v0.18.0-1.20.1F` and `v0.18.0-1.20.1N`.

Both actual CurseForge dependency files are approved and generally available. Their CDN downloads match the base release's CI artifacts:

- Forge: [file 9596768](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9596768), `Singulo-0.18.0-1.20.1F.jar`, SHA-256 `616183ffe3a68dd8c4d29b6853289c08930cb30f999fc9ec2c7eeb6198e35d8f`.
- NeoForge: [file 9596800](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9596800), `Singulo-0.18.0-1.20.1N.jar`, SHA-256 `46eaeae60747d30d3a30e6b7c7c6219ee44c3280ebaf4da7df255655eb8704a8`.

Local integration against each exact released binary passes all 57 required GameTests, 5,331 offline rule assertions, 108 Singulo material references, resource regeneration and both locales. Release validation passes 25 unit tests. Both real-client runs completed all 85 captures, including the final inventory capture, and both actual lens method handles resolved and were invoked successfully. Representative beam and armor captures were visually inspected.

The independently built Linux CI run [43484638080](https://github.com/GenichiMaruo/Singulo-Arsenal/actions/runs/43484638080) passed both loader jobs. Local Windows production artifacts match that CI byte-for-byte:

- Forge SHA-256: `0217a5259a5dfcc7c42959d3e3953984122b123edb076a766f93e5b8ee9348b7`.
- NeoForge SHA-256: `dc59f4a73bf78b9582dc37640095534e6093423497b1c09572c559c628666507`.

Java sources now use LF checkouts on all platforms. The previous Windows/Linux release comparison found only a compiler-generated local-variable debug name difference caused by CRLF offsets in `FxClient.java`; LF avoids that source of artifact drift without changing the executable code.

A new required GameTest exercises the actual Singulo SMES charging block with capacitor-equipped Arsenal armor: partial charging, exact supply debit, capacity limiting, blocked output and full-charge output without losing the module. It uses test-only reflection and does not add a compile-time or production dependency on the new 0.18 API.

The legacy meltdown fixture now waits until its spawned target is visible to the real spatial query before firing once, matching the existing horizon fixture. Initial Forge validation exposed a transient missing-hit failure; diagnostic reruns passed. The immediate-hit, terrain-preservation and owner-protection assertions remain unchanged.

Candidate Linux NeoForge CI also exposed a zero-hit failure in the pre-existing cannon amplifier fixture. It now waits for the actual beam query to see its target, asserts the target is unharmed, fires exactly once and retains the exact amplified-damage assertion. No production combat code was changed.

The same readiness gate was applied to the remaining legacy combat fixtures that use spatial queries. It waits for fixture visibility only; original attack counts and exact damage/FE/terrain/owner assertions are retained. A pre-attack health check prevents unrelated damage from creating a false pass. Drone gear is equipped only after readiness, avoiding automatic attacks during the wait.

The LF checkout rule is included in the Java compile cache key to prevent reuse of pre-normalization classes. After recompilation, the Windows Forge production JAR matches the independently built Linux CI artifact byte-for-byte (SHA-256 `0217a5259a5dfcc7c42959d3e3953984122b123edb076a766f93e5b8ee9348b7`).
