# Singulo 0.18.0 compatibility

Arsenal 0.2.1 declares `[0.16.0,0.18.0)`, so the loader rejects Singulo 0.18.0 before gameplay initialization. Version 0.2.2 expands this to `[0.16.0,0.19.0)`. Previous 0.16.x / 0.17.x support is retained.

No production gameplay, recipes or saved-data format changes are intended. Material IDs, machine recipes, mass-pellet data and the public `GravitationalLensing.add/addDistortion(Vec3,float,float,float)` entry points are checked against Singulo 0.18.0.

Integration CI is pinned to Singulo `v0.18.0`, commit `1a94e63423183c4fe2174b86831fc86843229d5e`.

The actual CurseForge dependency is [file 9596414](https://www.curseforge.com/minecraft/mc-mods/singulo/files/9596414), `Singulo-0.18.0-1.21.1N.jar`, SHA-256 `1ebf3ffcc50c73a6ac6e4cbcf35e6ed8b1fc98fefcf52c5b6202205bafc7ff7a`. The CDN download matches the base release's CI artifact. Local integration against that exact binary passes all 56 required GameTests, 5,331 offline rule assertions, 108 Singulo material references, resource regeneration and both locales. Release validation passes 23 unit tests.

The real-client review against the same published dependency completed all 85 captures, including the final inventory capture, and both actual lens method handles resolved and were invoked successfully. The release still waits for the dependency file to become generally available.

Java sources now use LF checkouts on all platforms. The previous Windows/Linux release comparison found only a compiler-generated local-variable debug name difference caused by CRLF offsets in `FxClient.java`; LF avoids that source of artifact drift without changing the executable code.

A new required GameTest exercises the actual Singulo SMES charging block with capacitor-equipped Arsenal armor: partial charging, exact supply debit, capacity limiting, blocked output and full-charge output without losing the module. It uses test-only reflection and does not add a compile-time or production dependency on the new 0.18 API.

Java compilation also includes `.gitattributes` in its cache key. Gradle can otherwise reuse pre-normalization class files because it normalizes source line endings for input fingerprints, while javac synthetic local-variable debug names can retain source character offsets.

The Windows production JAR is byte-for-byte identical to the independently built Linux CI artifact (SHA-256 `982fbc6f5e0c897c9e029bc2e9609a6878f5052c323ce2f7191149c32153fb26`). No production gameplay class was modified.
