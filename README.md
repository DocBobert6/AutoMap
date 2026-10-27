# AutoMap

**Status: work in progress. No release yet, no APK published from this repo yet.**

AutoMap is a slipstream build of [Vela Maps](https://github.com/PimpinPumpkin/Vela)
for use with the AutoSpace app: an immersive navigation map with new day and
night palettes, 3D buildings, painted highway lanes and road markings, and
smart auto-zoom.

## What AutoMap will be

- Package: `app.automap` (internal code namespace stays `app.vela`, so the
  patch stack rebases cleanly onto upstream Vela).
- Display name: AutoMap. Every user-visible mention of Vela becomes AutoMap;
  upstream Vela is credited here and in the app's About screen.
- Licence: GPLv3, same as upstream Vela. See `LICENSE`.
- Build method: **slipstream, not a hard fork.** A pinned upstream Vela
  release is taken, a patch stack is applied in a fixed order, the result is
  built and verified, and shipped on its own channel. Every upstream weekly
  stable is rebased the same way. A hard fork is only a fallback if upstream
  is archived, drops car support, changes licence, or the patch stack stops
  rebasing cleanly.
- AutoSpace companion change: AutoSpace adds `app.automap` to its
  navigation-app catalog and prefers it over `app.vela`. AutoMap can coexist
  with upstream Vela on the same phone; lists and routes move by Vela's file
  backup and restore.

## Planned scope

See `PLAN.md` for the full phase-by-phase, build-by-build plan (Builds 0-19).

In scope: 3D buildings, fade-away buildings, bigger traffic lights and stop
signs, highway lanes, crosswalks and detailed road markings, overpass and
exit emphasis (2D only), smart/dynamic auto-zoom, time-of-day with new day
and night palettes sampled from Google Maps reference screenshots plus
Automatic / Day / Night and 2D / 3D controls, sun-angle building shading, a
realistic vehicle icon, nav declutter in the car, terrain relief in the car
as a toggle off by default, and a destination arrival view (entrance, side
of street, nearby parking).

Out of scope, by decision: trees in any form (including tree icons), true
3D bridges and bridge transparency, full weather simulation, AI landmark
reconstruction, conversational guidance, satellite hybrid in the car for
version one, and "more detailed maps overall" as a standalone item. The icon
also has no arrows.

## Icon

![AutoMap icon](docs/icon.png)

The icon is a crop of a real map image: a highway interchange, ramps, grass,
buildings, and a pond, zoomed in and shaped as a UI app icon. No arrows, no
trees, no text.

## What is not in this repo yet

The full source tree is being built and verified locally first. It will be
published here when builds pass their gates (CI green, verified APK,
screenshot diffs, drive-test checklist per build). Until then, this repo
contains only the plan, the licence, and the icon, so nothing here claims a
build that does not exist yet.

## Upstream credit

AutoMap is built on Vela Maps by PimpinPumpkin, GPLv3. Map data and place
data downloads in the app continue to come from upstream Vela's hosting
until AutoMap mirrors them. AutoMap has no donation button; upstream
donations are not diverted through this project.
