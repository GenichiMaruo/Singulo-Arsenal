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
