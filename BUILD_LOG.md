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

## AutoMap GitHub-native build 5 — puck wheels tucked under the body (alpha.4, 2026-10-27)
- Fifth build under the GitHub-native model: work committed to DocBobert6/AutoMap main as a base_tree commit (parent = previous main HEAD 32a9ba6d), CI builds from the tree via .github/workflows/automap-debug-release.yml. No patch stack was generated or applied. Version bumped together, per the alpha.1 rule: versionName 0.3.0-alpha.4, versionCode 5 (was 0.3.0-alpha.3 / 4), so the in-app updater can tell this build from the installed one.
- TASK (Robert 2026-10-27 10:15, screenshot): "Car is great. Only problem, the wheels stick out instead of being under car." In the screenshot the top-down car in browse/day showed wheels as black rectangles protruding sideways beyond the body width, front and rear.
- ROOT CAUSE, BOTH PATHS: the wheels were drawn entirely outside the body. 3D mesh (Puck3D.kt car()): tyre boxes ran x -0.49..-0.37 and 0.37..0.49 against a body flank at |x| = 0.43 — 0.06 of every tyre outside the body. Flat symbol path (VelaMapView.kt drawCarPuck, used for the browse/nav symbol puck and therefore the screenshot): wheels ran cx±24..34 against a body half-width of 24 — a full 10 px outside the body, with body-coloured ovals patching their middles.
- FIX, 3D MESH: tyre boxes moved inward to -0.45..-0.34 and 0.34..0.45 (same 0.11 width, same y ±0.16, same z 0..0.32, same wheelbase +0.58/-0.68). The body flank is 0.43, so only 0.02 of tyre peeks out; the rest (0.34..0.43) sits under the body top cap, which is higher (z 0.42 vs tyre top 0.32) and drawn over it in painter's order. Silver hubs re-centred on the tyre (±0.395: -0.425..-0.365 and 0.365..0.425, z 0.32..0.35) so they stay inside the flank instead of protruding with the tyre.
- FIX, FLAT SYMBOL PATH: wheels moved inward to cx±19..28 (same 18 px height, same centres y 53 front / 127 rear). The body path drawn next covers their inner part (19..24), leaving only the outer 4 px tire edge peeking out, like a real top-down car. The old body-coloured ovals over the wheel middles were removed with the old positions — they are no longer needed once the body itself covers the inner tyre.
- UNCHANGED, AS ORDERED: the 29-point curved silhouette, the 1.4x puck size, the body size/outline (2.20 x 0.86, wheelbase 1.26), tintable body colour (default white), night lights (bright #FFF6C8 headlights, #C62828/#B3261E taillights in both paths), Bluetooth gating with the "Car always on" toggle, the settings colour picker, the updater, and the three settings flips. No "Altima" in any user-visible string (verified: no case-insensitive hit in the changed files or in the published resources.arsc).
- Verification: local :app:compileDebugKotlin -PappId=app.automap green in this VM (BUILD SUCCESSFUL in 7m 21s; warnings pre-existing, none in this build's files). No unit test was added for the wheel tuck: Compose/Android-graphics code with no Robolectric harness in this tree, so behaviour is verified by code inspection (positions/sizes/layer order above) + dex/resources presence, stated not claimed. A full local :app:assembleDebug was not run; the full APK was verified from CI, which is the GitHub-native model's authoritative build. Device verification is owed on Robert's device, not claimed here: real look at wheels tucked under the body at map and settings sizes, in browse and navigation, day and night.
- Publishing (GitHub-native, not a chat attachment): work commit 25dafcfba6df85e29bbe05630d1cfaf72578068f on DocBobert6/AutoMap main (parent 32a9ba6d, base_tree commit, 3 files: app/build.gradle.kts, app/src/main/java/app/vela/ui/map/Puck3D.kt, app/src/main/java/app/vela/ui/map/VelaMapView.kt). Workflow dispatch run 40521102381 (job 122248116746, build) completed success. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.4, asset AutoMap-0.3.0-alpha.4.apk, 143,792,524 bytes (exact match to the GitHub listing), sha256 1f4a85c7fc02d8aeca05a1f45654ea46ed9f2ba1e72a23b6ab5985db9321438c (exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log at 14:30:53Z, so the published artifact is definitively this run's build). Verified by downloading it back through the authenticated API (asset API redirect to the signed URL followed explicitly). aapt2 package app.automap, versionCode 5, versionName 0.3.0-alpha.4, label AutoMap. Dex contains carTopDownPreviewBitmap, VehicleColorPicker, puck_car_color, showBrowsePuck, vela-nav-puck, CarAlwaysOn, AutoMapUpdater; resources.arsc contains settings_puck_* and settings_car_always_on* strings and no "Altima" (any case). The old v0.3.0-automap-debug, v0.3.0-alpha.1, v0.3.0-alpha.2, and v0.3.0-alpha.3 releases are untouched.
- CERT CAVEAT: the CI APK is signed with the GitHub runner ephemeral debug keystore, cert DN "Android Debug", SHA-256 b31e19a4d67efaf94763b9a26cdf10b3d41c30e0f1d7098aaee8cca9af68d39b (SHA-1 b2ef364d1d0a80be4f6da7a3e4791807d9778b91) — different from every previous CI and local debug cert. It will NOT install over any earlier build; uninstall first. CI debug builds cannot update over each other. No release keystore was created and the repo was NOT made public, both still-open decisions.

## 0.3.0-alpha.5 — flat/stretched puck fix + upstream updater banner disabled (2026-10-27)

Work commit: f26197afcf8149d489cd63d109b1a4705ee5302c (parent 21980069, base-tree commit, 5 files).
CI: workflow run 40528446955 (job 122274331704, build), completed success, via .github/workflows/automap-debug-release.yml (manual dispatch).
Release: https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.5 (prerelease), asset https://github.com/DocBobert6/AutoMap/releases/download/v0.3.0-alpha.5/AutoMap-0.3.0-alpha.5.apk
Verified by downloading the asset back: size 143,776,140 bytes (exact match to the GitHub listing), sha256 9ad371a9ab5917d56e9129913ca1262a894d49627f86d0347fa713a2c00e604b — exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log, so the published artifact is definitively this run's build.
aapt2: package app.automap, versionCode 6, versionName 0.3.0-alpha.5, label AutoMap. Dex contains CarAlwaysOn, isBluetoothConnected, VehicleColorPicker, carTopDownPreviewBitmap, puck_car_color, showBrowsePuck, vela-nav-puck, AutoMapUpdater, sun_prompt_done, dismissSunShadingPrompt. resources.arsc contains settings_car_always_on*, settings_automap_* and root_sun_* strings, and no "Altima" in any case.
Install caveat: CI APK signed with the GitHub runner's ephemeral debug keystore, cert DN "Android Debug", SHA-256 190be7511f2c24619e0daa6c7fa6b525bb285b63a2120781c0e9bdcc1a42d4b6 (SHA-1 00bf6dd91d26d0afa6d695f9afd0178f38d59aa4) — different from every previous CI and local debug cert. It will NOT install over any earlier build; uninstall first. CI debug builds cannot update over each other. No release keystore was created, repo was not made public.

### Task 1 — car flat/stretched: root cause and fix
Root cause (Robert's 2026-10-27 10:53 screenshot, browse free-drive follow at 55-degree tilt): the follow-overlay mesh projected the ground plane with cos(tilt) — py = cy - (y * cos(tilt) + z * sin(tilt)) * s — so at 55 degrees the car's length was foreshortened to ~57% while its width stayed full, and, combined with heading rotation, the car read as flat and diagonally stretched. This double-counts the tilt: the map camera already renders the ground tilted, and the overlay is screen space, not map space — the same class of error as the flat fallback's screen-space scaleY = cos(tilt). The symbol path was already screen-space and square, and the car screen path was already square + rotate-only, so neither of those was the bug.
Fix, per path (curved silhouette, tucked wheels, 1.4x size, tintable colour default white, night lights, Bluetooth gating, colour picker all unchanged):
- 3D follow overlay, mesh (Puck3D.kt PuckMesh.draw): ground footprint (y) is now unscaled at any tilt — py = cy - (y + z * sin(tilt)) * s — so the 2.20 x 0.86 ratio is preserved exactly at any rotation, tilt, and zoom; height (z) still projects by sin(tilt), so side faces keep their 3D depth, and the ground shadow is drawn unscaled to match the footprint. At tilt 0 the output is identical to before; only tilted views change.
- 3D follow overlay, flat fallback (VelaMapView.kt, arrow shape when no mesh): scaleY squash removed — showPuckOverlay now sets puckOverlaySquash = 1f, and the overlay Image sets scaleY = 1f regardless of the stored field.
- Flat symbol path (VelaMapView.kt ME_ARROW_LAYER): iconPitchAlignment locked explicitly to VIEWPORT, so the symbol is a screen-space icon and is never foreshortened by map tilt. Bitmap itself was already square (283 * scale) with equal x/y canvas scaling.
- Settings previews: they draw through the same mesh (puck3DPreviewBitmap at tilt 50 degrees, carTopDownPreviewBitmap at tilt 32 degrees), so they are fixed by the mesh change with no separate edit.
- Car screen (CarMapRenderer.kt drawPuck): no change needed — verified again: square createScaledBitmap(px, px) + rotation only, no non-uniform scale. It was not the bug.

### Task 2 — upstream updater banner disabled
The banner "AutoMap 0.5.2621 is available" was upstream Vela's SelfUpdater (PimpinPumpkin/Vela via VelaLinks), which would have replaced AutoMap with upstream Vela. Disabled entirely for AutoMap, by the cleanest route: MapViewModel.maybeCheckForUpdate() now returns before any check (no daily launch check, no banner), and checkForUpdateNow() now reports "none found" without contacting upstream. The Settings upstream section in AboutSettings.kt is compiled out (if (false) block, kept in the diff for review). Verified by code inspection that state.updateInfo — the only driver of the UpdateCard banner in MapScreen and the inline offer in AboutSettings — is only ever set to a non-null value inside those two disabled functions (the other two assignments only clear it), so no upstream prompt can appear. VelaLinks was NOT moved, because it also feeds upstream map/place/road-feature downloads AutoMap still takes. AutoMapUpdater (Settings > About > AutoMap updates, Alpha channel) is untouched and is the only update path.
Caveat, stated plainly: the upstream SelfUpdater class and its strings remain in the binary as dead code (the two check functions return before reaching the old check calls, and the Settings section is compiled out). Dead code, not a live prompt — a future cleanup may delete SelfUpdater outright, but that was not needed to kill the banner and was left out of this build to keep the diff minimal.
Local compile: :app:compileDebugKotlin -PappId=app.automap green in the VM (BUILD SUCCESSFUL in 6m 12s; warnings pre-existing, none in this build's files). Full APK verified from CI, which is the GitHub-native model's authoritative build. No unit test was added for the mesh projection or the updater gating: Compose/Android-graphics code with no Robolectric harness in this tree, so behaviour is verified by code inspection (projection formula, layer properties, updateInfo assignment sites) + dex/resources presence, not claimed as test coverage.
Not changed, as ordered: routing, traffic, places, search, voice publishing, backup, palettes, Automatic default, SunShading, Bluetooth gating, colour picker, updater (AutoMapUpdater), or the three settings flips. No "Altima" in any user-visible string.
Still owed, not claimed: device verification on Robert's device — real look: car proportions correct at map/settings sizes, in browse and navigation, day and night, at tilt; and no upstream update banner. Still open, unchanged: release keystore, repo visibility (stays private), region-bake hosting, private-repo updater limitation (unauthenticated in-app check returns 404).

## AutoMap GitHub-native build 7 — location freshness fix: never route from a stale parked origin (alpha.6, 2026-10-28)
- Seventh build under the GitHub-native model: work committed to DocBobert6/AutoMap main as a base_tree commit (parent = previous main HEAD 4ddb0622, 10 files; all 10 pushed blobs re-verified byte-identical to the worktree by git blob SHA). CI builds from the tree via .github/workflows/automap-debug-release.yml. No patch stack was generated or applied. Version bumped together, per the alpha rule: versionName 0.3.0-alpha.6, versionCode 7 (was 0.3.0-alpha.5 / 6).
- TASK (Robert 2026-10-27, confirmed: car-screen entry, puck itself looked parked mid-drive, phone locked/off during the drive, SimLocation off): occasionally AutoMap did not pick up his location — driving with no route, then entering a destination mid-drive, it routed from where he was parked last.
- ROOT CAUSES (from the read-only audit at ~/workspace/location-staleness-audit.md): (a) the phone feed starves while backgrounded/locked (no background-location grant, phone ViewModel stopped during projection, NavigationService never feeds location during a route-less drive, startLocation() was a no-op if locationJob was alive but starved, seed only once at init from lastKnown); (b) the car RoutePreview read LocationProvider.lastKnown() — polling it 15 s never made it fresher, its timestamp was discarded, and the live car puck / NavSession position was never consulted; (c) the phone route() used myLocation with no age check — myLocationStale only greys the dot.
- FIX, LocationProvider (core/.../location/LocationProvider.kt): new lastKnownWithTime() returns a KnownFix (point + wall-clock age) instead of discarding the timestamp; the prefs cache now stores its time (KEY_TIME) so the fallback has an age too; lastKnown() delegates to it.
- FIX, phone (MapViewModel.kt, MapScreen.kt, MainActivity.kt): state gains myLocationFixMs (elapsedRealtime of the last accepted fix; 0 = none/seed-only = stale), stamped on every accepted fix, on the sim branch, and on the init seed from lastKnownWithTime's age. route() freshness-gates a live origin: stale (>30 s, FRESH_LOCATION_MS) or missing → hold, show "Locating… waiting for a fresh GPS fix before routing.", restart a starved collector, re-route from the first fresh fix; on a 30 s timeout (LOCATE_WAIT_MS) warn ("Location is still stale — no route started…") and drop the request — never silently route from the parked point. Hand-picked origins bypass the gate; reversed routes (live point is the dest) need no gate. refreshLocationIfStarved(), called from MainActivity.onResume and the map launch effect, restarts a collector alive but with no fix for >12 s (STALE_LOCATION_MS) instead of the old locationJob != null no-op, and adopts a fresher snapshot (e.g. one the car session cached) when it exists.
- FIX, car (CarMapRenderer.kt, RoutePreviewCarScreen.kt): renderer gains livePuckWithAgeMs() (live puck + fix age). RoutePreview fetchRoutes() prefers the live puck, falls back to a snapshot only if ≤30 s old, waits up to 30 s for a fresh fix, and otherwise shows a waiting-for-GPS message with no routes instead of routing from the parked point. startNav() re-verifies at Go time: a fresh live puck >50 m from the preview origin re-fetches the route from the live puck, and a never-verified origin with no fresh live fix refuses to start (toast). AlongRoute/Nearby car screens use a fresh-checked (≤30 s) snapshot or null, with existing null handling; SearchCarScreen search bias is unchanged (bias only).
- DELIBERATELY NOT DONE: no ACCESS_BACKGROUND_LOCATION, no always-on location foreground service (a permission/battery decision Robert has not made); upstream SelfUpdater stays disabled (verified still early-returning in this tree); no routing/traffic/places/search-ranking/voice/backup/palette/updater/puck-visual/Bluetooth/SunShading change. No "Altima" in any user-visible string (verified: no hit in the published resources.arsc). New strings car_waiting_gps, route_locating, route_stale_timeout (translatable=false).
- Verification: local :app:compileDebugKotlin -PappId=app.automap green in the VM (BUILD SUCCESSFUL in 3m 2s after one real failure fixed on the way — a missing CarToast import in RoutePreviewCarScreen.kt; remaining warnings pre-existing, none in this build's files). No unit test was added for the freshness gate: Compose/Android-graphics + car-screen code with no Robolectric harness in this tree, so behaviour is verified by code inspection + dex/resources presence, stated not claimed. A full local :app:assembleDebug was not run; the full APK was verified from CI, which is the GitHub-native model's authoritative build. Device verification is owed on Robert's device, not claimed here: park, drive 2+ km with NO route, phone locked / AutoSpace on the head unit, enter a destination mid-drive on the CAR screen (and on the phone) — the route must start on the current road, not the parked spot; repeat after a cold start at the parked spot, and with a ~30 s GPS gap (expect brief "Locating…" not a confident parked origin).
- Publishing (GitHub-native, not a chat attachment): work commit 7605a910d9a091286fbb30960e8eaa50df29d094 on DocBobert6/AutoMap main. Workflow dispatch run 40616673295 (job 122580674784, build) completed success. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.6, asset AutoMap-0.3.0-alpha.6.apk, 143,794,084 bytes (exact match to the GitHub listing), sha256 45eb7d54aa4c4ca148db4e8a2a6fd4469a16a2fdebcf4f6a49d454c7a6fb8f7d (exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log, so the published artifact is definitively this run's build). Verified by downloading it back through the authenticated API (asset API 302 to the signed URL followed explicitly). aapt2 package app.automap, versionCode 7, versionName 0.3.0-alpha.6, label AutoMap. Dex contains livePuckWithAgeMs, lastKnownWithTime, refreshLocationIfStarved, gateRouteOrigin, myLocationFixMs, AutoMapUpdater, CarAlwaysOn; resources.arsc contains car_waiting_gps, route_locating, route_stale_timeout and no "Altima" (any case). The old releases are untouched.
- CERT CAVEAT: the CI APK is signed with the GitHub runner ephemeral debug keystore, cert DN "Android Debug", SHA-256 8714cf0700230944120a2cbff5be05e80817a3372024463f13d278ef5ec601ce (SHA-1 23bdeac12edf29527086880212dc91979876e3c3) — different from every previous CI and local debug cert. It will NOT install over any earlier build; uninstall first. CI debug builds cannot update over each other. No release keystore was created and the repo was NOT made public, both still-open decisions.

## AutoMap GitHub-native build 8 — repo public + first signed alpha (alpha.7, 2026-10-28)
- Eighth build under the GitHub-native model. Repo DocBobert6/AutoMap changed
  private -> PUBLIC (verified by an unauthenticated read-back: private false,
  visibility public) so the in-app updater's unauthenticated GitHub API check
  no longer 404s on the repo itself.
- RELEASE KEYSTORE (new, stable): generated 2026-10-28, RSA 2048, alias
  automap, DN CN=AutoMap, OU=AutoMap, O=AutoMap, C=US, validity to 2054.
  Cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4,
  SHA-1 e26604fba81b384e44269897552137a86ff3d76d. Stored at
  ~/workspace/automap/automap-release.jks, backed up at
  ~/workspace/automap-keystore-backup/automap-release.jks (paths only; the
  keystore is NEVER in the repo and no password is recorded here). Password
  and keystore material live only as GitHub secrets: AUTOMAP_KEYSTORE_BASE64
  (base64 keystore), VELA_KEYSTORE_PASSWORD (store = key password),
  VELA_KEY_ALIAS (automap).
- WORKFLOW: new .github/workflows/automap-release.yml (manual dispatch only).
  It restores the keystore from secrets, builds :app:assembleRelease
  -PappId=app.automap using the existing releaseFromEnv signing config in
  app/build.gradle.kts, verifies the APK is not debug-signed, and attaches
  it to the versioned alpha prerelease. The debug workflow is left intact
  and separate. Secret values never appear in the workflow or logs.
- FIRST RUN FAILED, FIXED (build-line rule): run 40618273739 failed at
  :app:lintVitalRelease with 145 ExtraTranslation errors — the 8 keys Build 0
  removed from the default locale (settings_support, settings_support_hint,
  settings_support_button, welcome_donate_title, welcome_donate_body,
  welcome_donate_confirm, welcome_donate_dismiss, donate_no_browser) still
  had translations in 20 locale files. Debug builds never run lintVital, so
  this only surfaced on release. Fix commit 098efc07 deleted exactly those
  145 orphaned entries (0 remaining); lint was NOT disabled, and no
  default-locale or feature change was made.
- PUBLISHING: work commit 65fc80ff (version bump + release workflow),
  lint-fix commit 098efc07, workflow dispatch run 40619247995 (job
  122588963846) completed success. Published prerelease
  https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.7, asset
  AutoMap-0.3.0-alpha.7.apk, 117,327,049 bytes (smaller than debug builds:
  release R8 minify + shrinkResources), sha256
  db3c14b447919b85d326cc316af2dabdf5fde78a513d8b883a6ca1dd58383f29 (exact
  match to GitHub's asset digest AND to the sha256sum in the CI job log, and
  the CI log's signer cert matches the new release cert above). Verified by
  downloading it back unauthenticated from the public release page: aapt2
  package app.automap, versionCode 8, versionName 0.3.0-alpha.7, label
  AutoMap. Old releases untouched.
- UPDATER VERIFICATION (unauthenticated, public repo): GET
  api.github.com/repos/DocBobert6/AutoMap/releases?per_page=20 now returns
  200 with alpha.7 first, asset AutoMap-0.3.0-alpha.7.apk — the exact asset
  AutoMapUpdater's Alpha channel picks (first AutoMap-*.apk, newest version
  wins), and a release build has BuildConfig.DEBUG false, so the updater's
  auto-check gate that was dormant on debug builds is now live. Note:
  releases/latest still 404s, but that is GitHub's own rule (latest
  excludes prereleases and AutoMap has no stable release) — it is only used
  by the Stable channel, not Alpha. The updater's 404 error copy still says
  the repo is private; that text is now stale and is a small follow-up, not
  a functional block.
- INSTALL CAVEAT (one last time): this signed alpha will STILL require
  uninstalling any debug-signed build first, because Android will not
  install a differently-signed APK over one. Every signed alpha after
  alpha.7 uses this same key, so in-app updates should then work without
  uninstalling. No release keystore was created before this build; none of
  the previous debug certs can ever update to this one.
- NOT CHANGED: no app feature code except the version bump, the release
  workflow, and the orphaned-translation cleanup above. No routing,
  traffic, places, search, voice, backup, palette, puck, Bluetooth,
  SunShading, or updater-logic change; upstream SelfUpdater stays disabled.
  No "Altima" in any user-visible string (unchanged).
- STILL OWED, NOT CLAIMED: device verification — install alpha.7 after
  uninstalling the debug build, then update to a subsequent signed build
  in-app without uninstalling; that second step is what proves seamless
  updates, and it cannot be proven from this build alone.

## AutoMap GitHub-native build 9 — six-bug batch (alpha.8, 2026-10-28)

- Ninth build under the GitHub-native model, first SIGNED alpha after alpha.7: work committed to DocBobert6/AutoMap main as a base_tree commit (parent = alpha.7 HEAD 07f13182, 5 files), CI builds from the tree via .github/workflows/automap-release.yml (signed release workflow, NOT the debug workflow). No patch stack was generated or applied. Version bumped together, per the alpha rule: versionName 0.3.0-alpha.8, versionCode 9 (was 0.3.0-alpha.7 / 8).
- TASK (Robert 2026-10-27 21:53, six bugs, in order):
- BUG 1 — map auto-recenters after panning when selecting a route origin/destination. Root causes, all in VelaMapView.kt's camera pass: (a) a sheet appearing/growing nulled lastCameraTarget unconditionally, so the next frame re-flew to the stale cameraTarget after a pan — now it only re-frames when the camera is actually on the target (<200 m); (b) the whole-route fit re-fired on inset-only changes (panel minimise/appear) because its key mixes route hash with insets — now a user pan sets routePannedSinceFit and inset-only refits are swallowed (key adopted, no flight) until a genuinely new route arrives; (c) free-drive follow now also suppresses while pickingOrigin/Dest/Stop (MapScreen.kt), not just while directionsOpen.
- BUG 2 — location marker jumps/drifts with the map when recentering after a small pan. Root cause: the free-drive follow ticker seeded its eased point (browseCam, which is also the painted puck position) from the panned camera centre (cp.target) instead of the live fix, so the puck drifted with the map until it eased home. Now seeded from the live fix; the camera glides to it, and FOLLOW_JUMP_M snaps a far panned-away camera.
- BUG 3 — "Search This Area" occasionally recenters back on the originally searched area. Root causes: (a) it biased from mapCenter, which is only refreshed on camera idle and could still hold the previous area after a pan — now bias and narrow box come from the same live viewport centre; (b) it set state.center, creating a fly target that could yank the camera back — no longer set; (c) the narrow box expired after 30 s, so a slow search lost its narrowing and the marker fit framed the original wide set — expiry removed (box still consumed once, and replaced by the next Search This Area tap).
- BUG 4 — live traffic layer tiling bugs / blank patches at close street zoom. Root cause: ensureTraffic removed the layer AND its source on toggle-off, forcing a full re-fetch of every tile on the next toggle/style pass; at close zoom (z16 tiles overscaled) Google throttles those bursts and missing tiles left blank patches. Now the layer is hidden by visibility NONE with its source kept cached, and re-shown on toggle-on. (A fresh style still has no layer/source, so the add branch re-creates them then.)
- BUG 5 — after destination + Start, a route-select popup on a countdown that would not let him click to select. Identified as FasterRouteCard (the only post-Start route-selection popup with a countdown — 10 s, Switch/No, auto-accept by default), plus NavStopOffer (same defect class). Root cause: the countdown froze only on FOCUS, and on a touch phone tapping a button never gives the card itself focus, so the clock kept running and auto-fired while the driver reached for a button. Both cards now also freeze on any touch press (observed in the Initial pointer pass, never consumed, so the buttons still receive the tap) and resume on release. No default or auto-accept behaviour changed.
- BUG 6 — map scales disappear shortly after launch. Root cause: the scale bar lived inside the fabChromeOk block, so any sheet, search, directions panel, step list, driving chrome or transient prompt shortly after launch unmounted it. It now draws whenever the map is shown, gated only by the landscape side panel and Street View, still lifting over the speed box / nav bar.
- DELIBERATELY NOT DONE: no routing/traffic-data-source/places-search-ranking/voice/backup/palette/puck-visual/Bluetooth/SunShading/updater change; upstream SelfUpdater stays disabled; alpha.6 freshness gates untouched (myLocationFixMs, route() 30 s gate, car live-puck preference); no new keystore, repo visibility unchanged. No "Altima" in any user-visible string (verified: no hit in the changed files or the published dex strings).
- Verification: local :app:compileDebugKotlin -PappId=app.automap green in the VM (BUILD SUCCESSFUL in 3m 15s after one real failure fixed on the way — PointerEventType has no Cancel in this Compose version, so the touch-freeze observes Press/Release only; remaining warnings pre-existing, none in this build's files). No unit test was added for these camera/UI fixes: Compose/Android-graphics code with no Robolectric harness in this tree, so behaviour is verified by code inspection + pushed-source read-back + built-asset checks, stated not claimed. A full local :app:assembleRelease was not run; the full APK was verified from CI, which is the GitHub-native model's authoritative build. Device verification for all six bugs is owed on Robert's device, not claimed here.
- Publishing (GitHub-native, not a chat attachment): work commit 56b219d54640940b4eed649078f09f2b3ee80c40 on DocBobert6/AutoMap main. Workflow dispatch run 40622107586 (job 122598344728, build) completed success via .github/workflows/automap-release.yml. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.8, asset AutoMap-0.3.0-alpha.8.apk, 117,327,049 bytes (coincidentally the same byte count as alpha.7; the content differs — see hashes), sha256 dd1c110157a157c553d5c7928a64f2784f92b5f4f19ff37eb818588f13331705 (exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log, so the published artifact is definitively this run's build). Verified by downloading it back from the public release page: aapt2 package app.automap, versionCode 9, versionName 0.3.0-alpha.8, label AutoMap; CI job log confirms the signer cert is the release cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (SHA-1 e26604fba81b384e44269897552137a86ff3d76d). The old releases are untouched.
- INSTALL NOTE (signed alpha, not debug): this build uses the SAME release key as alpha.7, so it should install OVER alpha.7 in-app without uninstalling — verified by cert match (CI log signer cert = alpha.7 release cert), not by a device install, which is still owed: install alpha.8 over alpha.7 and confirm no uninstall is needed.

## AutoMap GitHub-native build 10 — remove all degoogled claims (alpha.9, 2026-10-28)
- Tenth build under the GitHub-native model. Work commit 1af46885 (parent = alpha.8 main HEAD 38f4380f, 21 files: app/build.gradle.kts + 20 strings files; all blobs re-verified byte-identical by git blob SHA). CI builds from the tree via .github/workflows/automap-release.yml (signed release workflow). No patch stack generated or applied.
- TASK (Robert 2026-10-27): remove ALL claims of "degoogled" from the app. Wording only, no feature/data-source change.
- CLAIMS FOUND AND REMOVED (three keys, default locale + every locale that translated them, 20 strings files):
  - settings_about_hint: "degoogled maps client" (EN) and equivalents — de "degoogelter Karten-Client", es "cliente de mapas sin Google", fr "client cartographique dégooglisé", hu "degoogled térképkliens", it "client di mappe degoogled", iw "לקוח מפות ללא Google", ja "脱Googleの地図クライアント", nl "gedegoogelde kaartenclient", pl "odgooglowany klient map", pt/pt-rBR "cliente de mapas sem Google", ro "client de hărți fără servicii Google", ru "дегуглённый клиент карт", sv "avgooglad kartklient", uk "клієнт карт без Google", zh-rTW "去 Google 化的地圖用戶端", zh "去谷歌化的地图客户端" — all reworded to neutral "maps client" in each language. Rest of the sentence unchanged, including "scraped Google" and "No Play Services required".
  - welcome_tagline: "Maps & navigation, degoogled." and equivalents in de, es, et, fr, hu, it, iw, ja, nl, pl, pt, pt-rBR, ro, ru, sl, sv, uk, zh, zh-rTW — all reworded to plain "Maps & navigation." in each language.
  - welcome_feature_open_source_body: "built for GrapheneOS and other de-Googled phones" and equivalents in all locales above — all reworded to "built for Android phones" in each language.
  - Verified after the edit: grep -r "degoogl" app/src/main/res returns nothing, and none of the three keys in any locale still contains a degoogled / without-Google equivalent. The published resources.arsc likewise contains no "degoogl" and contains the new wordings.
- FLAGGED, NOT CHANGED (functional wording, not app claims, per the build brief): the "Use AutoMap without Google" privacy switch and "No Google" mode title (a real toggle that turns Google requests off); GrapheneOS mentions in PSDS/location tips (OS-specific instructions, not degoogled claims); code comments mentioning degoogled ROMs / the no-Google-SDK implementation (internal, not user-visible); site/ docs (stale upstream Vela copy, not shipped in the APK). The About hint still says "No Play Services required" — a requirement statement, not a degoogled claim; removing it is a separate decision if wanted.
- NOT CHANGED: no feature, data-source, routing, traffic, places, search, voice, backup, palette, puck, Bluetooth, SunShading, updater, or freshness-gate change; upstream SelfUpdater stays disabled; no new keystore, repo visibility unchanged (public). No "Altima" in any user-visible string (verified in the published resources.arsc).
- Verification: local :app:compileDebugKotlin -PappId=app.automap green in the VM (BUILD SUCCESSFUL in 7m, after copying the gitignored obf/TTS runtime jars/AARs from the previous workdir and a local-only gradle.properties JDK17 path that was NOT pushed). No unit test was added: strings-only change, no Robolectric harness in this tree, so behaviour is verified by grep + resources.arsc presence, stated not claimed. A full local assembleRelease was not run; the full APK was verified from CI, which is the GitHub-native model's authoritative build.
- Publishing: workflow dispatch run 40624747079 (job 122606873136) completed success. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.9, asset AutoMap-0.3.0-alpha.9.apk, 117,325,973 bytes, sha256 cbf448fb4274d84f6fc2373a6855e40fb382edb0e8117d8619b2b7f4b290150e (exact match to GitHub's asset digest). Verified by downloading it back unauthenticated from the public release page: aapt2 package app.automap, versionCode 10, versionName 0.3.0-alpha.9, label AutoMap; apksigner cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (the stable release cert, not a debug cert), so this signed alpha should install over alpha.8 in-app without uninstalling — cert match, not device proof.
- STILL OWED, NOT CLAIMED: device verification — no degoogled claim visible anywhere in the app, and the in-app update from alpha.8 to alpha.9 without uninstalling.

## AutoMap GitHub-native build 11 — in-app update download crash fix (alpha.10, 2026-10-28)
- Eleventh build under the GitHub-native model. Work commit cb1f189f (parent = alpha.9 main HEAD c9c043c2, 4 files: app/build.gradle.kts, app/src/main/java/app/vela/update/AutoMapUpdater.kt, app/src/main/res/xml/provider_paths.xml, app/src/main/res/values/strings.xml; all blobs re-verified byte-identical by git blob SHA). CI builds from the tree via .github/workflows/automap-release.yml (signed release workflow, NOT the debug workflow). No patch stack generated or applied. Version bumped together, per the alpha rule: versionName 0.3.0-alpha.10, versionCode 11 (was 0.3.0-alpha.9 / 10).
- TASK (Robert 2026-10-27, audited read-only 2026-10-27): in-app update alpha.7 -> alpha.8 crashed just before the download finished — app simply closed, no error, no installer sheet.
- ROOT CAUSE (Rank 1, code-certain): AutoMapUpdater downloads to cacheDir/updates/, but provider_paths.xml only declared a files-path updates/ root (the old filesDir location) plus cache diag/export roots — NO cache root for updates/. FileProvider.getUriForFile therefore threw IllegalArgumentException at install time, and install() sat outside the runCatching that wraps only fetchApk, with no try/catch of its own, so the exception on the bare automap-update thread killed the process with no dialog. Rank 2 (secondary, only if progress was still 95-99%): silent process death — bare thread, no DownloadManager, foreground service, wake lock, or download keeper.
- FIX: (1) added <cache-path name="updates" path="updates/" /> to provider_paths.xml — download dir unchanged, cache is the right home for a transient APK; verified the FileProvider root now covers the download dir in code (updatesDir = File(cacheDir, "updates")). (2) Wrapped install() in try/catch so any future install-site failure lands in a Failed state with a message ("Could not open the installer: … Try downloading again.") instead of killing the app. All install entry points funnel through install() — download thread, onAppOpened deferred install, Ready-UI Install button — so all are covered. (3) No resume support added: retries still wipe the dir and re-download from byte 0, as before. (4) Stale updater copy fixed: the 404 error and the Settings caveat no longer claim the repo is private or that alphas are debug-signed (repo public and alphas signed since alpha.7); the new caveat says signed alphas should update in-app over the previous signed alpha, and a 404 means the check could not reach the releases, not that you are up to date. Header comments in AutoMapUpdater.kt updated to match (404 bullet, debug-gate bullet, APK-pick bullet).
- DELIBERATELY NOT DONE: no feature, routing, traffic, places, search, voice, backup, palette, puck, Bluetooth, SunShading, or updater-logic change beyond the crash fix + stale copy; upstream SelfUpdater stays disabled; alpha.6 freshness gates untouched; no new keystore, repo visibility unchanged (public). No "Altima" in any user-visible string (verified: no hit in changed files or the published binary strings).
- Verification: local :app:compileDebugKotlin -PappId=app.automap green in the VM (BUILD SUCCESSFUL in 6m 38s; warnings pre-existing, none in this build's files). No unit test was added: updater/install code with no Robolectric harness in this tree, so behaviour is verified by code inspection + pushed-source read-back + built-asset checks, stated not claimed. A full local assembleRelease was not run; the full APK was verified from CI, which is the GitHub-native model's authoritative build.
- Publishing: workflow dispatch run 40629678827 (job 122622897356, build) completed success. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.10, asset AutoMap-0.3.0-alpha.10.apk, 117,325,953 bytes, sha256 4c56b8bc9e413c537342455a4af045afe15ff2ac173c3859e02ee7f771071dba (exact match to GitHub's asset digest AND to the sha256sum printed in the CI job log, so the published artifact is definitively this run's build). Verified by downloading it back unauthenticated from the public release page: aapt2 package app.automap, versionCode 11, versionName 0.3.0-alpha.10, label AutoMap; apksigner cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (the stable release cert, not a debug cert), so this signed alpha should install over alpha.9 in-app without uninstalling — cert match, not device proof. New install-failure string and new caveat wording confirmed present in the published binary; no "repo is private" copy remains in the binary.
- STILL OWED, NOT CLAIMED: device verification — in-app update from alpha.9 to alpha.10, download to 100%, installer sheet appears, app does NOT close, install completes without uninstall. If the device still crashes, the CrashCatcher report / logcat (exception, stack, thread) would settle whether it was this FileProvider site or the Rank-2 silent-death path.

## AutoMap GitHub-native build 12 — settings UI restructure (alpha.11, 2026-10-28)
- Twelfth build under the GitHub-native model. Work commit 59bb265e (parent = alpha.10 main HEAD 30fe652f, 24 files changed + PerformanceSettings.kt deleted; tree read back: Performance file absent, Navigation present, main ref = 59bb265e). CI builds from the tree via .github/workflows/automap-release.yml (signed release workflow, NOT the debug workflow). No patch stack generated or applied. Version bumped together, per the alpha rule: versionName 0.3.0-alpha.11, versionCode 12 (was 0.3.0-alpha.10 / 11).
- TASK (Robert 2026-10-27, scoped by the read-only audit at ~/workspace/settings-and-updater-crash-audit.md §3, plus his 2026-10-28 clarification): restructure Settings so each page is more compact, similar items/toggles are categorized onto their own category pages (more clicks, less clutter/confusion), and every option has an accurate explanation. Clarification applied: EVERY hub page splits into compact sub-category pages — no hub page stays one long scrolling list — not just the hub itself.
- NEW PAGE TREE AS BUILT (hub: 11 rows, Performance gone):
  - Appearance (sub-hub) → Theme (app theme, map scheme, map detail, nav day/night, Material You) / Display (page transitions, font, interface size, app name) / Map style (map look, map colours, map buttons) / Locale (units, clock, app language, directions language, map names — moved here from Map).
  - Map → Layers & buttons (live traffic, transit, topography, sun shading, layers button, parking button, 3D buildings, painted roads, building overlay, compatibility rendering, satellite) / Map behaviour (two-finger tilt, keep north up, start view, category shortcuts) / Addresses (house numbers + link to Appearance > Locale for map names).
  - Places → Places on the map (source, on-map) / Place pages (full place load, reviews/photos, hide adult, hide external links).
  - Navigation → Guidance (keep screen on through pause in bar, vibrate chips, turn declutter, fade buildings) / Vehicle & road display (road label, icon size, arrow colours, icon shape, preview, car always on, Bluetooth permission, tap places, traffic lights, confirm end, speed display, car speed, speeding alert) / Vehicle colour (own page: the full colour picker, linked from Vehicle while the icon is the car) / Cameras & road alerts (camera group + bridge openings, Road-alerts group title deleted) / Route options (NEW phone home: avoid tolls/highways/ferries via vm.setAvoid* — same prefs car settings and route chooser write — plus route-traffic-on-tap and live rechecks) / Links from other apps (link action + Google Maps links, moved from Privacy).
  - Voice → Guidance voice (spoken directions mode, road names, detail, download progress, install-Vela button, TTS engines, volume, speed, advanced/playground/variant) / Voice library (own page; openLibrary lands there directly).
  - Search → Search behaviour (voice search, other locations, contacts, voice-command examples) / Speech recognition (ASR engines, mic provider picker, speech preload) / Quick search buttons (sub-hub row opening the existing quick-chips editor).
  - Offline → Offline areas (download viewport area, places with downloads, cached places + Street View cache, addresses-missing banner) / Storage & cleanup (storage breakdown, clear map cache, delete all, storage location) / Routing downloads (region updates, downloaded list, routing catalogue).
  - Saved → Saved places & lists (places export/import, lists export/import) / Saved routes & parking (saved routes, parking history).
  - Privacy & Google → Google-free & data (Google-free switch, shared links, session rotation, telemetry block, usage counts, privacy-policy button) / What Google is used for (See also LINKS only, to Places/Map/Search/Navigation owning pages) / History (clear history + links to parking history and recorded trips).
  - Diagnostics & trips → Diagnostics (share diagnostics, redact, export, building debug, nav trace, crash reports) / Demo modes (simulate driving, simulate location) / Recorded trips (save trips, name trips, trip list + share/delete/rename).
  - About & updates → About (project, map data, version, installer row, what's new + toggle) / AutoMap updates (auto-check, channel, check/download/install, caveat unchanged from alpha.10).
- ROBERT'S FOUR DECISIONS, AS IMPLEMENTED: (1) Shared-links default OFF under Google-free (GoogleFree.init default true→false, mutableStateOf false, hint now states "Off by default"; existing users who never set it pick up the new default, users who explicitly set it keep their choice). (2) Performance page FOLDED AWAY entirely: file deleted, hub row and SEARCH_INDEX entries removed, enum entry removed; its five rows moved as above. (3) Privacy duplicate list replaced with "See also" LINKS (GoogleUsesSection is no longer rendered anywhere: Privacy uses links, and onboarding uses links too — the section composable remains in the tree unused). (4) Car voice toggle FIXED to preserve Alerts-only: CarVoice.set now flips only VoiceGuide.muted and never touches alertsOnly, so Alerts-only → on → off returns to Alerts-only instead of full silence; effective alerts = muted AND alertsOnly, as MapViewModel.setVoiceMode computes it.
- NO OPTION LOST: every existing option still exists, works and persists under its new home (same preference holders; only the page changed). The one option that could not be moved cleanly as a same-page row is none — but two navigation notes: (a) Privacy "See also" links and Map's map-names link open the owning SECTION's sub-hub (not the exact sub-page), because search/sub-page plumbing is section-level; the user then taps the category row. (b) The layers-sheet satellite toggle and the car settings toggles remain quick controls, not full Settings rows, by design.
- SEARCH-INDEX FIXES: dead upstream-updater entries (settings_update_auto/channel/check_now) deleted; AutoMap updater rows indexed; Performance rows remapped to their new homes; route-traffic-on-tap remapped Privacy→Navigation; Google Maps links remapped Privacy→Navigation; new rows indexed (route avoids, car always on, Bluetooth permission, vehicle colour, satellite, painted roads, storage Internal/SD, crash export); picker VALUES indexed (Alpha/Beta/Stable, Internal/SD, Town/Region/Far, Car/UFO/Ship/Duck, Modern/Classic/Contrast, Imperial/Metric, voice modes); hint text indexed via SEARCH_HINT_INDEX (a hint match shows its row's label and opens its spoke).
- LAYERS SHEET: now a quick panel with a one-line satellite explanation (Esri, Google fallback; Google-sourced imagery not used under Google-free) and a "Map" shortcut row that closes the panel and opens Settings (threaded onOpenSettings through LayersCorner from MapScreen).
- CAR SETTINGS: five toggles unchanged (voice, avoid tolls/highways/ferries, traffic) plus a non-toggle "More settings on the phone" row at the end; voice toggle now preserves Alerts-only (see decision 4).
- ONBOARDING: the Customize page (GoogleUsesPage) no longer duplicates the Google switches; it lists each setting with its owning Settings category and a note that the master Google choice already gates every request, with customization available in Settings after setup. Note: this removes pre-setup per-feature customization — the master choice still guarantees no request before the answer, and the switches are one tap away in Settings after setup.
- EXPLANATIONS REWRITTEN / CORRECTED (audit's 12 flags + 30 rewrites, applied where code-accurate): places-source Open hint (fallback in uncovered areas); privacy intro (session linkage, no name/account); installer warning repeated at the Install button when set for car (audit #22, installer_play line); language hint (directions follow unless a separate Directions language is set); navigation-icon size retitled ("Navigation icon size" + hint sizing all shapes) and arrow-colours discoverability hint when the icon is not the arrow; voice-variant hint now uses the real speaker count (cnt from voiceSpeakerCount()); Flock card/voice hints note they fire even with the layer off; bridge hint adds "On by default" and its group title is gone; storage voices row keeps its label but now names the Search destination in a hint and adds a tappable "Speech recognition" row opening Search; region-updates group hint (default never on its own, small patches, full re-download never automatic); Street View cache hint (panoramas for offline Street View, pruned oldest-first over 150 MB); downloaded-list hint (manifest estimates, ~2.35× zip for packs); WhatsNew toggle hint; saved-places import intro adds JSON and CSV; download-viewport hint states the button is disabled until the map has a view; Bluetooth permission hint states denial leaves the blue dot; zoom-to-turn floor hint states default Town; vibrate hint states per-mode defaults; live-rechecks hint states on by default; clear-history hint adds cached places, "cannot be undone", and that the diagnostics log is not cleared here; map-scheme hint names the separate nav day/night row; nav day/night shows DISABLED (not hidden) under Automatic with "Already automatic"; map colours/buttons show DISABLED (not hidden) on non-painted looks with "Only applies to the Liberty look"; colours/buttons hints state Modern/Round defaults; interface-size and units hints added; mic-provider picker hint added; route-traffic-on-tap keeps its full hint on its new Navigation home; volume chips hint states 0.6×/1.0×/1.6×/2.2× and speed hint states 0.5×–2.0× in 0.1× steps; "Tap places while driving (experiment)" label drops "(experiment)"; About updater caveat NOT regressed (alpha.10 wording kept verbatim).
- CODE CHECKS THAT CORRECTED AUDIT WORDING (task required these before shipping): per-mode vibrate defaults CONFIRMED from Haptics.defaultFor (on walk/bike/transit, OFF driving — audit's example omitted transit, now stated); live-rechecks default CONFIRMED true (MapViewModel.liveRechecksOn, getBoolean nav_live_rechecks, true); Back during a drive CONFIRMED always asks (MapScreen comment + NavEndConfirm: Back always asks, red X only with the toggle on — audit's first wording kept); SpokenDetail Brief CONFIRMED to strip street names regardless of SpokenRoadNames (core SpokenDetail defines BRIEF as short form with no street name, independent of the road-names flag — Brief hint kept as written, now code-grounded); Street View cache retention CONFIRMED as size-only (prune to 150 MB, oldest-first, no time limit — no retention period stated); layers-sheet satellite under Google-free CONFIRMED as Esri with Google fallback gated by NoGoogle (wording states Google-sourced imagery is not used under Google-free); painted-look set CONFIRMED as Liberty only (MapStyle.painted=true only for LIBERTY — "Only applies to the Liberty look" is exact).
- DELIBERATELY NOT DONE: no routing, traffic, places/search ranking, voice publishing, backup, palette, puck, Bluetooth-gating, SunShading, updater-logic, or alpha.6 freshness-gate change; upstream SelfUpdater stays disabled; no new keystore, repo visibility unchanged (public). No "Altima" in any user-visible string (verified: no hit in the published binary's resources; dex clean). No Robolectric harness in this tree, so no unit-test coverage is claimed — verification is local :app:compileDebugKotlin green (multiple runs, BUILD SUCCESSFUL) + pushed-source read-back + built-asset checks.
- Publishing: workflow dispatch run 40633697403 (build) completed success on head 59bb265e. Published prerelease https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.11, asset AutoMap-0.3.0-alpha.11.apk, 117,353,925 bytes, sha256 54279d0abe3071a02d99fbbc85a05e652f311ae50f53788c64a5b45225190147. Verified by downloading it back unauthenticated from the public release page: aapt2 package app.automap, versionCode 12, versionName 0.3.0-alpha.11, label AutoMap; apksigner cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (the stable release cert, not a debug cert), so this signed alpha should install over alpha.10 in-app without uninstalling — cert match, not device proof. New category strings ("Vehicle colour", "Route options", "Google-free and data", "More settings on the phone") confirmed present in the published binary's resources; no "Altima" present.
- STILL OWED, NOT CLAIMED: device verification — every new category page opens, every moved option still works and persists, every explanation matches what the option actually does, no option lost; in-app update from alpha.10 to alpha.11 without uninstall; layers-sheet shortcut opens Settings; car settings "More settings on the phone" row renders in the car; onboarding Customize page shows links (first-run only, not re-testable after setup without clearing data).

## 2026-10-28 — alpha.12: remove remaining user-visible "Vela" mentions, credit only (GitHub-native build 13)

Work commit 733370f1 (parent 27ff449f, alpha.11 HEAD). CI run 40635939246 (signed release workflow .github/workflows/automap-release.yml) green. Release v0.3.0-alpha.12 (prerelease), asset AutoMap-0.3.0-alpha.12.apk, 117,354,445 bytes, sha256 b5dc4349d1f2fb38a6c8289cf20577652b5b036b3b2b91ca575dc5703d318833 (matches GitHub asset digest and CI job-log sha256sum exactly), package app.automap, versionCode 13, versionName 0.3.0-alpha.12, signing cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (stable release cert, not a debug cert) — should install over alpha.11 in-app without uninstalling; cert match only, device proof still owed.

Scope: Robert ordered (2026-10-28) ALL remaining user-visible "Vela" mentions removed except the credit. Findings: default + all locale strings.xml already had zero standalone "Vela" in user-visible text (Build 0 rename had done those); the real remainders were in Kotlin code and res XML comments. Every user-visible one was reworded; the About credit was missing as text, so it was ADDED (see below).

Mentions found and removed (old -> new):
- app/src/main/java/app/vela/diag/CrashCatcher.kt: "Vela crash report" header/subject/body -> "AutoMap crash report".
- app/src/main/java/app/vela/diag/DiagExporter.kt: JSON "app":"Vela" -> "app":"AutoMap"; filename "vela-diag-" -> "automap-diag-"; subject "Vela debug session" -> "AutoMap debug session"; body "a Vela diagnostics export" -> "an AutoMap diagnostics export".
- app/src/main/java/app/vela/diag/NavTrace.kt: filename "vela-nav-trace-" -> "automap-nav-trace-"; header "# Vela nav smoothness trace" -> "# AutoMap nav smoothness trace"; build line "# vela <version>" -> "# automap <version>"; subject "Vela nav smoothness trace" -> "AutoMap nav smoothness trace".
- app/src/main/java/app/vela/ui/map/MapViewModel.kt: TripNote "build: Vela <version>" -> "build: AutoMap <version>".
- core/src/main/java/app/vela/core/voice/VelaPiper.kt: voice-library LABEL "Vela voice" -> "AutoMap voice" (user-visible in the voice picker).
- app/src/main/res/xml/automotive_app_desc.xml: comment "Declares Vela as a templated car app" -> "Declares AutoMap as a templated car app".
- app/src/main/res/xml/provider_paths.xml: comment "downloaded Vela APK" -> "downloaded AutoMap APK".
- app/src/main/res/values/themes.xml and values-night/themes.xml: comments "Vela's own" -> "AutoMap's own" / "runs Vela dark" -> "runs AutoMap dark".
- Code comments reworded for accuracy (not user-visible): MapViewModel KDoc "Vela voice" -> "AutoMap voice"; PiperCatalog "a Vela voice" / "get a Vela voice" -> AutoMap wording; SearchSettings "Vela Voice" = AUTO -> "AutoMap Voice" = AUTO; Onboarding "download the Vela neural voice?" -> "download the AutoMap neural voice?"; MapScreen UpdateCard "A newer Vela is out" -> "A newer AutoMap is out".
- Repo docs: root docs references to Vela are upstream credit/history (README "AutoMap is a build of Vela Maps", PLAN "Slipstream build of Vela Maps", BUILD_AND_SYNC "Slipstream of upstream Vela at 85eb69c") and were NOT rewritten — rewriting them would falsify attribution. docs/book, FEATURES, SPEC, PRIVACY and site/ are upstream documentation/history, not app UI. BUILD_LOG history was not rewritten.

Credit ADDED (was missing as About text; the only credit before was by link): new default-locale string settings_project_credit — "AutoMap is a slipstream of Vela (GPLv3). The Website and GitHub buttons below open the upstream Vela project." — rendered as a Hint in the About Project group (AboutSettings.kt), above the Website/GitHub buttons. Other locales fall back to the default value. This credit line is the ONLY standalone "Vela" text in the published resources.arsc (verified by strings dump), plus the internal style symbol name "Theme.Vela" (not user-visible text).

Exception list (NOT renamed, non-user-visible):
- Internal Kotlin package app.vela.* / imports — renaming would break upstream rebase.
- VelaLinks / data-manifest URLs (site/repo/downloads for maps, places, road features) — the About Website/GitHub buttons still open upstream Vela (velamaps.org / github.com/PimpinPumpkin/Vela) by design; destinations unchanged.
- ENGINE_ID "vela.piper" — stable internal routing id (comment in code says stable); VoiceGuide routing + voice_engine pref depend on it. Only the user-visible LABEL changed.
- Preference/file names (vela_settings, vela_location), log tags, style layer ids (vela-paint-*, vela-parking, vela-hillshade) — internal, never shown.
- Code comments mentioning Vela (e.g. "Vela's own traffic/places", "upstream Vela self-updater DISABLED", "Use Vela without Google" in KDoc) — internal documentation, not app text; a few voice-related ones were reworded above for accuracy, the rest left.
- About Project GitHub button label still reads "AutoMap on GitHub" while its destination is the upstream Vela repo — flagged, not changed: label change would need locale-consistent handling and was not ordered; the new credit line now explains the buttons open the upstream project.

No behaviour-tied Vela mention was silently changed. No feature, routing, traffic, places, search, voice, backup, palette, puck, Bluetooth, SunShading, updater-logic, alpha.6 freshness-gate, or alpha.11 settings-tree change (only rewording inside it). Upstream SelfUpdater stays disabled. No new keystore; repo visibility unchanged (public). No "Altima" in any user-visible string (verified: no hit in published resources.arsc).

Verification: local :app:compileDebugKotlin -PappId=app.automap green (7m 41s, warnings pre-existing, none in changed files); pushed blobs verified byte-identical to local files (git hash-object); pushed source read back from GitHub main confirms all rewordings and the credit string; published APK downloaded back and verified (package/version/size/hash/cert above); published resources.arsc contains "AutoMap voice", the credit line, and no "Vela voice"/"Vela crash"/"Vela debug"/"Vela nav" wording and no "Altima". No Robolectric harness exists in this tree, so no unit-test coverage is claimed. Device verification owed: no Vela visible anywhere except the About credit line; settings tree intact; in-app update from alpha.11 without uninstall.


## 2026-10-28 — alpha.13: Phase D — host painted-road regions, region-aware baked source (GitHub-native build 14)
Work commit 45015728 (parent c90a704b, alpha.12 HEAD). CI run 40640907032 (signed release workflow .github/workflows/automap-release.yml) green. Release v0.3.0-alpha.13 (prerelease), https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.13, asset AutoMap-0.3.0-alpha.13.apk, 117,354,445 bytes (same byte count as alpha.12 by coincidence; content differs — alpha.12 sha256 b5dc4349d1f2fb38a6c8289cf20577652b5b036b3b2b91ca575dc5703d318833, alpha.13 below), sha256 8e574d785dcff01fdd240656c72e19771dbbf824f21287068c87afaec104fe16 (exact match to GitHub's asset digest), package app.automap, versionCode 14, versionName 0.3.0-alpha.13, label AutoMap, signing cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (stable release cert, not a debug cert) — should install over alpha.12 in-app without uninstalling; cert match only, device proof still owed.
TASK (Robert 2026-10-28, last overnight step before Phase E): actually HOST the painted-road region bakes so painted-road markings for those regions go LIVE in the app, not just baked locally; plus two additional regions selected by measurement, not personal geography (Robert's 2026-10-28 00:52 correction, superseding the 00:47 personal-geography reading for the additional two).
HOSTING: the five PMTiles files live in this repo under painted-regions/ on main, served unauthenticated by raw.githubusercontent.com. Raw was chosen over a release-asset URL because raw answers HTTP Range requests (verified: Accept-Ranges: bytes on a raw file, and after push every hosted file returned HTTP 206 with PMTiles magic on a range fetch), which the streamed-PMTiles reader needs to fetch only visible tiles; a release-asset URL 302s to a time-limited presigned URL, a poor streaming source. painted-regions/painted-regions-manifest.json is the fetchable, checkable copy of the same list (id, name, url, sha256, sizeBytes, bbox [S,W,N,E], priority). The app compiles the same list in (core PaintedRegions) so a style can be built synchronously — the car snapshotter style JSON and the phone applyBaked path are synchronous and cannot stall on a manifest fetch; the repo manifest is the fetchable copy, not a runtime fetch the style waits on. That distinction is stated plainly, not claimed as a runtime fetch.
REGIONS (all 5, with hosting URLs https://raw.githubusercontent.com/DocBobert6/AutoMap/main/painted-regions/<file>):
- Cincinnati metro — bbox -84.9,38.8,-84.0,39.5; painted-cincinnati-metro.pmtiles, 3,924,594 B, sha256 58c4b0cbef06fea5ff9a75fd8263c99d9db29c792c363568704027131987c7d9; 86,546 ways (32.2% lanes), 153,621 marks (Build 12 bake, re-hosted unchanged).
- Northern Kentucky — bbox -85.1,38.5,-84.2,39.1; painted-northern-kentucky.pmtiles, 1,377,475 B, sha256 b11cba8dc778457064dfd52590b495feeccfdd345633838af1b10ade24d49db5; 21,325 ways (28.4% lanes, 873 turn:lanes), 33,157 marks.
- Central Kentucky — bbox -85.3,37.6,-84.0,38.5; painted-central-kentucky.pmtiles, 2,468,503 B, sha256 fe03beb10d51162ad4df8669a95faec3907986d68fff7f2dd4a4b97225611a46; 36,271 ways (11.8% lanes), 34,155 marks.
- Houston — bbox -95.80,29.50,-95.00,30.05; painted-houston.pmtiles, 7,819,391 B, sha256 eb323b065c8c76eafc49bd20555efc7fcb8721f4d13e3d8a791662b1d473b6a7; bake 200,361 ways, 81,721 nodes, 355,867 marks (SURFACE 72,996, CENTER 50,939, LANE 124,954, ARROW 22,178, BIKE 3,850, CROSSWALK 19,172, STOP 36,717, MEDIAN 25,061).
- Dallas-Fort Worth — bbox -97.40,32.60,-96.50,33.10; painted-dallas-fort-worth.pmtiles, 10,430,637 B, sha256 3dbefc2da75a68bbc41e3dd94a8830d5387c98e7be13342ea93ad9da13b5323b; bake 348,500 ways, 107,942 nodes, 506,952 marks (SURFACE 112,718, LANE 189,000, ARROW 31,961, CENTER 65,757, CROSSWALK 27,085, BIKE 4,733, STOP 37,964, MEDIAN 37,734).
MEASURED SELECTION (pre-declared rule, applied before seeing the ranking: rank by highest combined lanes + turn:lanes coverage, motorway_link/interchange count as tie-breaker; same Build 12 filter — osmium tags-filter, then osmium export — on Geofabrik state extracts, clipped to metro-core bboxes). Results: Houston 203,609 ways, 33.94% lanes, 9.93% turn:lanes, 14.84% lanes≥3, 4,851 motorway, 3,718 motorway_link (18.26/1,000 ways) — rank 1; Dallas-Fort Worth 351,090 ways, 30.44% lanes, 7.62% turn:lanes, 16.28% lanes≥3, 7,165 motorway, 7,318 motorway_link (20.84/1,000) — rank 2; Atlanta 138,599 ways, 17.01% lanes, 3.84% turn:lanes, 1,588 motorway, 2,608 motorway_link (18.82/1,000); Chicago 317,579 ways, 7.21% lanes, 1.42% turn:lanes, 1,665 motorway, 1,684 motorway_link (5.30/1,000). Houston and Dallas-Fort Worth win clearly on both the primary metric and the tie-breaker; Chicago's sparse lane tagging (7.21%) is exactly why measurement, not a guess, was used. Limitation stated: only these four metros were measured (Texas extract covered both winners in one download); Phoenix/Seattle were excluded to keep state downloads tractable and were not measured. New bakes used the Build 12 pipeline verbatim: filtered export → :core:testDebugUnitTest --tests '*PaintedRoadsBakeTest' (-DvelaPaintIn/-DvelaPaintOut) → tippecanoe -l paint -z15 -Z15 -P -b 24 --no-feature-limit --no-tile-size-limit --no-line-simplification --force. Coverage follows OSM tagging and nothing else — sparse tagging reads as sparse markings, no markings invented. (One measurement hiccup, recorded honestly: the first Atlanta clip came out 0 bytes and failed the filter; a clean re-extract produced 40 MB and the numbers above.)
CLIENT SIDE: new core object PaintedRegions (pure Kotlin, unit-testable): pick(lat,lng) returns exactly one covering region — lowest priority, then smallest bbox area — and pickUrl wraps it as pmtiles://. OVERLAP RULE, AS IMPLEMENTED: Cincinnati metro is priority 1, every other region priority 2, so in the Cincinnati/Northern-Kentucky overlap the metro file loads and the Northern Kentucky file does not — never both, so no double-draw. (Priority outranks area deliberately: the Northern Kentucky bbox is smaller in area, 0.54 deg² vs metro 0.63 deg², but the metro file spans the state line and is denser, 32.2% lanes / 153,621 marks vs 28.4% / 33,157.) Phone: PaintedRoadsLayer.applyBaked(style, dark, lat, lng) follows the camera target; VelaMapView dial-1 passes mapRef camera target; the region id is part of the source key so crossing a boundary swaps the source. Car: CarStyle.inject takes an optional paintUrl; CarMapRenderer passes PaintedRegions.pickUrl(puck lat/lng) at the inject call site. Outside every hosted region both paths fall back to the California bake via VelaLinks.paintedRoads(), as before. Deliberately NOT derived from VelaLinks.RELEASES (which defaults to upstream PimpinPumpkin/Vela) — the DocBobert6/AutoMap raw URLs are hardcoded in PaintedRegions so painted-region hosting never points upstream. No new user-visible strings were added by this build.
CONSTRAINTS HONOURED: no routing, traffic, places, search, voice, backup, palette, puck, Bluetooth, SunShading, updater-logic, alpha.6 freshness-gate, or alpha.11 settings-tree change; upstream SelfUpdater stays disabled; no new keystore; repo visibility unchanged (public). No "Altima" in any user-visible string (verified: no hit in changed files; published APK's hosted-region URL strings present in dex — PaintedRegions/pickUrl names themselves are R8-obfuscated in the release build, as expected, so dex-name presence is not claimed; the evidence is the pushed source + local compile + test + URL strings in dex).
VERIFICATION: local :core:testDebugUnitTest --tests '*PaintedRegionsTest*' green, 6 tests / 0 failures (overlap picks metro, Dry Ridge picks northern, Lexington picks central, outside all picks nothing, pickUrl shape, every region has URL/sha/size); local :app:compileDebugKotlin -PappId=app.automap green (warnings pre-existing, none in this build's files). Full local assembleRelease not run; full APK verified from CI. Pushed blobs verified byte-identical to local files by git blob SHA (all 13 files, including all 5 PMTiles). Hosted files verified unauthenticated after push: all 5 raw URLs return HTTP 206 on a range fetch with PMTiles magic, and the manifest lists all 5 correctly. Manifest, code list, and hosted files were cross-checked by script (hash, size, id, sha, bbox, priority) — identical, modulo trailing-zero formatting (29.50 in Kotlin source vs 29.5 in JSON). Note: this build's local Gradle config (JDK 17 toolchain line in gradle.properties) was restored before pushing, as in previous builds; it is not in the pushed tree.
STILL OWED, NOT CLAIMED: device verification — painted markings actually appearing on the car screen in each hosted region (Cincinnati, Northern KY, Central KY, Houston, Dallas-Fort Worth), and no double-draw in the Cincinnati/Northern-KY overlap; in-app update from alpha.12 to alpha.13 without uninstall. PAUSED before Phase E (AutoSpace companion round) as ordered — Phase E NOT started.


## 2026-10-28 — alpha.14: Phase F — exit shields visible in nav, palettes locked, 2D/3D default 3D (GitHub-native build 15)
Work commit fdb6f853 (parent a4c649d6, alpha.13 HEAD; 9 files). CI run 40685897150, job 122808044142 (signed release workflow .github/workflows/automap-release.yml) completed success. Release v0.3.0-alpha.14 (prerelease), https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.14, asset AutoMap-0.3.0-alpha.14.apk, 117,355,113 bytes, sha256 f0d9de5ae48a41a2d7f5dcd82c596bf438095baae77dd4d8f5955cf6e0925dfd (exact match to GitHub's asset digest), package app.automap, versionCode 15, versionName 0.3.0-alpha.14, label AutoMap, signing cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (stable release cert, not a debug cert) — should install over alpha.13 in-app without uninstalling; cert match only, device proof still owed.
TASK (Robert 2026-10-28 07:53, "I agree with all of this", on the seven Phase F questions): build items 1-3, record items 4-7 as no-change decisions.
1. EXIT SHIELDS VISIBLE DURING NAVIGATION: the three highway-shield layers (highway-shield-non-us, highway-shield-us-interstate, road_shield_us) are removed from BOTH nav-declutter lists — the phone drive-nav strip in VelaMapView and the car DECLUTTER_LAYERS in CarMapRenderer — so Build 15's enlarged shields (icon 1 -> 1.35, text 10 -> 13 in CarStyle) are no longer hidden by the Build 5 declutter during a drive. All other declutter items hide as before (POI tiers, transit, bike/trails, hamlet labels, one-way arrows, address overlays). The phone's vela-exit-shield layer was already kept by TurnDeclutter (KEEP list). Car note, stated plainly: the car snapshotter style has no vela-exit-shield layer (CarStyle.inject never adds it, and a snapshotter cannot add layers after load), so on the car the visible shields are the enlarged highway shields, not the phone's exit-number badges.
2. PALETTES LOCKED — code check first, then lock. CHECK RESULT: MapColors COULD override — applyMapTheme branched on classic()/contrast(), so Classic replaced both AutoMap day and night palettes and Contrast replaced the night palette. MapLook non-Liberty looks COULD also override in effect: applyMapTheme is gated on painted, so Bright/Positron/Fiord/Black loaded as served with their own colours and no AutoMap palette at all. LOCK, central in the holders so every reader sees it: MapColors.current() is always MODERN, classic()/contrast() always false, set() ignores non-Modern picks and init() ignores any saved pre-lock pick; the calibration bundle's defaultMapPalette is likewise ignored. MapLook.init() ignores any saved non-Liberty pref and set() ignores non-Liberty picks (Liberty = DEFAULT). Settings > Appearance > Map style shows the other looks/colours disabled, not removed, with a new settings_map_palette_locked hint (default locale only; other locales fall back). The layers sheet routes through the same MapLook.set, so it is covered too. Automatic/Day/Night (AppTheme.mapMode) and sun-angle shading are untouched: they choose day vs night WITHIN Modern.
3. 2D/3D FIRST-INSTALL DEFAULT = 3D: verified, NO code change needed — MapDetail already defaults to 3D (in-memory true, init reads map_detail_3d with default true). A comment records the Phase F verification. Existing installs keep their saved choice (init reads the pref); 2D remains one tap away.
NO-CHANGE DECISIONS (recorded, not built): keep "No Play Services required" in the About hint; no background-location permission and no always-on location service; FasterRouteCard taken as confirmed as the countdown popup (Phase C complete) — no code change; dropping the Vela app entirely left for later, no backup/restore/uninstall path touched.
CONSTRAINTS HONOURED: no routing, traffic, places, search, voice, backup, puck, Bluetooth, SunShading, updater-logic, alpha.6 freshness-gate, alpha.11 settings-tree, or Phase D hosted regions / overlap rule change; upstream SelfUpdater stays disabled; no new keystore; repo visibility unchanged (public). No "Altima" in any user-visible string (verified: 0 hits in the published resources.arsc; the locked-palettes hint is present).
VERIFICATION: local :app:compileDebugKotlin -PappId=app.automap green (9m 36s, warnings pre-existing, none in changed files); pushed blobs verified byte-identical to local files by git hash-object (all 9 files). Full local assembleRelease not run; full APK verified from CI. No Robolectric harness exists in this tree, so no unit-test coverage is claimed. Device verification owed, not claimed: shields visible during a real drive (phone and car), palettes not overridden by a colour/look pick, fresh-install default 3D with existing installs unchanged, in-app update from alpha.13 without uninstall.
STILL OWED, NOT CLAIMED: device verification as above. NEXT PHASE: Phase E (AutoSpace companion round) — NOT started here, per the brief; it still waits on Robert making a new AutoSpace version.


## 2026-10-28 — alpha.15: audit fix — dead code, cleanliness, UI consistency, car surface (GitHub-native build 16)
Work commit 08eb5e6f (parent e693b477, alpha.14 HEAD; 72 files). CI run 40693860457 (signed release workflow .github/workflows/automap-release.yml) completed success. Release v0.3.0-alpha.15 (prerelease), https://github.com/DocBobert6/AutoMap/releases/tag/v0.3.0-alpha.15, asset AutoMap-0.3.0-alpha.15.apk, 117,309,713 bytes, sha256 91feef445c8886d1466e156ef317d445b07d03bdb9e193a9ab63d6b6f36b4733 (downloaded back and hashed), package app.automap, versionCode 16, versionName 0.3.0-alpha.15, label AutoMap, signing cert SHA-256 05b94eb54c7a79bd7c2da295b77613c5149f30b682bab335c2245982ce47bde4 (stable release cert, not a debug cert) — should install over alpha.14 in-app without uninstalling; cert match only, device proof still owed.
TASK (Robert 2026-10-28 08:21, "do your recommendations", with two guardrails: nothing interferes with anything already decided; AutoMap stays fully compatible with AutoSpace and AutoSpace's assistant/AI assistant): implement the full audit recommendations from ~/workspace/automap-audit-full/AUDIT_REPORT.md (snapshot alpha.13), re-checked against the alpha.14 tree before acting.
DEAD CODE REMOVED (each only where grep proved it dead): the upstream SelfUpdater cluster as one unit — SelfUpdater.kt, ApkChoice.kt, ApkChoiceTest.kt, updateInfo/updateApkPending/updateDownloadPct state, selfUpdater constructor param, maybeCheckForUpdate/checkForUpdateNow/downloadUpdate/installPendingUpdate/sharePendingUpdate/dismissPendingUpdate/dismissUpdate/cancelUpdateDownload, the pending-APK dialog and UpdateCard composable in MapScreen, the if(false) upstream block in AboutSettings, and 10 if(false)-only strings plus the shared card strings they orphaned (update_available_title, update_downloading, update_later, update_install, update_download_failed, update_car_title, update_car_body, update_car_save, update_car_anyway, download_label_update). AutoMapUpdater is a SEPARATE live path and was NOT touched (only a stale comment reworded). Dead function GoogleUsesSection (its rows are live and kept). 14 dead main-source Kotlin symbols: TripStore.loadRoute, rememberNoTouchDevice, PermissionRationale (whole file), statusGreen, setSpokenDirections, neuralVoiceInstalled, piperInstalled, voiceWorking, installableEngines, installVoiceEngine (+ installingEngine state), PanelControls, QuickChipsRow (row only; the QuickChips screen is live), WebPopularTimesFetcher.prewarm, GoogleRasterTiles (embedded mt0..mt3.google.com URLs the project deliberately avoids). 79 dead base strings + orphan translations in a scripted pass (exact-token grep, not substring: R.string.car_pause falsely matches car_paused, R.string.settings_car_blue falsely matches settings_car_bluetooth_permission), 99 keys pruned in total including the SelfUpdater strings. Dead drawables ic_car_pause/ic_car_play, dead script merge-road-features-manifest.sh, 6 dead site assets (07-bus-stop.webp, 10-stop-routes.webp, 11-stop-list.webp, badge-fdroid.png, badge-obtainium.png, voice-demo.m4a — zero references repo-wide). tools/check-translations.py after the prune: only the PRE-EXISTING settings_voice_variant_hint placeholder mismatch (default %1$d vs translations "hundreds of speakers"), present in alpha.14 HEAD too, not caused by the prune and not fixed here (translation copy, not code; the audit did not order it).
REPAIR, RECORDED HONESTLY (the Stage 1 brace-counting deletion helper over-deleted, and the first compile proved it): single-expression functions carry no braces, so the counter ran into the NEXT function and deleted it, and a deleted function's @Composable annotation was left orphaned on the following function. Restored verbatim from HEAD: MapViewModel.setVoiceMode, MapViewModel.speakText, SheetPalette.bg (live, used by MapScreen/NavOverlays), DpadFocus.rememberDpadMode. Removed orphan @Composable annotations (and their dead KDocs) in DpadFocus, QuickChipsSettings, MapScreen (UpdateCard's, which had made plainReleaseNotes composable and broke WhatsNew), PlaceSheet (PanelControls', which had doubled RatingHistogram's) and GoogleUses (trailing KDoc + annotation with no function). CategoryRow's new enabled parameter was moved BEFORE onClick: as the last parameter it stole every trailing lambda (the first compile's systematic error). Lesson applied from the workspace's own rules: a fix script that asserts stops the chain, and patched regions are re-read visually, not trusted to anchor counts.
CLEANLINESS: NavFraming.speedZoom shared by both phone nav tickers (was the formula verbatim twice); NavTurnTarget.isQuiet shared by CarMapRenderer.nextNonQuietManeuver (was the CONTINUE/STRAIGHT + fork test inlined); AlongRoute car distance steps now match Format.formatDistance / the car nav card (50 ft steps at/above 100 ft, 10 ft below — the always-10 ft drift is gone); GeoConstants (M_PER_DEG 111_320.0, M_PER_PX_Z0 78271.517) replaces all 29 + 4 literals across 9 + 4 files; CarVoice.set persists the NORMALIZED alerts flag (muted AND alertsOnly), matching MapViewModel.setVoiceMode; painted-roads loop calls ensureActive() so cancellation propagates instead of becoming a silent null inside runCatching; silent fallbacks now log (CarStyle.inject parse failure, ManeuverMapper.glyph failure, RoadFeatures region-parse failure) with no behaviour change; stale comments fixed (painted roads no longer called a "test" in PaintedRoadsLayer/VelaMapView; CarSettings comment corrected). NOT unified/renamed, deliberately: prefs() stays per-file (renaming vela_settings would break existing installs); phone MapLibre vs car JSON painted layers stay duplicated (different renderers, unification is a rewrite, not a cleanup); AutoMapUpdater L2/L7 hardening skipped as not low-risk against the alpha.10 fix.
UI CONSISTENCY: naming pass as one strings-only change, British spelling (the app already leans British): Map colors -> colours, Color scheme -> Colour scheme, Material You colors -> colours, Arrow colors -> colours; Live traffic overlay -> Traffic; Surveillance cameras -> Plate cameras (Flock kept once, in the hint); Navigation icon -> Vehicle icon (code name Puck stays internal); Google-free polarity fixed — settings_google_free "Use AutoMap without Google" -> "Google-free" (ON = Google-free on, matching Welcome "No Google"); About hint "scraped Google" -> "Google's public endpoints" (matching the Privacy intro); Google-free hint now states the shared-links exception up front; end-confirm hint notes the car host-stop behaviour; Welcome Google-uses line points at the owning pages; "See also: owning pages" -> plain language; hub subtitles corrected (Appearance lists Theme/Display/Map style/Locale; Map lists Layers and buttons/Map behaviour/Addresses; About drops stale "support"). Search: results now show "Section -> Sub-page" from SEARCH_SUBPAGE (settings_category_chips fixed from MAP to SEARCH — the chips live under Search > Quick search buttons; the Map row is only the mode switch); the audit's stated alternative (path under the result) was taken instead of threading highlight through every sub-hub. Disabled-vs-hidden house rule implemented in SelectableRow/CategoryRow (enabled parameter, disabled rows lose click/chevron and dim): Navigation sub-hub Vehicle-colour row removed (R-04 — the Vehicle page's own link is the single entry point); Vehicle-colour and arrow-colour rows are now always visible, disabled with an explaining hint unless the shape is car / arrow respectively, instead of hint-only dead ends. Layers panel (A-03) gains sun-shading, painted-roads and fill-missing-buildings tiles (same holders Settings flips); sun-shading joins the button-accent set (it is beyond the default map); the alpha.11 "Map" shortcut row remains as the route to everything else.
CAR SURFACE (AutoSpace-safe: no CarAppService, NAVIGATION category, FEATURE_CLUSTER, turn publishing, routing, traffic, places, search-ranking or voice-publishing change; only existing static holders and existing CarDeps fields, plus existing car templates): A-01 voice row now shows the REAL mode (On / Alerts only / Off, reusing settings_voice_mode_on/alerts/off) and tapping it steps through the same three modes as the drive's speaker button (CarVoice.step) — the old toggle read muted, so Alerts-only showed OFF. A-04 recents added to SavedCarScreen from existing CarDeps.recentPlaces (saved routes stay phone-only — wiring them needs CarAppService changes, Phase E scope, reported honestly, not claimed). A-05 terrain toggle added (Topography holder; the car renderer already honours it and hides hillshade during nav, like the phone); transit and places-source toggles NOT added — the car snapshotter style has no transit layer and no ambient places layer, so they would change nothing on the car map and would be misleading toggles. A-06 offline-status row in car settings: saved-areas count from OfflineMaps.list (cached, refreshed on callback; no row until the callback lands, so no guessed zero; viewport coverage is not available in a car template, so the row reports count, not coverage). A-11 car search shows QuickCategories.all() chips (capped at 6, running runSearch(chip.query, submit = true)) when nothing has been searched yet; a searched-but-empty query keeps the "nothing found" hint. R-03 "More settings on the phone" row removed — ListTemplate has no footer slot and a non-action row reads as a broken button.
ABOUT / ONBOARDING: A-09 What's New toggle (whatsnew_show_after_updates) moved from About to the AutoMap updates group (it belongs with the updater that triggers the notes); the Updates group title removed because the page is already titled "AutoMap updates"; search sub-page path updated to AutoMap updates. R-09 onboarding header corrected: no donation prompt, no "Support Vela" Settings entry (support was removed in Build 0; credit lives in About); location KDoc corrected to match VelaRoot.kt, which fires the system dialog directly with no separate rationale screen (verified in code).
ADDS/REMOVES NOT TAKEN, WITH REASON: A-10 toll prices SKIPPED — no toll-price data in the directions path (only avoidTolls/avoidHighways/avoidFerries flags); no estimator built, never ship an estimate as a price. ReviewsPanel inline branch KEPT — only caller passes fullScreen = true and the inline path is intertwined with the full-screen WebView code; removal is high-risk for low value. scripts/old-android-smoke.sh KEPT — its claimed workflow file does not exist, but the script itself is a standalone emulator dev tool (installs an APK, walks onboarding), not app dead code; deleting a dev tool on "workflow missing" evidence alone would be overreach. U-14 hub order (Search behind Voice) left — the audit's alternative (rename Voice) was not taken either; order unchanged, no rename. U-31 Driving mode vs Drive naming left — the car icon is untitled by AA convention and renaming the phone title would break driving_mode_leave. U-20 other settings duplicates left — the Privacy Uses page already follows the link-only pattern; other duplicates are cross-page links, not copies.
CONSTRAINTS HONOURED: Phase F shields visible during nav, palettes locked (MapColors/MapLook/MapDetail untouched in this build), 3D first-install default, alpha.11 settings tree (only the A-09 toggle move within it), day/night palettes, puck + colour picker, Bluetooth gate, SunShading prompt, alpha.6 freshness gates, Phase D regions + overlap rule (PaintedRegions untouched), Vela credit only, degoogled removal. No trees in any form, no 3D bridges, no weather simulation, no AI landmarks, no conversational guidance, no satellite in car, no "more detailed maps overall". No "Altima" in any user-visible string (verified: 0 hits in changed files and in the published resources.arsc). Published resources.arsc "Vela" occurrences are only the About credit line, internal key names (mapvm_vela_voice_removed, settings_voice_install_vela) and the Theme.Vela symbol — the documented non-user-visible exceptions. No new keystore; repo visibility unchanged (public). Local-only artefacts excluded from the pushed tree: gradle.properties JDK-installations line reverted before pushing; run-gradle-here.py removed from the commit (kept untracked locally); core/libs + app/libs are git-ignored and were not committed.
VERIFICATION: local :app:compileDebugKotlin -PappId=app.automap green (after the repair above; warnings pre-existing, none in changed files). Full local assembleRelease not run; full APK verified from CI as above. No Robolectric harness exists in this tree, so no unit-test coverage is claimed for this build (the only unit-tested code touched is untouched: PaintedRegionsTest and SunAngle/updater suites are not in the changed set). Device verification owed, not claimed: in-app update alpha.14 -> alpha.15 without uninstall; no Vela visible anywhere except the About credit line; settings tree intact with the toggle move; car voice row shows the real mode and steps; search results show the sub-page path; disabled colour/arrow rows behave as described; car Saved shows recents; car settings show terrain + offline count; car search shows chips before the first search; painted markings still live in all five Phase D regions with no double-draw in the Cincinnati/Northern-KY overlap.
STILL OWED, NOT CLAIMED: device verification as above. NEXT PHASE: Phase E (AutoSpace companion round) — NOT started here, per the brief; it still waits on Robert making a new AutoSpace version.


## STANDING RULE (Robert 2026-10-28) — all future builds go through GitHub through a workflow
After alpha.15, ALL future builds must be done through GitHub through a workflow, like the other apps. The signed release workflow (.github/workflows/automap-release.yml, manual dispatch only) is the SOLE publishing route for alphas: no locally-built APK is published, no debug build is shipped as an alpha, and no APK is delivered as a chat attachment (standing rule since 2026-10-26). This build (alpha.15) used that workflow: CI run 40693860457, completed success, on work commit 08eb5e6f. This rule is recorded here at Robert's direction alongside the alpha.15 record; it does not change any code, version, or release.
QUEUED FIRST FOR THE NEXT BUILD (Robert 2026-10-28 ~08:49, arrived after alpha.15 was published, so it is NOT in alpha.15 and is not dropped): "Update section release notes before download" — in Settings > About > AutoMap updates, BEFORE the user downloads an update, show the target release's notes/changelog on the Updates page, above/before the Download button, fetched from the GitHub release body the updater already reads. Notes display must not trigger a download, must not change the update-check, verification (size + SHA-256), or install flow, and must not touch the upstream SelfUpdater (deleted in alpha.15, stays gone) or AutoSpace compatibility. If notes cannot be fetched, show a plain "Release notes unavailable" state, never a blank download button and never invented notes. FEASIBILITY, CHECKED AGAINST THE ALPHA.15 TREE (not built yet): AutoMapUpdater already fetches the release JSON (fetchAvailable) but release(json) does NOT read the "body" field and Release has no notes field — so the next build must add a notes field, read it, persist it in the saved-found JSON, and render it in the Available state above the Download button. GROUNDING CAVEAT the next builder must handle: the current workflow hardcodes a 221-character boilerplate paragraph as every release's --notes, so today's bodies carry NO changelog — displaying body alone would show boilerplate, not features. The next build must therefore ALSO change the workflow to write real per-build notes into the release body (e.g. the work commit message, which already carries the full summary) BEFORE dispatching, so the new release's body is real notes; the app change alone is not enough. Order in the next build: workflow change committed first, then dispatch, then verify the downloaded-back release body and the on-page notes match.
