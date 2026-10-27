# AutoMap plan — phase by phase, build by build

Slipstream build of Vela Maps, package `app.automap`, for AutoSpace.
One build at a time, in order. Each build needs CI green, a verified APK,
screenshot diffs, and a drive-test checklist before the next build starts.
No patch touches routing, traffic, places, search, or voice publishing.

## Phase 0 — Slipstream rig, Build 0

Pin one upstream weekly stable. Patch-stack repository, build script,
signing, release naming, AutoSpace catalog change. Build 0 ships with an
empty patch stack, renamed to AutoMap, behaving like upstream. Baseline:
snapshot timing, day/night screenshots on dense-city, highway, and
interchange routes, backup/restore test.

## Phase 1 — Palettes and controls, Builds 1–3

- Build 1: day palette, sampled only from flat areas of the Google Maps
  reference screenshots, applied in the light theme and base style.
- Build 2: night palette, sampled separately (cyan night route vs deep-blue
  day route, AMOLED variant kept separate).
- Build 3: Automatic / Day / Night plus 2D / 3D controls, mirroring the
  reference settings panel; final adaptive-icon layers from `docs/icon.png`,
  with a small-size legibility check. Default for Automatic is a gate
  decision, not assumed here.

## Phase 2 — Quick wins, Builds 4–6

- Build 4: realistic vehicle puck instead of the blue arrow, phone and car.
- Build 5: car nav declutter, ported from the phone's rules.
- Build 6: bigger traffic lights and stop signs — larger glyphs, lower zoom
  threshold, stronger outlines, from existing road-features data.

## Phase 3 — 3D buildings, then fade, Builds 7–8

- Build 7: 3D buildings in the car, verified and tuned to the new palette,
  height grow-in zoom 17 to 19 matched to the phone, dense-downtown
  performance cap.
- Build 8: fade-away buildings on turn approach, eased, with a toggle.
  Per-building occlusion is deferred, not in this build.

## Phase 4 — Smart auto-zoom, Builds 9–10

- Build 9: context scoring (maneuver type, multi-lane splits/forks,
  curvature, lane count where known, distance, speed) extending existing
  speed zoom and turn framing.
- Build 10: pitch and easing for the snapshotter; user pan/pinch/overview
  are hard overrides.

## Phase 5 — Painted roads, Builds 11–14

- Build 11: PaintedRoads car port — inject source and layers into the car
  style JSON before the snapshotter loads; the developer dial becomes a
  real setting.
- Build 12: bake expansion from California to one pilot region (region is
  a gate decision), with bake/download size measured.
- Build 13: staged regional rollout with coverage reports; where lane or
  crossing tags are missing, no markings are invented.
- Build 14: lane emphasis at splits and exits — needed lanes stand out,
  others muted.

## Phase 6 — Overpasses, exits, arrival, Builds 15–16

- Build 15: overpass/exit emphasis in 2D only — bridge casing, deck shadow,
  chosen ramp in route colour, larger exit shields and junction labels.
- Build 16: destination arrival — entrance highlight, side of street,
  nearby car parks as nearby places (not a parking recommendation), arrival
  framing.

## Phase 7 — Terrain and sun, as toggles, Builds 17–18

- Build 17: terrain relief in the car, off by default — hillshade, not true
  3D terrain.
- Build 18: sun-angle building shading tied to the time-of-day palette,
  with a toggle; night mode never washed out by a day light value.

## Phase 8 — Hardening and release, Build 19

Full regression (dense city, highway, interchange, rural, day, night, 2D,
3D, offline, backup/restore, AutoSpace turn publishing) against the Build 0
baseline. Patch stack and build script published here. Plain-language
release notes. Sync runbook.

## Ongoing — upstream sync builds

After every upstream weekly stable, rebase the patch stack, run the
regression matrix, and ship only if it passes, with no feature changes.
A conflicting patch is fixed in the open, not silently dropped.

## Out of scope

Trees in any form, true 3D bridges, bridge transparency, full weather
simulation, AI landmark reconstruction, conversational guidance, satellite
hybrid in the car for version one, "more detailed maps overall" as a
standalone item, and arrows in the icon.
