# AutoMap — release notes (builds 0–19)

AutoMap is a slipstream of Vela: the same app underneath, with a new look and
new car-map behaviour. It installs alongside Vela (package `app.automap`),
and your lists and routes move across with Vela's own file backup/restore.
This build is debug-signed for testing — it is not a store release.

## What is new

- **A new name and icon.** Everywhere you used to see Vela, you now see
  AutoMap, with a new map-style icon (highway interchange, grass, buildings
  and a pond — no arrows, no trees, no text). The "call it just Maps" name
  is still available as an opt-in. The Support/donation button is gone
  entirely; Vela is credited in About instead.
- **A new map palette, day and night.** Colours sampled from the new Google
  Maps look: warm tan land and deep-blue routes by day; deep slate
  buildings, teal-green parks and a bright cyan route at night. You can
  choose Automatic, Day or Night, and 2D or 3D maps, in Appearance.
- **A realistic car icon** instead of the blue arrow, on phone and car.
- **A calmer car map while navigating.** Place labels, shields, trails and
  address overlays step aside during navigation; traffic lights and stop
  signs are bigger, appear sooner, and have a white halo.
- **3D buildings in the car**, growing in from zoom 17 to 19 as on the
  phone, with a cap so dense downtowns stay smooth. Buildings also fade
  away as you approach a turn (on by default, switchable in Performance),
  so the turn itself stays visible.
- **Smarter auto-zoom.** The car camera now tightens for complex turns,
  forks, ramps, roundabouts and tight next legs, and eases for multi-lane
  roads — on top of the existing speed zoom. Panning, pinching or the zoom
  buttons now hold the camera until you tap recenter; they are no longer
  overridden.
- **Painted road markings in the car.** Lane lines, crosswalks, stop lines,
  centre lines, medians, bike lanes and turn arrows, drawn at real scale
  from OpenStreetMap tags (switchable in Map settings). At splits and
  exits, the arrows for the way you need stay bright while the others dim.
  Markings only appear where the map actually has the tags — where it
  doesn't, nothing is drawn rather than guessed.
- **Clearer overpasses and exits, in 2D.** Bridge casings are wider, bridge
  decks carry a soft shadow, exit shields are larger, and the route glows
  wider where it takes a ramp or fork.
- **A destination arrival view.** As you arrive, the camera pulls back to
  hold you and the destination together, the destination gains a highlight
  ring, you get an "On your left/right" pill when the route knows the side,
  and nearby car parks appear as plain places — never as a recommendation.
- **Terrain shading in the car (off by default).** The same hillshade the
  phone offers, under the Topography switch — shaded relief for the wider
  view, not 3D terrain, and hidden during navigation.
- **Sun-angle building shading.** 3D buildings shade by the sun's actual
  position for your location and time of day (switchable in Map settings).
  At night the buildings keep their night colours — never washed out.

## Deliberately not in here

No tree icons of any kind; no 3D bridges or bridge transparency; no weather
simulation; no satellite view in the car; no AI landmarks; no conversational
guidance. The basemap's green woodland areas are unchanged — they are
landcover, not icons.

## Things still to decide or test

- Which region's road markings to publish first, and where to host them —
  the bake pipeline is proven on a northern-Kentucky technical pilot, but no
  region or hosting has been chosen yet.
- Whether Automatic is the default colour scheme on first install, and
  whether sun-angle shading stays on by default — both are set as described
  above and can be corrected in one word.
- On-device checks: screenshots and drive tests on dense-city, highway and
  interchange routes, day and night, plus backup/restore — these need a
  real car and phone and are owed, not claimed here.
- Release signing: this build is debug-signed. A release key must be
  created and backed up before any published release.
