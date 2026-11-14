# 0.3.0 rendering changes (development only)

## Scope

The development branch targets Minecraft 1.21.1 / NeoForge 21.1.256 / Java 21
with the released Singulo 0.18.0. It is not a merged or published release.

- The display name remains **Accretion Jet Cannon** / **降着ジェット砲**.
  The internal item ID is now `singulo_arsenal:jet_cannon`, implemented by
  `JetCannonItem` / `ArsenalItems.JET_CANNON`. The old `jet_lance` ID resolves
  to the same item through NeoForge's registry alias. No balance or component
  schema was changed. See [migration details](cannon-migration-0.3.0.md).
- Only a main-hand cannon gets the two-handed first-person pose. A main-hand
  cannon suppresses the OFF_HAND render pass, for both dominant-arm settings.
  This does not remove the item, change its functionality, or hide third-person
  equipment. An offhand cannon is drawn as an ordinary held item without the
  artificial two-arm pose; firing remains main-hand-only.
- The actual ModelView matrix determines the vertex-space conversion:
  `inverse(ModelView) * inverse(cameraRotation)`. Aim, muzzle, arms and beam
  no longer assume vanilla's camera-relative world-space hand pose.
- Iris shader packs use the existing vanilla-lightning beam fallback because
  their pipeline suppresses unknown custom core shaders. The optional public
  Iris API is queried without a production dependency. Its shadow pass cannot
  overwrite the main-view muzzle cache or apply main-camera aim transforms.
- All eight weapons/powered tools have separate FIXED translations measured
  from emitted geometry, not copied from their inventory GUI fitting. GUI,
  ground and hand transforms are unchanged.

## Evidence and repeatable checks

- JUnit matrix tests cover 2,106 basis/roundtrip checks over yaw, pitch and
  three ModelView arrangements, including non-identity bob transforms.
- `tools/check.py` protects the English name and deterministic generated
  resources in addition to the existing 5,331 rules checks.
- `tools/review/RENDER_REVIEW.md` describes the synchronized 68-image hand,
  aim, firing/reload and item-frame fixtures. The eight FIXED geometric
  centers are asserted within 0.001 item units, independently of screenshots.
  The two added offhand-cannon/main-empty fixtures explicitly cover vanilla's
  bare hand for both dominant arms. Earlier 66-image runs do not validate these
  two new fixtures; use the 68-image run for this regression.
- Shader validation uses official Iris 1.8.12, Sodium 0.6.13 and Complementary
  Reimagined r5.9.3, with shader-enabled state checked in the client log.
  This is a tested combination, not a guarantee for every shader pack.
- The ordinary 85-image harness retains third-person, GUI, armor/tool and
  actual trigger-path coverage. A generated image is evidence to inspect,
  not by itself a visual pass.

## 1.20.1 port notes

The current 1.20.1 branch has the same FirstPersonHands implementation and
similar GearRenderer/FxClient paths, so the underlying hand separation and
FIXED fitting can be ported. It uses Forge event namespaces and a different
client-extension registration path. Partial ticks and the 1.20.1 model-view
contract must be adapted and tested on Forge and NeoForge separately. This
branch deliberately does not claim a compiled or client-tested 1.20.1 port.

## Primary rendering contracts

- Minecraft/NeoForge 1.21.1 GameRenderer and RenderHandEvent sources in the
  resolved development dependency.
- [Iris hand pipeline](https://github.com/IrisShaders/Iris/blob/1.21.1/common/src/main/java/net/irisshaders/iris/pathways/HandRenderer.java)
- [Iris public API](https://github.com/IrisShaders/Iris/blob/1.21.1/common/src/api/java/net/irisshaders/iris/api/v0/IrisApi.java)

## Quiet review clients

`prepare_gui.py` sets only the disposable review game's music volume to zero.
The client harness asserts this setting at startup. Other sound categories and
the user's ordinary game/OS settings are not changed.
