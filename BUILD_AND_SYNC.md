# AutoMap — build and sync runbook

Slipstream of upstream Vela at commit `85eb69c` ("Reach snapshot for
2026-10-26"). Internal Kotlin namespace stays `app.vela.*`; the shipped
package is `app.automap` via Vela's `appId` build property.

## Building (this VM)

Prerequisites, all under `~/workspace/` (none of this is a source patch):

- Android SDK at `~/workspace/android-sdk` (platforms 34 + 37,
  build-tools 34/36/37, cmdline-tools, platform-tools, ndk).
- Temurin 21 at `~/workspace/jdk21` (JAVA_HOME) and Temurin 17 at
  `~/workspace/jdk17` (the `:osmand-shaded` toolchain). The system JDK's
  cacerts symlink is broken; do not use it.
- Gradle 9.8.0 at `~/workspace/gradle-dist/gradle-9.8.0` (the wrapper
  download fails through this VM's TLS-intercepting egress proxy; the
  binary is run directly).
- `~/workspace/automap/.gradle-home/cacerts-automap`: Temurin cacerts plus
  the Hatch Sandbox Egress CA, referenced from `JAVA_TOOL_OPTIONS`.
- `gradle.properties` carries one local-only line:
  `org.gradle.java.installations.paths=.../jdk17,.../jdk21`. It is NOT part
  of the patch stack and must not be committed as a source change.

Build (reads the egress proxy from the environment; never writes proxy
credentials to a file):

```
python3 ~/workspace/automap/build-logs/run-gradle.py :app:assembleDebug -PappId=app.automap
```

The runner sets `ANDROID_HOME`, `GRADLE_USER_HOME`
(`~/workspace/automap/.gradle-home`, isolated because a shared Gradle
journal lock blocked builds), `JAVA_HOME`, and `JAVA_TOOL_OPTIONS`
(`-Djava.net.preferIPv4Stack=true` — the Gradle daemon binds an
IPv6-mapped socket in this VM and clients cannot connect without it —
plus the trust store above). It prints `GRADLE_EXIT: <code>`; trust that
and the APK artifact, not log tail order (`gradle.log` is appended and
buffered out of order).

Verify every build from the built artifact, never from poll output:

```
# package, version, label
aapt2 dump badging app/build/outputs/apk/debug/app-debug.apk
# signature (debug builds; NOTE: the debug cert changed between sessions —
# builds 0–11: SHA-256 70aa9404cfd61c119d9f043719b8caf527fd2e10eb3dad485eb5979b3f124cc5;
# builds 13–18 + final: SHA-256 4754268cd20cd4b6cd918b85f83d22fc32e5cf8b69fd46bc8f60f42483f5d3d8.
# The final APK will NOT install over a builds 0–11 APK — uninstall first.)
apksigner verify --print-certs app/build/outputs/apk/debug/app-debug.apk
# feature markers in dex / resources.arsc, e.g. palette hexes, layer ids,
# setting strings — see BUILD_LOG.md per build for the exact list.
```

Runtime files the build needs but the repo gitignores (CI fetches the same
from upstream `PimpinPumpkin/Vela` releases `obf-runtime` / `cronet-runtime`
/ `tts-runtime`): `core/libs/osmand-java.jar`, `osmand-shared-jvm.jar`,
`gnu-trove-osmand.jar`, `kxml2-vela.jar`, `app/libs/cronet-*.aar`,
`app/libs/sherpa-onnx-*.aar`.

## Patch stack (GPL compliance)

- `automap-patch-stack.diff` (this directory) is the full source diff
  against upstream `85eb69c`: tracked modifications plus the new source
  files. It excludes `gradle.properties` (local build config, above),
  `BUILD_LOG.md` / `RELEASE_NOTES.md` / this runbook (docs), `build-logs/`
  (logs + APKs) and `.gradle-home/` (build state).
- The adaptive-icon foreground PNG is binary and cannot ride in the text
  diff: source is `~/workspace/vela-feasibility/automap-icon-ui.png`
  (Robert-approved 2026-10-26), shipped in the APK as
  `res/drawable-nodpi-v4/ic_launcher_foreground.png` (624,866 B).
- The bake pipeline is upstream `scripts/bake-painted-roads.sh`
  (osmium + the `:core:testDebugUnitTest '*PaintedRoadsBakeTest'` bake +
  tippecanoe); the stage-1 technical pilot + coverage report live in
  `build-logs/bake-pilot/` (NOT bundled into any APK).
- This worktree's git remote points at the local feasibility clone, not a
  public repo. Publishing the stack to a public repo (and the release
  keystore, below) awaits a repo decision — nothing has been pushed
  anywhere.

## Weekly sync (after every upstream weekly stable)

1. Fetch upstream; check out the new weekly stable in a scratch clone.
2. Re-apply this patch stack in build order (0–18, BUILD_LOG.md order).
   A conflicting patch is fixed in the open, never silently dropped.
3. Run the regression matrix below. Ship only if it passes, with no
   feature changes in a sync build.
4. A regressed feature blocks the sync until fixed or explicitly reverted
   by Robert.
5. If the patch stack stops rebasing cleanly for two syncs in a row, or
   upstream is archived, drops car support, or changes licence, the agreed
   fallback is a hard fork — that decision is Robert's, not automatic.

## Regression matrix (Build 19)

Code-level (done in this VM, Build 19):

- [x] Full `:app:assembleDebug -PappId=app.automap` green; final APK
      verified (package, version, label, signature, 22 feature markers,
      icon bytes) — see BUILD_LOG.md.
- [x] Full `:core:testDebugUnitTest` green (see build19-coretests.log).
- [x] Changed-file audit: only `app/vela/ui`, `app/vela/car`,
      `app/vela/ui/map`, `app/vela/ui/settings`, `res/`, and new
      `core/.../SunAngle` files — no routing, traffic, places, search,
      voice-publishing or backup code touched.
- [x] Rename audit: base `strings.xml` contains 0 "Vela"; no tree feature
      added anywhere (the only "trees" hit is the icon comment itself).
- [x] AutoSpace companion entry for `app.automap` still present
      (uncommitted in the AutoSpace worktree; RecommendedApps entry still
      owed — no published release URL exists yet).

Device-level (OWED — this VM has no paired device; do not claim these):

- [ ] Screenshots, dense-city / highway / interchange routes, day + night,
      2D + 3D, vs the Build 0 baseline.
- [ ] Drive tests per build checklist (puck, declutter, signs, fade,
      auto-zoom, markings, arrival, hillshade, sun shading).
- [ ] Snapshot timing / memory / legibility vs Build 0 baseline.
- [ ] Offline run, backup/restore to a second install, AutoSpace turn
      publishing ("navigate with AutoMap").

## Release signing (not done — debug only)

No keystore has been generated. Before any published release: generate a
fresh keystore under `~/workspace/automap/` (never in a repo), record its
fingerprint and location, back it up somewhere durable (losing it means
no future release can install over the first one), and only then sign.
Until then, builds are debug-signed and labelled as such.
