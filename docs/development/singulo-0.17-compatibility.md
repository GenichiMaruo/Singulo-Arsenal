# Singulo 0.17.0 compatibility

Arsenal 0.2.0 declares `[0.16.0,0.17.0)`, so the loader rejects Singulo 0.17.0 before gameplay initialization. Version 0.2.1 expands this to `[0.16.0,0.18.0)` and pins integration CI to `1b6a4a4b5363013c465d6e12f057e92f1a5f1377` for Minecraft 1.21.1.

The addon has no compile-time Singulo dependency. Material IDs, machine recipes, mass-pellet data and the public `GravitationalLensing.add/addDistortion(Vec3,float,float,float)` client entry points are checked against the released source and runtime. No production gameplay or saved-data format change is intended.

Local validation uses the actual, SHA-256-verified CurseForge 0.17.0 JAR. 1.21.1 NeoForge: 55/55 required GameTests passed. Offline checks passed all 5,331 rule assertions, 108 Singulo material references, resource regeneration and both locales. Release-validator unit tests passed 23 cases. The opt-in client review now asserts both actual lens method handles resolve and can be invoked; its completion log and captures are kept with the validation evidence. Final CI and client outcomes are recorded in the PR. No tag, GitHub Release, CurseForge upload or Modrinth upload is part of this task.

Windows checkout also now keeps generated `.mcmeta` files in LF, matching generator output. This fixes a false regeneration mismatch on CRLF-default systems without changing asset contents.
