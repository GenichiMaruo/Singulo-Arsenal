# Singulo 0.18.0 compatibility

Arsenal 0.2.1 declares `[0.16.0,0.18.0)`, so the loader rejects Singulo 0.18.0 before gameplay initialization. Version 0.2.2 expands this to `[0.16.0,0.19.0)`. Previous 0.16.x / 0.17.x support is retained.

No production gameplay, recipes or saved-data format changes are intended. Material IDs, machine recipes, mass-pellet data and the public `GravitationalLensing.add/addDistortion(Vec3,float,float,float)` entry points are checked against Singulo 0.18.0.

Final source pins, release-binary hashes and validation results are pending the official Singulo 0.18.0 release. This branch must not be published before those checks complete.

Java sources now use LF checkouts on all platforms. The previous Windows/Linux release comparison found only a compiler-generated local-variable debug name difference caused by CRLF offsets in `FxClient.java`; LF avoids that source of artifact drift without changing the executable code.

A new required GameTest exercises the actual Singulo SMES charging block with capacitor-equipped Arsenal armor: partial charging, exact supply debit, capacity limiting, blocked output and full-charge output without losing the module. It uses test-only reflection and does not add a compile-time or production dependency on the new 0.18 API.

The legacy meltdown fixture now waits until its spawned target is visible to the real spatial query before firing once, matching the existing horizon fixture. Initial Forge validation exposed a transient missing-hit failure; diagnostic reruns passed. The immediate-hit, terrain-preservation and owner-protection assertions remain unchanged.

Candidate Linux NeoForge CI also exposed a zero-hit failure in the pre-existing cannon amplifier fixture. It now waits for the actual beam query to see its target, asserts the target is unharmed, fires exactly once and retains the exact amplified-damage assertion. No production combat code was changed.
