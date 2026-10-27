# AutoMap build log (slipstream of Vela at 85eb69c)

## Environment fixes (local build config only, NOT source patches)
- Gradle daemon could not connect in this VM until `JAVA_TOOL_OPTIONS=-Djava.net.preferIPv4Stack=true`
  (daemon bound to an IPv6-mapped socket; client dispatch reset without it). Verified on a trivial
  Gradle project first (HELLO-OK, Gradle 8.14.6 and 9.8.0).
- Gradle user home isolated to `~/workspace/automap/.gradle-home` (shared `~/.gradle` journal lock
  from another daemon blocked the build).
- System JDK 21 has a broken cacerts symlink (`/etc/ssl/certs/java/cacerts` missing). Using
  Temurin 21 at `~/workspace/jdk21` (real cacerts) as JAVA_HOME.
- Egress proxy re-signs TLS with "Hatch Sandbox Egress CA" (`/etc/ssl/certs/hatch-egress-ca.pem`).
  Imported into `~/workspace/automap/.gradle-home/cacerts-automap`, referenced via
  `javax.net.ssl.trustStore` in JAVA_TOOL_OPTIONS in `build-logs/run-gradle.py`.
- Java 17 toolchain required by `:osmand-shaded` (toolchain languageVersion 17). Downloaded
  Temurin 17.0.21 to `~/workspace/jdk17`, registered via
  `org.gradle.java.installations.paths` in `gradle.properties` (local build config only).
- Build runner: `build-logs/run-gradle.py` (proxy from environment, ANDROID_HOME,
  GRADLE_USER_HOME, JAVA_HOME, JAVA_TOOL_OPTIONS). It contains no secrets; proxy credentials
  are read from the environment at run time, never written to a file by this script.
- `gradle.properties` local change: `org.gradle.java.installations.paths` added (see above).
  No source patch has been applied yet.

## Build 0 — rename + icon applied, rebuild in progress
- Worktree: `~/workspace/automap`, cloned from `~/workspace/vela-feasibility`, checked out at
  upstream commit 85eb69c ("Reach snapshot for 2026-10-26").
- Rename applied via `patches/build0-rename.py` PLUS a finish pass (the script died at step 5:
  its DonatePrompt anchor '/** The filled primary button' does not exist upstream — the
  composable is the file's last block). Finish pass removed: DonatePrompt composable
  (WelcomeScreen), showDonatePrompt/DONATE_URL/openDonate/dismissDonatePrompt (Onboarding),
  the showDonatePrompt term in MapViewModel's prompt gate (missed by the original script),
  the settings_support search-index line (SettingsHub), and the donate_no_browser reuse in
  WhatsNew.openRelease (now uses the existing no_browser_installed key). Verified after:
  base strings.xml contains 0 'Vela'; no user-visible 'Vela' string literal in Kotlin;
  app_name = AutoMap, app_name_generic = Maps (unchanged, opt-in alias).
- Icon applied: adaptive foreground = full-bleed PNG from the locked
  `automap-icon-ui.png` (transparent rounded corners mirror-filled so no seam under any
  launcher mask), background = solid #D2D4C2 (edge average), monochrome = simplified
  vector map glyph (crossing highways + ramp + pond, no arrow, no trees, no text).
  48px render still reads as a map (roads + pond). Old vector chevron foreground deleted.
- AutoSpace companion (uncommitted, worktree only — that worktree has unrelated
  pre-existing changes, so nothing was committed): `gearslip/.../host/KnownApps.kt`
  partial set gains "app.automap" (preferred over app.vela when both installed).
  RecommendedApps entry deferred: no published AutoMap release URL exists yet.
- Rebuild as `-PappId=app.automap` (build-logs/build0.log): BUILD SUCCESSFUL in 2m 51s.
  VERIFIED Build 0: APK build-logs/apk-build0/automap-build0.apk, 138 MB, package
  app.automap, version 0.3.0/code 1, label AutoMap (all locales), debug cert
  70aa9404..., sha256 e2e76aab3521ea28bbd9ed670a90abfc2d382a06bb19dc748c44f701b50a1187,
  icon PNG 624,866 B present in the APK (res/drawable-nodpi-v4/ic_launcher_foreground.png).
  AutoSpace companion: KnownApps partial set gains app.automap (uncommitted worktree change;
  RecommendedApps entry deferred — no published AutoMap release URL yet).

## Builds 1-3 — VERIFIED (one APK, build-logs/apk-build123)
- Day palette (Build 1) + night palette (Build 2) applied together: both are branches of
  applyMapTheme and cannot be meaningfully separated into two APKs; each value was
  verified separately in the artifact (see below). Build 3 controls + final icon layers
  (icon itself landed in Build 0; Build 3 verified the 48px legibility render).
- Build 1 day: land #f8edd7, water #99daf7, park/grass/wood/wetland #cef3c9,
  buildings #e9e9ea, roads #a8b6c4, motorway #8aa4c0 unchanged, route #1F6FEB unchanged.
- Build 2 night: MODERN_DARK land #1c2a4a, water #0b1e3a, green #2f7d6e, pitch #1f5f52,
  minor #5a6f8e, service #3a4a66, major/motorway #7a8fb5, building #566a96,
  buildingLine #6a7da8, halo #141d2e; route freeflow cyan #3FE0D4 at night via
  MapScreen.routeTrafficColor(route, mapDark) AND VelaMapView.routeGradient dark-aware
  (new ROUTE_FREEFLOW_NIGHT, passed at all 6 call sites; the empty-spans outline call
  is unaffected). AMOLED untouched.
- Build 3: Appearance gets Color scheme Automatic/Day/Night (FOLLOW/LIGHT/DARK
  underneath, unchanged) and Map detail 2D/3D (new MapDetail pref, default 3D; phone
  b3dKeep gated, car tilt flattened + building-3d hidden in 2D via applyOverlays).
  New string keys are base-only (translators fill locales later, per upstream
  check-translations practice: missing = warning, not failure). One fix during the
  build: the strings edit initially deleted settings_map_theme_title, breaking
  SettingsHub compile (CANNOT_INFER_PARAMETER_TYPE at listOf) — key restored.
- VERIFIED: GRADLE_EXIT 0, APK build-logs/apk-build123/automap-build123.apk, 140 MB,
  package app.automap, sha256 af5a10446cd60ddc8ff76e950a1e6945e6326db631bbbc06e693c87986576d25;
  dex contains f8edd7, 99daf7, cef3c9, 1c2a4a, 566a96, 3FE0D4; resources.arsc contains
  "Color scheme", "Automatic", "Day", "Night", "Map detail".

## Builds 4-6 — VERIFIED (build-logs/apk-build456)
- Build 4: default puck shape is now CAR (PuckStyle.init default SHAPE_CAR); a saved
  choice still wins, and the shape selector remains. Car uses the same navPuckBitmap.
- Build 5: car nav declutter ported (applyOverlays, gated on TurnDeclutterPref):
  poi_r1/r7/r20/transit, vela-transit-stops, highway shields, vela-bikeroutes/trails/
  transit, label_other/village, one-way arrows, vela-addr-* hide during nav, restore
  after. Hillshade deliberately excluded (its own toggle, Build 17).
- Build 6: car signs — gate 13.5 → 12.5, glyph scale up (~30%, cap 56 → 72 px),
  ring stroke 1.5 → 2.5, white halo disc behind each sign bitmap. Phone — controls
  icon-size stops 0.98/1.37/1.95 → 1.22/1.71/2.44 (both creation and scaled paths),
  white icon halo 1.5. Phone thresholds NOT lowered (nav 15.4 / browse 19.0 are
  user-tuned values; the lower threshold is the car's).
- VERIFIED: GRADLE_EXIT 0, APK build-logs/apk-build456/automap-build456.apk, 142 MB,
  sha256 8000ecdf7c668cac5cffc0931defa5d587d39deeb6d0bb0c4dd57829fd861a55.

## Builds 7-8 — VERIFIED (build-logs/apk-build78)
- Build 7: verified the car already inherits the phone's building-3d geometry
  (applyBuilding3dGeometry: minzoom 17, 30% height at z17 → full at z19) and the new
  palette building colours (day #e9e9ea, night #566a96) through shared applyMapTheme.
  Added the dense-downtown cap: snapshotter long edge capped at 2048 px (ordinary
  head units ≤1920 unaffected; draw() already stretches the bitmap to the surface
  and scales overlays by sx/sy, so the cap cannot break projection). Both the
  Options size and the per-frame setSize use the capped size.
- Build 8: car turn-approach building fade (FadeBuildingsOnTurn pref, default on,
  toggle in Performance settings next to turn declutter): opacity eases per tick
  toward 0.25 within 150 m of the next turn, back to 1.0 past 400 m, via the
  snapshotter's building-3d fillExtrusionOpacity. Per-building occlusion NOT built
  (agreed deferral). Fade state resets on style reload.
- VERIFIED: GRADLE_EXIT 0, APK build-logs/apk-build78/automap-build78.apk, 145 MB,
  sha256 d1e8631d6390531bc55bb889ab6640f7579072ded6e6e394f8e63651e0be8c43.

## Builds 9-10 — VERIFIED (build-logs/apk-build910)
- Build 9: CarFraming.contextZoomDelta (pure, JVM-testable): distance to turn
  (≤150 m +0.7 → 0 at ≥400 m), maneuver type (fork/ramp/roundabout +0.4, sharp/U-turn
  +0.5, turn +0.2, merge/keep/slight +0.15), lane count (4 lanes −0.15, 5+ −0.3),
  tight next leg (<200 m +0.3, <500 m +0.15); clamped [−0.5, +1.0], added to the
  speed/turn-framing zoom and clamped to [13.0, 18.5]. The renderer scores the next
  non-quiet maneuver (same quiet-skip as NavTurnTarget.pointFor).
- Build 10: user pan/pinch/zoom-button is now a HARD camera override
  (userCamOverride, set in stopPanning/zoomBy, cleared only by follow() or leaving
  overview; auto-recenter respects it). Overview was already hard. Pitch/easing:
  verified existing — bearing/tilt/zoom ease per 70 ms ticker tick (CAM_BEARING_EASE
  0.3, TILT_EASE 0.05, ZOOM_EASE 0.06); the context delta rides that easing, so no
  new easing mechanism was invented.
- VERIFIED: GRADLE_EXIT 0, APK build-logs/apk-build910/automap-build910.apk, 145 MB,
  sha256 de0a0469fa917951ffadd4ed05e0251694aa8bf8574ec39f78c0caaeb8fa3251.

## Build 11 — VERIFIED (build-logs/apk-build11)
- PaintedRoads car port: CarStyle.inject adds the vela-paint-baked vector source
  (pmtiles:// + the configured bake URL) and the 8 paint layers as JSON (real-metre
  width expressions translated verbatim, minzoom 16.5, above the last bridge layer),
  gated on the Vela look. Theme colours + arrow images applied in applyTheme
  (snapshotter addImage works; bitmap density set to the car's). Visibility follows
  the new PaintedRoadsPref setting (default on) via applyOverlays. Phone: the dial
  still picks the source, but the setting gates it off entirely; toggle in Map
  settings. Arrow icon-size uses the car's density (documented approximation only
  in that the snapshotter renders at pixelRatio 1.0).
- VERIFIED (re-checked from the artifact 2026-10-27): GRADLE_EXIT 0, APK
  build-logs/apk-build11/automap-build11.apk, 140 MB, package app.automap,
  version 0.3.0/code 1, sha256
  95d963e7b76f77eec8a56707f190a2c935c9dae9ae0b8e200a0ef556384a73e5;
  dex contains vela-paint-baked and vela-paint-arrow; resources.arsc contains
  "Painted road markings".
- SDK licences: the workspace SDK's licence files predated the platform-37 licence hash; fixed
  by running sdkmanager --licenses against the ABSOLUTE sdk path (an earlier run with
  `--sdk_root=~/...` silently wrote into a stray `/home/hatch/~/workspace/...` tree, which also
  swallowed the build-tools 36.0.0 install; that install was moved into the real SDK).
- Gitignored runtime files (CI fetches them the same way): core/libs osmand-java.jar
  (18,663,092 B), osmand-shared-jvm.jar (1,756,140 B), gnu-trove-osmand.jar (327,840 B),
  kxml2-vela.jar (37,252 B); app/libs cronet-155.0.8059.16.aar (14,890,158 B),
  sherpa-onnx-1.13.3.aar (57,044,841 B) — all from upstream PimpinPumpkin/Vela releases
  (obf-runtime / cronet-runtime / tts-runtime tags), all passing CI's size thresholds.
- BASELINE VERIFIED (build baseline15): BUILD SUCCESSFUL in 18m 21s. APK
  `app/build/outputs/apk/debug/app-debug.apk`, 137 MB, package app.vela, versionCode 1,
  versionName 0.3.0, debug-signed (Android Debug cert SHA-256
  70aa9404cfd61c119d9f043719b8caf527fd2e10eb3dad485eb5979b3f124cc5).

## Build 12 — PIPELINE VERIFIED, product region still open (technical pilot only)
- Tools restored 2026-10-27: osmium-tool 1.16.0 / tippecanoe 2.49.0 (a system apt
  replay held the dpkg lock briefly, then the cached .debs installed cleanly).
- Source: Geofabrik Kentucky extract dated 2026-10-26 (kentucky-261025.osm.pbf,
  154,863,410 B; BBBike Cincinnati still 404). Clipped to a northern-Kentucky bbox
  (-84.9,38.7,-84.3,39.2: Cincinnati KY suburbs) purely as a TECHNICAL pilot to
  verify the pipeline — this is NOT the product pilot-region decision, which
  remains Robert's call, and nothing was uploaded (no --upload, no hosting choice).
- Pipeline run end-to-end (scripts/bake-painted-roads.sh steps, Gradle via
  run-gradle.py): osmium tags-filter -> osmium export (31,056-line geojsonseq)
  -> :core:testDebugUnitTest '*PaintedRoadsBakeTest' GRADLE_EXIT 0 ->
  tippecanoe to build-logs/bake-pilot/painted-pilot-nky.pmtiles (1,003 KB,
  layer paint, z15 only).
- COVERAGE REPORT (no invented markings: marks derive only from OSM tags/geometry;
  untagged ways get no lane/arrow marks): input 19,080 ways (5,915 lanes-tagged,
  858 turn:lanes-tagged) + 8,088 nodes (6,552 crossing, 659 traffic_signals,
  877 stop) -> 31,323 marks: SURFACE 6398, LANE 7842, CENTER 6611, ARROW 2800,
  BIKE 572, CROSSWALK 3225, STOP 2940, MEDIAN 935. Arrow icons:
  arrow-left 1283, arrow-right 803, arrow-through 272, arrow-left-through 176,
  arrow-through-right 243, arrow-left-through-right 9, arrow-left-right 13,
  arrow-uturn 1.
- Bake output lives only under build-logs/bake-pilot/ (NOT bundled into any APK);
  bundling/hosting awaits the pilot-region + hosting decisions.
- Build 12's product gate (which region to ship, where to host it) is OPEN and is
  listed as outstanding in the final report; the pipeline itself is verified.

## Builds 13-14 — VERIFIED (build-logs/apk-build1314)
- Build 13 (staged rollout, mechanism + stage 1): stage 1 is the Build 12
  technical pilot, written up as build-logs/bake-pilot/COVERAGE.md (source,
  pipeline steps, input/output counts, the ~31% lanes-tag coverage limit).
  Stage 2+ = the same script per region Robert names, one coverage report
  each, published only after a hosting decision. No region was added silently
  and no bake output was bundled into an APK.
- Build 14 (lane emphasis at splits/exits, car): CarMapRenderer now dims
  non-matching baked turn ARROWS to 0.25 within 400 m of a mappable turn
  (plain left/right turns + U-turns only; forks, ramps, roundabouts, merges
  and keeps are NOT guessed), via an icon-opacity expression (zoom fade-in
  x a substring case on the feature's `icon` property, verified against the
  MapLibre 13.6.1 Expression API: `in(String, Expression)` + `switchCase`).
  Lane LINES are untouched — they carry no direction. The next-non-quiet
  maneuver lookup is shared with Build 9's context zoom; emphasis state
  resets on style reload like the other overlay states.
- VERIFIED: GRADLE_EXIT 0, BUILD SUCCESSFUL in 59s (gradle.log tail),
  APK build-logs/apk-build1314/automap-build1314.apk, 138 MB, package
  app.automap, version 0.3.0/code 1, sha256
  86e9207a422aec4c4bcc6fd3a05864c2dd4ee0061abdaa82065ba7e66045d40a;
  dex contains laneEmphasisApplied, emphasisDirection, vela-paint-arrow.

## Build 15 — VERIFIED (build-logs/apk-build15)
- Overpass/exit emphasis, car, 2D only (no 3D bridge geometry — the open stack
  cannot express it, lines extrude nothing): CarStyle.inject widens every
  bridge_*_casing line-width x1.3, inserts a `vela-bridge-shadow` line under
  the first bridge layer (black, 0.20 opacity, offset 2.5 px, blurred) as the
  deck shadow, and enlarges exit shields (icon-size 1 -> 1.35, text-size
  10 -> 13) whenever visible. The Build 5 declutter still hides shields
  during nav — noted, not silently weakened.
- Ramp route emphasis: drawRoute draws a wider translucent halo (30 px, alpha
  110) over ~200 m of route around each RAMP/FORK maneuver's location, driven
  only by the route's own maneuvers — no road class guessed from the polyline.
- VERIFIED: GRADLE_EXIT 0, BUILD SUCCESSFUL in 31s (gradle.log tail),
  APK build-logs/apk-build15/automap-build15.apk, 138 MB, package
  app.automap, version 0.3.0/code 1, sha256
  a6fb2c00a81faa330849d880ddf547e6239bd95ef01844117b8ade703fa7dfc1;
  dex contains rampHaloPaint, vela-bridge-shadow, laneEmphasisApplied.

## Build 16 — VERIFIED (build-logs/apk-build16)
- Destination arrival, car: within 300 m of the destination the camera pulls
  back (never in) just enough to hold puck + destination in frame (same span
  math as framePoints); the destination pin gains a highlight ring (the
  entrance-area marker — no OSM entrance point exists in the route model, so
  none is invented) and, when the ARRIVE maneuver knows it, an "On your
  left"/"On your right" pill (null side draws no pill, never a guess).
- Nearby car parks as nearby places (NOT a recommendation): a new
  `vela-parking` symbol layer in the car style (source poi, filter
  subclass=parking per the OMT schema, minzoom 15, hidden by default) with a
  code-drawn blue P badge image, shown only inside the arrival window.
  Polygon-only car parks with no POI point do not appear (data limit, not
  filled in). The Build 5 declutter is untouched — no POI tier is unhidden.
- VERIFIED: GRADLE_EXIT 0, BUILD SUCCESSFUL in 26s (gradle.log tail),
  APK build-logs/apk-build16/automap-build16.apk, 138 MB, package
  app.automap, version 0.3.0/code 1, sha256
  10c9154a432877f40b333587024fb621a743f804eabe61b3fc4513b144d563b3;
  dex contains vela-parking, arrivalRingPaint, "On your left", rampHaloPaint.

## Build 17 — VERIFIED (build-logs/apk-build17)
- Terrain relief in the car as a toggle OFF by default: CarStyle.inject adds
  the terrarium DEM source (AWS Open Data, keyless) + a `vela-hillshade`
  layer (exaggeration 0.32, below the first road layer, capped at z16) with
  the phone's day colours; the renderer re-colours it per theme at night
  (MODERN_DARK shadow #0a1018 / highlight #3a4a68 / 0.45). Visibility follows
  the EXISTING Topography toggle (same pref as the phone, still off by
  default) and mirrors the phone: shown for overview/browse, hidden during
  nav so the route stays clean. Shaded relief only, never 3D terrain.
- VERIFIED: GRADLE_EXIT 0, BUILD SUCCESSFUL in 32s (gradle.log tail),
  APK build-logs/apk-build17/automap-build17.apk, 138 MB, package
  app.automap, version 0.3.0/code 1, sha256
  48023f5b5d50d879b5c686c9107e4f8c833630ce6b510247fef3c379edd9340a;
  dex contains vela-hillshade, terrarium, Topography.

## Build 18 — VERIFIED (build-logs/apk-build18)
- Sun-angle building shading, car, with toggle: new pure function
  `core/.../data/SunAngle.kt` (NOAA-style approximation, ~1 degree) gives
  azimuth/elevation from lat/lng/time; CarStyle.inject takes an optional
  style-light position [1.15, azimuth, 90-elevation] baked at style load
  (the snapshotter exposes no light setter — verified via javap — so the
  angle moves only on style reloads: theme flips, surface recreations).
  Intensity stays at 0.25 so a low sun never washes anything out; at night
  (elevation <= 0), with the toggle off, or with no GPS fix yet, no position
  is passed and the default light stands — night never washed out. New
  SunShading pref (default ON, following the time-of-day palette — a default
  choice for Robert to correct) + toggle in Map settings next to Topography
  + search index. Applies on the patched-style path only; the plain-URL
  fallback keeps the default light, as it does for every injected layer.
- VERIFIED: SunAngleTest 4/4 green (:core:testDebugUnitTest), then
  GRADLE_EXIT 0, BUILD SUCCESSFUL in 1m 39s, APK
  build-logs/apk-build18/automap-build18.apk, 138 MB, package app.automap,
  version 0.3.0/code 1, sha256
  58c8d9f9632f072971dd089b2b521dd27fa6f67a1c9ded45a85e65ab91edb734;
  dex contains sunPosition, SunShading, vela-hillshade; resources.arsc
  contains "Sun-angle building shading".

## Build 19 — VERIFIED (final APK = build-logs/apk-final/automap-final.apk)
- Final APK is the Build 18 artifact, byte-identical (same sha256 below);
  no source changed after it, only docs/diff files.
- Regression, code-level (this VM): full :app:assembleDebug green;
  22/22 feature markers pass in the final APK dex/arsc (day + night palette
  hexes, paint/hillshade/parking/bridge-shadow layer ids, lane emphasis,
  ramp halo, arrival ring, sun position, context zoom, fade, MapDetail,
  "Color scheme", "Map detail", "Painted road markings",
  "Sun-angle building shading"); icon PNG 624,866 B present; label AutoMap
  (all locales); package app.automap, 0.3.0/code 1; base strings.xml
  contains 0 "Vela"; changed-file audit shows no routing, traffic, places,
  search, voice-publishing or backup code touched; no tree feature added
  (only "trees" hit is the icon comment itself); AutoSpace KnownApps entry
  for app.automap still present (uncommitted there).
- Regression, code-level tests: full :core:testDebugUnitTest GRADLE_EXIT 0 —
  182 suites, 1248 tests, 0 failures, 0 errors, 20 skipped
  (build19-coretests.log).
- GPL publication (local): automap-patch-stack.diff (11,855 lines: 43
  tracked-file diffs + 8 new files; icon PNG noted by hash, not in the text
  diff; gradle.properties excluded as local build config) + build script
  build-logs/run-gradle.py + runbook BUILD_AND_SYNC.md + release notes
  RELEASE_NOTES.md. Nothing pushed to any repo: the worktree remote is the
  local feasibility clone, and the private DocBobert6/AutoMap repo (created
  2026-10-26, README says no release/APK yet) awaits Robert's word before
  any push — no binary has been conveyed, so no GPL conveyance is triggered.
- Device-level regression OWED (no paired device in this VM, not claimed):
  screenshots + drive tests (dense-city/highway/interchange, day/night,
  2D/3D), snapshot timing/memory/legibility vs Build 0 baseline, offline,
  backup/restore, AutoSpace turn publishing.
- Release signing: NOT DONE — all builds debug-signed. SIGNATURE CHANGE
  BETWEEN SESSIONS (verified with apksigner on each APK): builds 0–11 carry
  Android Debug cert SHA-256 70aa9404cfd61c119d9f043719b8caf527fd2e10eb3dad485eb5979b3f124cc5,
  while builds 13–18 and the final APK carry a DIFFERENT debug cert,
  SHA-256 4754268cd20cd4b6cd918b85f83d22fc32e5cf8b69fd46bc8f60f42483f5d3d8
  (SHA-1 b67530d5c9dde18db03d31a630f11ac5ff91026c), from the debug
  keystore created in this session at /root/.android/debug.keystore
  (2026-10-27 01:08). Consequence: the final APK will NOT install over a
  builds 0–11 APK — uninstall first. A release keystore must be created
  under ~/workspace/automap/ and backed up before any published release;
  losing it would block all future update installs.

## Follow-up — Robert's three decisions (2026-10-26 22:01 EDT, implemented 2026-10-27)
- Decision 1, pilot regions (hosting STILL undecided — bakes prepared only,
  NOT uploaded, NOT bundled into any APK):
  Pipeline = Build 12 (osmium tags-filter + export + PaintedRoadsBakeTest +
  tippecanoe -l paint -z15 -Z15 -P -b 24). Sources: Geofabrik Kentucky
  2026-10-26 (build-logs/bake-pilot/kentucky.osm.pbf, 148 MB) and Geofabrik
  Ohio 2026-10-27 (build-logs/bake-regions/ohio.osm.pbf, 312 MB).
  - Northern Kentucky, bbox -85.1,38.5,-84.2,39.1 (Kentucky only): Florence,
    Covington, Newport, Dry Ridge (38.68,-84.53), Grant County, I-75/I-71
    south of the Ohio River; excludes Cincinnati/Ohio, Maysville, Louisville.
    Input 21,325 ways (6,063 lanes 28.4%; 873 turn:lanes); 6,460 crossing +
    672 traffic_signals + 852 stop nodes. Bake 33,157 marks.
    Output build-logs/bake-regions/northern/painted-northern-kentucky.pmtiles
    (1.4 MB). Supersedes the stage-1 technical pilot bbox
    -84.9,38.7,-84.3,39.2 for product purposes.
  - Cincinnati metro, bbox -84.9,38.8,-84.0,39.5 (Kentucky + Ohio merged
    with osmium merge, then clipped once, so state-line ways are not
    double-counted; Kentucky alone would have missed the Ohio half):
    Cincinnati + Ohio suburbs (Mason, West Chester, Blue Ash, Sharonville,
    Milford), Kentucky river suburbs, I-75/I-71/I-275; excludes Dayton
    (39.76, north of 39.5), Columbus, Louisville. Input 86,546 ways
    (27,837 lanes 32.2%; 3,630 turn:lanes); 30,895 crossing + 3,518
    traffic_signals + 4,038 stop nodes. Bake 153,621 marks.
    Output build-logs/bake-regions/cincinnati/painted-cincinnati-metro.pmtiles
    (3.8 MB). Overlaps the northern-Kentucky bbox by design (metro spans
    the state line); which file a client loads is part of the undecided
    hosting decision, not papered over.
  - Central Kentucky, bbox -85.3,37.6,-84.0,38.5 (Kentucky only): Lexington,
    Frankfort, Richmond, Danville, I-64/I-75; excludes Louisville (west of
    -85.3), northern Kentucky north of 38.5, points east of -84.0.
    Input 36,271 ways (4,269 lanes 11.8%; 391 turn:lanes); 5,855 crossing +
    1,108 traffic_signals + 549 stop nodes. Bake 34,155 marks.
    Output build-logs/bake-regions/central/painted-central-kentucky.pmtiles
    (2.4 MB).
  Coverage report: build-logs/bake-regions/COVERAGE_REGIONS.md. Limit stated
  there: lane tagging is sparse by construction (28.4%/32.2%/11.8% with
  lanes, only a few percent with turn:lanes, thinnest in central Kentucky);
  OSM tagging reality, no invented markings.
- Decision 2, Automatic colour scheme default: CONFIRMED, no code change.
  app/.../ui/theme/AppTheme.kt: mapMode initial FOLLOW, prefs load defaults
  to "FOLLOW" with fallback FOLLOW. FOLLOW is the Automatic equivalent.
- Decision 3, SunShading off by default + first-boot prompt:
  app/.../ui/SunShading.kt: mutableStateOf(false), prefs default false
  (was true in both). app/.../ui/Onboarding.kt: showSunShadingPrompt state
  + sun_prompt_done pref in vela_onboarding; armVoiceOrSun() arms the voice
  offer if still owed, otherwise the sun offer; dismissVoicePrompt,
  dismissLocationPrompt, dismissNotifPrompt, completeWelcome and init all
  route through it, so the prompt appears once, never stacks on an
  unanswered voice prompt, and existing installs upgrading to this build
  get it when welcome is done and the question was never answered.
  dismissSunShadingPrompt(context, enable) records sun_prompt_done=true
  once either way and calls SunShading.set(context, enable), so only an
  explicit yes enables it; skip/dismiss leaves it OFF.
  app/.../ui/VelaRoot.kt: SunShadingPrompt composable (VelaDialog) between
  the voice prompt and WhatsNew. Strings (values/strings.xml,
  translatable="false"): root_sun_title "New: sun-angle building shading",
  root_sun_body (position of the sun, night keeps night colours, takes
  effect on map style reload, Settings Map to change), root_sun_enable
  "Enable shading", root_sun_skip "Keep it off". Settings toggle already
  existed (MapSettings next to Topography, settings_sun_shading) and was
  not moved. Everything else unchanged: NOAA approximation
  (core/.../data/SunAngle.kt), intensity 0.25, style-load-only angle
  updates, night/no-GPS/toggle-off guards keeping the default light.
  Build-line note: first rebuild failed on :app:mergeDebugResources —
  root_sun_body carried a doubled backslash before an apostrophe
  (Invalid unicode escape sequence). Fixed by rewording the string with
  no apostrophe at all; rebuilt green. Do not reintroduce an escaped
  apostrophe in that string.
- Follow-up local APK (debug-signed, VM-verified, NOT the published one):
  build-logs/apk-followup/automap-followup.apk, 140 MB,
  sha256 9511309ab96924004f7bb6a4935140d5cf65a747768b1137001d5193a304f133,
  aapt2: package app.automap, versionCode 1, versionName 0.3.0, label
  AutoMap (all locales), icon ic_launcher_foreground present, apksigner
  debug cert SHA-256 4754268cd20cd4b6cd918b85f83d22fc32e5cf8b69fd46bc8f60f42483f5d3d8
  (same as builds 13-18/final, NOT builds 0-11 — uninstall-first
  consequence stands). Dex contains dismissSunShadingPrompt,
  showSunShadingPrompt, sun_prompt_done, root_sun_title.
  Tests: :core:testDebugUnitTest --tests '*SunAngleTest' GRADLE_EXIT 0,
  4 tests, 0 skipped, 0 failures, 0 errors. No first-boot/preference
  unit test was added: the prompt lives in Compose + SharedPreferences
  onboarding code with no Robolectric harness in this tree; its
  once-only/persist/respect behaviour is verified by code inspection
  (paths above) and by dex presence, not by a unit test — stated here
  rather than claimed.
- Device verification still OWED (not claimed): screenshots, drive tests,
  snapshot timing, backup/restore, AutoSpace turn publishing on Robert's
  device.

## Follow-up publishing — GitHub (2026-10-27)
- Route used (after direct upload was proven blocked): GitHub Actions
  release workflow, NOT a chat attachment, NOT a Contents-API file push
  (APK ~140 MB > 100 MB limit). Repo DocBobert6/AutoMap main now holds
  only: README, PLAN, LICENSE, docs/icon.png, automap-patch-stack.diff
  (2.1 MB, regenerated with --binary so the icon PNG rides in it; 50
  files; verified locally with `git apply --check` against a fresh
  archive of upstream 85eb69c), and
  .github/workflows/automap-debug-release.yml (workflow_dispatch only).
  Push route: custom.github push-tree, commit 58ce5091, then fix commit
  7c262971 (see below). Tree read back from GitHub: 9 entries, main
  head 7c262971.
- First CI run 40437752010 FAILED at step 3 (Checkout upstream Vela):
  actions/checkout treats `ref` as a branch/tag name, fetching
  refs/heads/85eb69c* and refs/tags/85eb69c*, which do not exist.
  Fixed by fetching the pinned SHA directly (git fetch --depth=1 origin
  85eb69c946711df272b54d3f4305e72746ce1035; verified locally first).
  Direct upload to uploads.github.com with the surrogate still 401s
  (request_id 552D:3EB274:15AE04:1C4325:6AE00867, recorded earlier);
  the CI route is the working one.
- Second CI run 40438033302 SUCCESS, all 13 steps green (patch apply,
  obf/cronet/TTS fetches from upstream Vela releases, assembleDebug
  -PappId=app.automap, gh release upload --clobber to the existing
  prerelease tag v0.3.0-automap-debug, release id 424963865).
- Published asset VERIFIED by read-back from GitHub:
  https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-automap-debug
  asset AutoMap-0.3.0-debug.apk, 143,728,912 bytes, download URL
  https://github.com/DocBobert6/AutoMap/releases/download/v0.3.0-automap-debug/AutoMap-0.3.0-debug.apk
  Downloaded copy via authenticated API: 143,728,912 bytes (exact match),
  sha256 19204355f205040ee9f5e6179a4699084bfe92773e0887a587455829c4092923,
  aapt2 package app.automap, versionCode 1, versionName 0.3.0, label
  AutoMap, icon present, dex contains dismissSunShadingPrompt,
  sun_prompt_done, root_sun_title.
- CERT CAVEAT (important, read before installing): the CI-built APK is
  signed with the GitHub runner's ephemeral debug keystore, cert
  SHA-256 89565e1ec975965bd1e5a5f23d6472b7d4640dd41a486e0ddcabdb2cba5624a4
  (SHA-1 d378f49583f02f7db75dd09265437c4e9899d495) — DIFFERENT from both
  local debug certs (builds 0-11: 70aa9404...; builds 13-18 + local
  follow-up: 4754268c...). It will NOT install over any earlier local
  build; uninstall first. Every CI debug build gets a fresh runner
  keystore, so CI debug builds cannot update over each other either.
  A release keystore is still not created; that remains the fix for
  stable updates.
- Release body (as created earlier) already states: debug-signed, no
  release keystore yet, device verification still owed, region bakes
  prepared not hosted with hosting undecided, will not install over
  builds 0-11 APKs. Unauthenticated curl of the download URL 404s
  because the repo is private; Robert, as owner, downloads it signed
  in via the release page.

## Altima top-down 3D puck + vehicle colour picker (2026-10-27)
- Scope decided by Robert 2026-10-26 22:51-22:52: a top-down realistic 3D
  Nissan Altima, used as BOTH the map puck and the settings preview
  (top-down 3D in settings too, no side view anywhere). The side profile
  in the AutoSpace icon was the model reference only (sedan proportions).
- Rendering, tintable by design (no baked raster colour):
  app/.../ui/map/Puck3D.kt: car() replaced with an Altima sedan model
  (2.20 long, 0.86 wide), raised hood and trunk panels, glass cabin with
  body-coloured roof and sunroof, mirrors, wheels with silver hubs, front
  grille, headlights and taillights. Body fill is the picked colour;
  edges are a darker shade of it and the roof a lighter one, so shading
  works over light and dark colours. altimaTopDownPreviewBitmap() renders
  the same mesh top-down at a low tilt, nose up, for settings.
  app/.../ui/map/VelaMapView.kt: drawCarPuck() (flat symbol path) updated
  to the same Altima proportions and hex body colour, with wheels,
  mirrors, hood and trunk seams, and sunroof.
- Colour preference: app/.../ui/NavChrome.kt: PuckStyle.carColor is now a
  hex string (#RRGGBB) under the same key puck_car_color, default
  #D93025 (the old red). normalizeCarColor() maps saved legacy names
  (red/blue/white/green/yellow) to their exact old hexes, so a user who
  never opens the picker keeps their colour. Invalid values are ignored
  by setCarColor, never applied. The map puck, car screen bitmap, and
  settings preview all read the same value, so the style key changes and
  the puck re-registers on a colour change.
- Settings: new app/.../ui/settings/sections/VehicleColorPicker.kt,
  AltimaColorPicker(), shown in NavigationSettings in place of the old
  five colour chips when the shape is the car (the old 64dp preview is
  skipped for the car, since the picker shows a 144dp preview; other
  shapes keep their old preview). Picker contents, all synced live and
  all writing the same preference: a 144dp top-down 3D Altima preview, a
  colour wheel (hue around, saturation centre-out, brightness from the
  V slider), HSV sliders, RGB sliders, and a hex field accepting
  #RRGGBB with or without the hash (invalid input shows an error and is
  never applied), plus Reset to default (#D93025). Strings added to the
  base strings.xml only, translatable=false, no apostrophes.
- Local verification (VM, debug): :app:assembleDebug -PappId=app.automap
  GRADLE_EXIT 0. Local APK build-logs/apk-altima/automap-altima.apk,
  143,750,068 bytes, sha256
  6779e8664b3e1f416a882473717502c157c70c7ba054351fef54c0dbaa0a98ba,
  aapt2 package app.automap, versionCode 1, versionName 0.3.0. Dex
  contains AltimaColorPicker, altimaTopDownPreviewBitmap,
  normalizeCarColor, puck_car_color; resources.arsc contains the new
  settings_puck_* strings. No unit test was added for the picker or the
  mesh: the picker is Compose-only with no Robolectric harness in this
  tree, and the mesh is Android-graphics code, so behaviour is verified
  by code inspection (loft ring vertex counts match: body 10/10, hood
  6/6, trunk 4/4, cabin 4/4) plus dex/resources presence, not by a unit
  test - stated, not claimed. No routing, traffic, places, search, voice
  publishing, backup, palette, Automatic default, or SunShading code was
  touched.
- Publishing (GitHub, not a chat attachment): patch stack regenerated
  (53 files, 2,184,740 bytes, git apply --check clean against a fresh
  archive of upstream 85eb69c) and pushed to DocBobert6/AutoMap main,
  commit a8894cbd6b9961fd8924e00d24067070069f68ee (README, PLAN,
  LICENSE, docs/icon.png, automap-patch-stack.diff, workflow unchanged).
  Workflow dispatch run 40441875999 completed success. Published asset
  verified by downloading it back through the authenticated API:
  https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-automap-debug
  asset AutoMap-0.3.0-debug.apk, 143,750,068 bytes (exact match to the
  GitHub listing and to the local build size), sha256
  ab205b6e1233aa024af924c48311aac772f8d34079f2c745ef485575bdf2fddb,
  aapt2 package app.automap, versionCode 1, versionName 0.3.0, dex and
  resources.arsc contain the Altima markers above.
- CERT CAVEAT: the CI APK is signed with the GitHub runner ephemeral
  debug keystore, cert SHA-256
  700059a40ed60999e72cbd11fb8c6c784659ce43a0b253a16ef8cebf6803a121
  (SHA-1 26cf5cb51b05ec0b4d65b8a05c9ca86be8be0015) - different from the
  previous CI cert and from every local debug cert. It will NOT install
  over any earlier build; uninstall first. CI debug builds cannot update
  over each other. No release keystore was created.
- Still owed, not claimed: device screenshots, drive tests, and
  AutoSpace turn publishing on Robert device; the picker and puck
  appearance at real map and settings sizes still need his eyes on a
  device, since this VM cannot render the Compose UI.

## AutoMap GitHub-native build 1 — car likeness, no brand naming, puck in browse for all shapes, white default colour (2026-10-27)
- First build under the GitHub-native model: work committed to DocBobert6/AutoMap main as a base_tree commit (parent = previous main HEAD), CI builds from the tree via .github/workflows/automap-debug-release.yml. No patch stack was generated or applied.
- 1. LIKENESS (Robert 2026-10-26 23:19, screenshots): the top-down car was a generic sedan, not a 2019 mid-size sedan. Puck3D.kt car() refined to true proportions: 2.20 long x 0.86 wide (4900 x 1850 mm, ratio 2.65), wheelbase 1.26 (2825 mm) with its centre 0.05 behind the body centre — front wheel +0.58, rear wheel -0.68, not the old symmetric +/-0.72 — true tyre size 0.32 across (not 0.40), long hood with crease ridges, trapezoidal roof narrowing toward the rear, sloping windshield and rear glass, boomerang headlights/taillights sweeping back along the flanks, wide front grille, short trunk with spoiler lip, mirrors at the A-pillar, door shut lines. The flat symbol path (VelaMapView.kt drawCarPuck) was refined to the same proportions: shell 48 x 130 px, front wheel centre y 53, rear wheel centre y 127. Still tintable (body = picked colour, darker edges, lighter roof) and still top-down 3D in both the map puck and the settings preview — no side view anywhere.
- 2. NO BRAND NAMING (Robert 2026-10-26 23:19): every user-visible "Altima" removed. settings_puck_color_hint now reads "The top-down 3D car below is the exact car shown on the map...". Internal function AltimaColorPicker() renamed VehicleColorPicker(), altimaTopDownPreviewBitmap() renamed carTopDownPreviewBitmap(). Verified: grep of app/src/main/java + app/src/main/res finds no "Altima" (any case) after the change.
- 3. PUCK IN BROWSE, ALL SHAPES (Robert 2026-10-26 23:55, urgent widening): root cause found in code, not in the car mesh — upstream Vela only ever drew the selected puck during navigation; in browse applyData always drew the blue dot + heading cone and ignored the shape preference entirely, so NO shape (car, arrow, UFO, pirate ship, rubber duck) ever appeared on the map outside a route. That is a design change for AutoMap, stated as such: VelaMapView.kt applyData now shows the selected shape's puck bitmap (NAV_PUCK_IMG, which already draws every shape) at the current location in browse too, replacing the blue dot, oriented to the GPS bearing when available and falling back to north-up (the ME source already carries bearing 0 when none is known). A stale fix keeps the dot, so a frozen puck is never drawn with confidence. Navigation puck behaviour is unchanged (symbol puck in nav, 3D mesh overlay in follow mode). The earlier Car-only browse wiring was generalised to all shapes before it shipped — no Car-only version was published.
- 4. DEFAULT CAR COLOUR WHITE (agreed 2026-10-26 23:29, revised puck flip): PuckStyle.DEFAULT_CAR_COLOR changed #D93025 -> #FFFFFF, first-install only. A saved puck_car_color still wins; legacy colour-name mapping (red/blue/white/green/yellow) is unchanged. Reset-to-default in the picker now resets to #FFFFFF, and invalid-colour fallbacks in the mesh, flat path, and picker now fall back to white for consistency. PuckStyle.style BLUE->WHITE was NOT flipped — the user already has that blue/white choice and it stays as-is.
- Untouched, as ordered: no routing, traffic, places, search, voice publishing, backup, palette, Automatic default, or SunShading code was changed.
- Verification (see CI section below for the published-asset numbers): local :app:compileDebugKotlin green in this VM (full local APK build was not possible here — see note); dex/resources markers checked on the published asset. No unit test was added for the picker/mesh/browse wiring: Compose + Android-graphics code with no Robolectric harness in this tree, so behaviour is verified by code inspection + dex/resources presence, stated not claimed. Device verification (real look at map/settings sizes for every shape, drive tests, AutoSpace turn publishing) is owed on Robert's device, not claimed here.
- LOCAL BUILD NOTE: a full local :app:assembleDebug in a fresh checkout failed in this VM for environment reasons, not code reasons — the shared Gradle home's journal was locked by another daemon (PID 7328), an isolated Gradle home had to copy the 421 MB module cache, and a 4 GB daemon was then killed under VM memory pressure (8 GB RAM, another daemon alive). Kotlin compilation was verified locally with a 2 GB heap instead; the full APK was verified from CI, which is the GitHub-native model's authoritative build.
- Publishing (GitHub-native, not a chat attachment): work commit dd2ad2757e05a80716a5a0c45f82ddc70cb618bb on DocBobert6/AutoMap main (parent da9365c, base_tree commit, 6 files). Workflow dispatch run 40447830504 completed success. Published asset verified by downloading it back through the authenticated API (redirect to the signed release-asset URL followed, unauthenticated): https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-automap-debug asset AutoMap-0.3.0-debug.apk, 143,750,064 bytes (exact match to the GitHub listing), sha256 c702e9e12daf77a438a10f1c33667b6671ff9027bebed46bd5f799140b0c490d, aapt2 package app.automap, versionCode 1, versionName 0.3.0. Dex contains VehicleColorPicker, carTopDownPreviewBitmap, normalizeCarColor, puck_car_color, showBrowsePuck, vela-nav-puck; resources.arsc contains the settings_puck_* strings, the new "top-down 3D car below" hint, and no "Altima" (any case).
- CERT CAVEAT: the CI APK is signed with the GitHub runner ephemeral debug keystore, cert SHA-256 56daad6097830e5e098ff821a56f84f054cb4f36dbb5e87cd9d24b408cdddfc1 (SHA-1 229cb3b0465df37cbf5ef5d003d494af22ee9336) - different from every previous CI and local debug cert. It will NOT install over any earlier build; uninstall first. CI debug builds cannot update over each other. No release keystore was created.

## AutoMap GitHub-native build 2 — in-app updater, ported from AutoSpace (2026-10-27)
- Second build under the GitHub-native model: work committed to DocBobert6/AutoMap main as a base_tree commit (parent = previous main HEAD), CI builds from the tree via .github/workflows/automap-debug-release.yml. No patch stack was generated or applied.
- DESIGN DECIDED BY ROBERT 2026-10-26 23:50: neither a token entered in settings nor a browser-page opener. Port the AutoSpace UpdateChecker pattern, adapted to DocBobert6/AutoMap.
- WHAT WAS PORTED: new app.vela.update.AutoMapUpdater (object, StateFlow state: Idle/Checking/UpToDate/Available/Downloading/Ready/Failed): GitHub releases API check (releases/latest for Stable, releases?per_page=20 for Beta/Alpha), Stable/Beta/Alpha channels with Alpha as the default for alpha builds and a saved channel always winning over the default, automatic checks at most once a day on app opened (MainActivity onResume hook), a dedicated Updates section in Settings > About (auto-check toggle, channel selector, Check for updates, Download, Install, progress, status), downloaded APK verified against the size and SHA-256 GitHub lists for the asset, then handed to Android's own installer through the ${package}.fileprovider authority, with the unknown-sources permission flow (open settings, resume on return) when needed.
- VELA SELFUPDATER, CHECKED AND NOT REPOINTED: Vela already ships app.vela.update.SelfUpdater with its own About channel selector (stable/nightly/canary) and map card, but it points at upstream PimpinPumpkin/Vela through VelaLinks and uses a v0.<minor>.<run> versionCode scheme AutoMap does not share. Repointing it would also move VelaLinks, which feeds map/place/road-feature downloads AutoMap still takes from upstream, so it was left untouched. AutoMapUpdater has its own API base constant and its own prefs (automap_updates / automap_update_channel / automap_update_auto), so the two updaters do not fight over prefs or channels. The Vela section in About remains as upstream left it; the AutoMap section is separate and labelled as such.
- DIVERGENCES FROM AUTOSPACE, STATED: (1) APK pick does NOT exclude "debug" names — AutoMap's published alphas are debug-signed AutoMap-*.apk files, so excluding them would find nothing. (2) No car-session / drive-ended / notification hooks — Vela has no AutoSpace car-session equivalent, so those hooks were not forced in. (3) Debug gate kept: automatic checks never run on debug builds (AutoSpace rule), so automatic checks are dormant on the current debug-signed alphas and activate on release-signed builds; the manual Check for updates button is NOT gated and works on any build. (4) A 404 is NOT treated as "no stable release yet" — see private-repo verification below.
- VERSION COMPARISON, FIXED NOT COPIED: AutoSpace's isNewer compares only the numbers before the dash and then the stage, so it cannot tell 0.3.0-alpha.2 from 0.3.0-alpha.1 at all (found by a failing unit test, fixed in this build). AutoMapUpdater.isNewer compares the numeric base, then prerelease tokens in order (alpha < beta < stable, numeric suffixes as numbers). One deliberate exception: a prerelease of 0.3.0 beats plain 0.3.0, because 0.3.0 was never a stable release — it was the versionName of every pre-alpha debug build (versionCode 1), and the alpha scheme is what those builds migrate to. For any other base, a plain release still beats its own prereleases.
- ALPHA VERSIONING, STARTED HERE: versionName 0.3.0-alpha.1, versionCode 2 (was 0.3.0 / 1 on every build so far, all on one reused tag, which made any updater unable to tell a new build from the installed one). The workflow now tags by versionName (v0.3.0-alpha.1, then .alpha.2, and so on) and creates a versioned prerelease per alpha instead of overwriting v0.3.0-automap-debug; that old release is left untouched. Future alpha builds MUST bump versionName and versionCode together, or the updater cannot tell the new build from the installed one.
- PRIVATE-REPO VERIFICATION (the task's verification item, done before shipping): unauthenticated curl to api.github.com/repos/DocBobert6/AutoMap/releases/latest AND .../releases?per_page=20 both return HTTP 404 "Not Found", while the authenticated API returns the v0.3.0-automap-debug prerelease with its asset and sha256 digest. Conclusion, stated plainly: the updater code works as designed, but an unauthenticated in-app check against the private repo WILL fail with 404, and the updater reports that as a Failed state naming the private repo instead of falsely claiming the app is up to date. What would unblock it: a public release channel, or a different hosting for alpha assets. No token or browser fallback was invented, per Robert's decision. Shipping the updater with this limitation clearly reported is per the task brief; claiming it works end-to-end on a private repo would be false.
- INSTALL CAVEAT (in the Settings copy as well as here): no release keystore was created and the repo was NOT made public, both still-open decisions. CI debug builds are signed with ephemeral runner keystores and cannot update over each other, so until release signing lands, an in-app update may still need the old build uninstalled first. Seamless updates are not promised.
- Untouched, as ordered: no routing, traffic, places, search, voice publishing, backup, palettes, Automatic default, SunShading, or puck behaviour was changed.
- Verification: local :app:assembleDebug -PappId=app.automap green (version 0.3.0-alpha.1 / code 2, package app.automap); dex contains AutoMapUpdater and its Channel/Release/State types; resources.arsc contains all 17 settings_automap_* strings. Unit test app.vela.update.AutoMapUpdaterTest: 9 tests, 0 failures (next alpha beats previous alpha, legacy 0.3.0 migration, plain beats its own prerelease otherwise, beta beats alpha, stable beats beta, reused debug tag not newer than alpha, same version not newer, older not newer), re-confirmed on the current tree by an independent --rerun-tasks run (BUILD SUCCESSFUL, 52 tasks executed, fresh XML 9 tests / 0 failures). The Compose Updates UI itself has no unit test (no Robolectric harness for Compose in this tree for UI state), so its appearance on a device is owed, not claimed. Device verification (real update check/download/install flow on Robert's device) is owed, not claimed here.
- Publishing (GitHub-native, not a chat attachment): work commit 4fbc089b1c9580435d2c7ed55b89a548a0af7b2b on DocBobert6/AutoMap main (parent d30e60ec, base_tree commit, 9 files; all 9 pushed blobs re-verified byte-identical to the worktree by git blob SHA). Workflow dispatch run 40452287954 (job 122017556699, build) completed success. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.1, asset AutoMap-0.3.0-alpha.1.apk, 143,774,536 bytes (exact match to the GitHub listing), sha256 94aa49d90269d319bad7eaa693339c5d9c21d8b12129bd3c45533eed2f0375d8 (exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log at 04:59:49Z, so the published artifact is definitively this run's build). Verified by downloading it back through the authenticated API (asset API 302 to the signed URL followed explicitly; the gh_api download-asset helper refuses redirects, so the follow was done with the same redirect-capture pattern it uses for logs/artifacts). aapt2 package app.automap, versionCode 2, versionName 0.3.0-alpha.1. Dex contains AutoMapUpdater (classes17/18/23.dex); resources.arsc contains all 17 settings_automap_* strings. The old v0.3.0-automap-debug release is untouched. Note: the release object's created_at (04:46:44Z) predates the run (created 04:54:16Z); published_at (04:59:50Z) and asset updated_at (04:59:57Z) fall inside the run, and the job-log hash match above settles provenance.
- CERT CAVEAT: the CI APK is signed with the GitHub runner ephemeral debug keystore, cert DN "Android Debug", SHA-256 14ad39d823b854eb8e5e5cb3cf2df5d2a5b00ad66093f3b320f932cfbc03d6e4 (SHA-1 37993c6a23c3125fdeb6b20061e3998d1c52bc23) — different from every previous CI and local debug cert. It will NOT install over any earlier build; uninstall first. CI debug builds cannot update over each other. No release keystore was created and the repo was NOT made public, both still-open decisions.

## AutoMap GitHub-native build 3 — Bluetooth-gated browse puck, curved sedan likeness, larger puck (alpha.2, 2026-10-27)
- Third build under the GitHub-native model: work committed to DocBobert6/AutoMap main as a base_tree commit (parent = previous main HEAD), CI builds from the tree via .github/workflows/automap-debug-release.yml. No patch stack was generated or applied. Version bumped together, per the alpha.1 rule: versionName 0.3.0-alpha.2, versionCode 3 (was 0.3.0-alpha.1 / 2), so the in-app updater can tell this build from the installed one.
- 1. BLUETOOTH GATING, FINAL MODEL (Robert 2026-10-27 00:33, superseding the 00:31-00:32 readings): DEFAULT — the browse location shows the blue dot, and the car appears only when a Bluetooth device is connected. SETTING TOGGLE "Car always on" — when toggled on, the car always shows regardless of Bluetooth, overriding the default gate. Scope is the browse/location display only: navigation is NOT gated (showPuck in applyData ignores Bluetooth entirely, so a route always shows its puck). Bluetooth means ANY connected device via the adapter, not car-specific profile matching — there is no reliable car-only signal on stock Android, and none was guessed. When gated off, browse falls back to the blue dot + heading cone, exactly as before the puck-in-browse change.
- IMPLEMENTATION OF THE GATE: new app.vela.ui.CarAlwaysOn in NavChrome.kt (mutableStateOf(false) = gated by default; init() reads pref car_always_on, default false; set() persists it; isBluetoothConnected(): false if Android 12+ and BLUETOOTH_CONNECT not granted, false if no BluetoothManager/adapter or the adapter is not enabled, otherwise true if any of HEADSET, A2DP, or GATT profile states is STATE_CONNECTED; every failure caught via runCatching, so no permission / no adapter / SecurityException reads as "not connected" — the blue dot stays, never a crash). VelaApp.kt calls CarAlwaysOn.init(this) after PuckStyle.init(this). AndroidManifest.xml gains BLUETOOTH (maxSdkVersion 30) and BLUETOOTH_CONNECT, with a comment that without the runtime permission the check reads "not connected" and the blue dot stays, never a crash. VelaMapView.kt: a bluetoothTick state, bumped by a BroadcastReceiver for ACTION_CONNECTION_STATE_CHANGED, ACTION_STATE_CHANGED, ACTION_ACL_CONNECTED, and ACTION_ACL_DISCONNECTED (RECEIVER_NOT_EXPORTED on SDK 33+, old overload below), so the puck/dot flip without waiting for the next location fix; applyData takes bluetoothTick as a parameter, showBrowsePuck = !navMode && me != null && !meStale && bluetoothOk, and meLayerKey gains bit 32 for tick parity so a connect/disconnect alone re-applies the layer. Settings: NavigationSettings.kt gains a "Car always on" ToggleRow after the vehicle colour picker (hint explains the default, the Bluetooth condition, and that navigation is not affected), plus, on Android 12+ only when BLUETOOTH_CONNECT is not granted, an "Allow Bluetooth access" point-of-use permission button following the SearchSettings contacts pattern (a denial leaves the gate reading "not connected", i.e. the blue dot stays). New strings settings_car_always_on, settings_car_always_on_hint, settings_car_bluetooth_permission (translatable=false).
- 2. LIKENESS REFINEMENT (Robert 2026-10-27 00:44, side-profile and front-3/4 reference images): the top-down car was still too blocky, like a square body. Puck3D.kt car() mesh: the old 14-point lower-body outline (pointed nose, wide shoulders, square tail) is replaced with a 29-point curved outline — short segments around the nose and tail curves, because a loft joins points with straight segments, so point density IS the curvature — widest at the rear haunches (0.43), slightly pinched at the doors, rounded at both ends. Dimensions unchanged: 2.20 long, 0.86 wide, wheelbase 1.26 (front wheel +0.58, rear -0.68, tyre 0.32). Hood ring front edge rounded to match the nose; trunk ring rear edge rounded to match the tail; cabin top ring changed to rear width 0.28 (from 0.30) and front width 0.34 (from 0.33), so the roof tapers rearward (fastback roofline) instead of sitting as a box. Flat symbol path drawCarPuck() in VelaMapView.kt: the roundRect shell is replaced with a curved Path (rounded nose, flanks bulging at fenders/haunches, rounded tail) in the same bounding box (y 22..152, half-width 24), so every internal detail still lands inside it. Still tintable (body = picked colour, darker edges, lighter roof). KEEP, AS ROBERT ASKED: the headlights stay bright (#FFF6C8) and taillights #C62828/#B3261E in both paths — on a dark night map they read as illuminated; the mesh does not know the theme, so this is not a day/night switch, it is the lights contrasting against the dark map.
- 3. PUCK SIZE (Robert 2026-10-27 00:45, nav screenshot: the car read smaller than the road, where Google's reads wider than it): the puck grows by 1.4x, consistently, in navigation and browse. Symbol path navPuckBitmap() base 202 -> 283, and the follow-overlay mesh path literal 202 -> 283 to match; the Settings "Arrow size" setting (Normal 1.0, Large 1.25, Extra large 1.5) still works — it now scales from the larger base, so the default (Normal) is already 1.4x the old size and Large/XL grow further. Car screen CarMapRenderer.kt drawPuck(): minOf(width, height) / 8f -> / 6f, cap 220 -> 290, times the Settings puck size as before — this is the car screen's only size change, because the car scales the phone bitmap to its own px, so a second multiplier there would double-scale. The arrow overlay path uses puckImg.width (now 283), so it grows automatically. Night lights kept.
- Untouched, as ordered: no routing, traffic, places, search, voice publishing, backup, palettes, Automatic default, SunShading, or updater code was changed (AutoMapUpdater itself untouched; the only shared file is the version default in app/build.gradle.kts, bumped per the alpha rule).
- Verification: local :app:compileDebugKotlin green in this VM (BUILD SUCCESSFUL; one real failure on the way — an unresolved rememberLauncherForActivityResult import in NavigationSettings.kt — fixed with the androidx.activity.compose import and recompiled green; remaining warnings are pre-existing, none in this build's files). No unit test was added for the gate/mesh/size wiring: Compose + Android-graphics code with no Robolectric harness in this tree, so behaviour is verified by code inspection + dex/resources presence, stated not claimed. A full local :app:assembleDebug was not run; the full APK was verified from CI, which is the GitHub-native model's authoritative build. Device verification is owed on Robert's device, not claimed here: real look at map/settings sizes, Bluetooth connected vs not, Car always on on/off, night lights, puck width vs road, for every shape in browse and navigation.
- Publishing (GitHub-native, not a chat attachment): work commit 1b7a458119929c1a4907d90ee03478290579a21e on DocBobert6/AutoMap main (parent e1f5afa3, base_tree commit, 9 files; all 9 pushed blobs re-verified byte-identical to the workdir by git blob SHA). Workflow dispatch run 40455244802 (job 122026933165, build) completed success. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.2, asset AutoMap-0.3.0-alpha.2.apk, 143,792,524 bytes (exact match to the GitHub listing), sha256 0531875425d5f526e2f36e43a625f328bfe562f9a80918aad11797a85f0a4b9c (exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log at 05:30:40Z, so the published artifact is definitively this run's build). Verified by downloading it back through the authenticated API (asset API 302 to the signed URL followed explicitly). aapt2 package app.automap, versionCode 3, versionName 0.3.0-alpha.2. Dex contains CarAlwaysOn, isBluetoothConnected, bluetoothTick, VehicleColorPicker, carTopDownPreviewBitmap, normalizeCarColor, puck_car_color, showBrowsePuck, vela-nav-puck, sun_prompt_done, AutoMapUpdater; resources.arsc contains settings_car_always_on, settings_car_always_on_hint, settings_car_bluetooth_permission, and no "Altima" (any case). The old v0.3.0-automap-debug and v0.3.0-alpha.1 releases are untouched.
- CERT CAVEAT: the CI APK is signed with the GitHub runner ephemeral debug keystore, cert DN "Android Debug", SHA-256 fe1db1f21306f4e5a8250c28c2dcf49a29e25c5208e852cbd17cee6b539725ca (SHA-1 1cf1ade21283efd64b5a80c68a07412f75c80e97) — different from every previous CI and local debug cert. It will NOT install over any earlier build; uninstall first. CI debug builds cannot update over each other. No release keystore was created and the repo was NOT made public, both still-open decisions.
## AutoMap GitHub-native build 4 — settings-default flips: traffic lights, zoom to next turn, road numbers first (alpha.3, 2026-10-27)
- Fourth build under the GitHub-native model: work committed to DocBobert6/AutoMap main as a base_tree commit (parent = previous main HEAD 4ad2f479), CI builds from the tree via .github/workflows/automap-debug-release.yml. No patch stack was generated or applied. Version bumped together, per the alpha.1 rule: versionName 0.3.0-alpha.3, versionCode 4 (was 0.3.0-alpha.2 / 3), so the in-app updater can tell this build from the installed one.
- THE THREE FLIPS (agreed by Robert 2026-10-26 23:29, after research confirmed the settings audit; all FIRST-INSTALL ONLY — a saved choice always wins over the new default, because every read below is SharedPreferences getBoolean(key, newDefault), which returns the saved value when one exists):
  1. Traffic lights in navigation OFF -> ON (pref `nav_traffic_lights`, Settings > Navigation): NavController.kt lightCues read changed getBoolean(..., false) -> (..., true), and the NavigationSettings toggle's initial state changed the same way, so the toggle, the guidance enrichment, and the saved value agree on a fresh install. The base strings hint was corrected from "off by default" to "on by default" so the UI copy does not contradict the new default.
  2. Zoom to next turn OFF -> ON (ZoomToTurn.on, Settings > Navigation): ZoomToTurn.kt initial state mutableStateOf(false) -> (true) AND init() getBoolean(KEY, false) -> (KEY, true), so both the pre-init state and the loaded state are on for a fresh install; a saved false still loads false.
  3. Road numbers first OFF -> ON (RoadNumbers.on, Settings > Navigation): RoadNumbers.kt initial state mutableStateOf(false) -> (true) AND init() getBoolean(KEY, false) -> (KEY, true), with the mirrored :core flag following init as before; a saved false still loads false.
- DEFAULT-COVERAGE CHECK: grep of the whole app tree found no other reader of these three keys with a false default — the traffic-light key is read in exactly the two places changed, and zoom_to_turn / road_numbers are read only through their holder objects. No migration or first-run reset touches these keys, so nothing reverts a saved choice.
- EXPLICITLY NOT DONE, as ordered: PuckStyle.style BLUE -> WHITE was NOT flipped (the user already has that blue/white choice); the default car colour was NOT changed (already white from the Altima fix); Automatic colour scheme (FOLLOW), SunShading (OFF + first-boot ask), terrain hillshade (OFF), and trees (OUT entirely) were NOT touched; live traffic, 2D/3D global default, house numbers, map names, category chips, car speed badge, and speeding alert were NOT flipped (research did not establish those). No routing, traffic, places, search, voice publishing, backup, palettes, puck behaviour, Bluetooth gating, or updater code was changed; the only shared file is the version default in app/build.gradle.kts, bumped per the alpha rule. Code comments in VelaApp.kt and NavController.kt were updated off -> on for consistency; no behaviour was rewritten.
- Verification: local :app:compileDebugKotlin -PappId=app.automap green in this VM (BUILD SUCCESSFUL in 6m 46s; warnings are pre-existing, none in this build's files). No unit test was added for these defaults: the holders are Compose/Android state objects with no Robolectric harness in this tree, so behaviour is verified by code inspection (both definition sites per setting, plus the coverage grep) and by the published-asset checks below, stated not claimed. Fresh-install defaults were verified from the pushed source on GitHub main (all three files read back contain the true defaults); a fresh install actually showing the toggles on, and an existing install keeping a saved false, are owed on Robert's device, not claimed here.
- Publishing (GitHub-native, not a chat attachment): work commit 7e2e8f9f7ce86ff127af59f54d98dd8f90059b93 on DocBobert6/AutoMap main (parent 4ad2f479, base_tree commit, 7 files; all 7 pushed blobs re-verified byte-identical to the worktree by git blob SHA). Workflow dispatch run 40457327271 (job 122033587481, build) completed success. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.3, asset AutoMap-0.3.0-alpha.3.apk, 143,792,520 bytes (exact match to the GitHub listing), sha256 d8be6036b21dca44eb13b78aeb306ccdc58f646195d95a8f2bd22abadbd7818d (exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log, so the published artifact is definitively this run's build). Verified by downloading it back through the authenticated API (asset API 302 to the signed URL followed explicitly). aapt2 package app.automap, versionCode 4, versionName 0.3.0-alpha.3, label AutoMap. The old v0.3.0-automap-debug, v0.3.0-alpha.1, and v0.3.0-alpha.2 releases are untouched.
- CERT CAVEAT: the CI APK is signed with the GitHub runner ephemeral debug keystore, cert DN "Android Debug", SHA-256 527031d4d0290669a1be2a14af39c90d2fb347ea6e467fdefc17a8c8614361ae (SHA-1 d28b9f4374b65908384751af754be14611c2dd58) — different from every previous CI and local debug cert. It will NOT install over any earlier build; uninstall first. CI debug builds cannot update over each other. No release keystore was created and the repo was NOT made public, both still-open decisions.
