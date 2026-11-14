# 0.3 rendering-focused client review

This is an opt-in alternative to the existing 85-capture script. It does not add
captures or runtime to that default script. It belongs only in a disposable
review world. Run results belong in the versioned review report; generating screenshots alone
is not a visual pass.

## Run

Use the existing `tools/review/prepare_gui.py` and `-I tools/review/gui.gradle`
client-review workflow. After preparation, create an empty marker file
`build/gui-review/render-only` before starting the client. This marker takes
precedence over `icons-only`. Remove it to return to the ordinary review.
Alternatively, pass `-Darsenal.review.render=true` **to the client JVM**;
a Gradle JVM system property alone is not automatically forwarded.

Run once with vanilla rendering and separately with the intended shader-mod
and shader-pack combination. Archive `screenshots` between runs, since these
runs deliberately use the same screenshot names. Record the mod/pack version,
shader enabled/disabled state, and FOV alongside the archived captures.

## Coverage (66 images)

- For each dominant arm: main-hand cannon with empty/torch/shield/real filled-map
  offhand; offhand cannon with main-hand torch; cannon in both hands.
- For each arm: yaw 0, 90, 180, -90 at pitch 0, plus yaw/pitch 45/-60 and -45/60.
- For each arm: real `tryFire` charge, jet, and early/middle/late reload. The last
  magazine round is seeded to avoid three redundant shots, but animation times
  are not faked. This is not an input-packet test; the existing full script
  retains its actual left-click test.
- All four weapons and four tools in a wall frame, rotation zero.
- Cannon in wall/floor/ceiling frames, each at all eight frame rotations.

Server setup is acknowledged and the expected client equipment/components,
handedness/view angles, or tracked frame entity/item/direction/rotation must
arrive before the timeline proceeds. A 200-client-tick readiness timeout fails
instead of silently capturing the preceding fixture. Normal short settling
intervals remain for equip animations and camera interpolation.

`ARSENAL_RENDER_READY` logs identify synchronized fixtures. The final
`ARSENAL_RENDER_FIXTURES_COMPLETE` only means the capture script ran; inspect
images for correct hand count, offhand suppression, muzzle/beam alignment,
view-dependent orientation, and item-frame centering. It is not a visual pass.

## Geometry audit

The render mode writes `screenshots/fixed-bounds.txt` before capturing. It uses
`Canvas.drawFine` idle vertices for all eight weapons/tools from all passes and the actually loaded
corresponding baked model's FIXED transform, including generated translation. It reports raw
model-pixel bounds, FIXED item-unit bounds, XY midpoint, and whether both axes
are within 0.001 item units of zero. The item renderer's -0.5 and custom renderer's
+0.5 offsets cancel; the audit includes the custom renderer's 1/16 art scale.

For a baseline, the report does not fail on an off-center model. To enforce the
new centering requirement create `build/gui-review/require-fixed-center`, or
pass `-Darsenal.review.requireFixedCenter=true` to the client JVM. A failed
assertion still leaves the diagnostic file. This checks emitted geometric
bounds at idle time zero, not the opaque pixel silhouette or dynamic glow at
every animation phase; frame screenshots remain required.

## Optional shader dependencies

Place official, checksum-verified Iris and required Sodium JARs under
`build/review-deps`, and pass `-PreviewShaders=true` alongside the opt-in init
script. This directory is ignored by Git and these dependencies are not part
of normal builds. Install the selected pack under the disposable game
directory's `shaderpacks` and configure its `config/iris.properties`. Record
exact versions/checksums and inspect the log to confirm the pack is enabled.
