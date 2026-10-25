# Upstream sync — 2026-10-25

Merge `upstream/main` at `03933939` into Jnr `ba3a589e` on
`fix/upstream-sync-2026-10-25`. No version bump, publication or update to `main`.

## Conflict resolutions

- `SettingsPreferences.kt`: keep persistent playlist reorder-lock preferences
  alongside upstream's YouTube Music home-section preferences.
- `ArtistDetailScreen.kt`, `NavGraph.kt`: normalize line endings for the three-way
  merge; retain Jnr's inline/pinned VU overlays and offline launch routing while
  incorporating upstream's localization, settings routes and Jam navigation.
- `FeedScreen.kt`: retain Jnr's Discover Mix/New Release tile mapping, subject to
  upstream's synthetic-section visibility condition.
- `PlayerHost.kt`: keep all three real-level VU placements and fullscreen VU;
  adopt upstream's quality badge, audio-controls action and updated layout.
- `PlaylistDetailScreen.kt`: combine upstream's remembered sort with Jnr's
  persistent reorder lock, stable-key drag behavior and pinned VU. Consolidate
  the independently added locate-song buttons into one; both locate actions
  account for the hero, VU and optional reorder-hint rows.
- `PlaylistScreen.kt`: keep the real-level VU above upstream's playlist search
  and generator controls.
- `SettingsScreen.kt`: use upstream's dedicated Spotify tab and account/session
  controls; retain Jnr source URL, version fallback and neutral About credit.
- `MainShell.kt`: combine network/offline Downloads startup and banner with
  upstream's YouTube connection/settings state, Feed-tap behavior and generator
  navigation. Retain the Downloads tab.

## Preservation checks

An assert-based Python check confirmed that 13 fork-critical build, workflow,
DSP/PCM, offline resolver, download and VU files are byte-for-byte unchanged from
`ba3a589e`. All 46 app/build/workflow files with substantive fork changes still
differ from upstream. Source checks confirmed all six direct real-level VU
bindings across player/library/discover/search and both inline/pinned VU pairs
in artist/playlist detail screens. All 14 locale/default strings XML files parse;
changed files contain no conflict markers.

`git -c core.whitespace=cr-at-eol diff --check ba3a589` passes. The unconfigured
`git diff --check ba3a589` reports CRLF-as-trailing-whitespace on 12 incoming files;
these are upstream line endings, not whitespace added by conflict resolution.

## Validation

- `.\gradlew.bat :app:testDebugUnitTest`: first invocation hit the tool's
  120-second timeout during Kotlin processing. Rerun with a 30-minute limit
  succeeded in 6m 38s (48 tasks: 15 executed, 33 up-to-date).
  JUnit XML: **665 tests, 90 classes, zero failures/errors/skips**. This includes
  playback (162 tests/19 classes), downloads (17 tests/4 classes), and all 14 new
  upstream test files (99 tests); the existing theme test changes also ran.
- `.\gradlew.bat :app:assembleDebug -PminSdk=24`: **BUILD SUCCESSFUL** in 3m 1s
  (80 tasks: 36 executed, 44 up-to-date), including native builds for all ABIs.
- Android SDK `aapt dump badging app\build\outputs\apk\debug\app-debug.apk`:
  package `com.lastwave.app`, label `LastWave-Jnr`, version `4.3.2`/32,
  minimum SDK **24**, target SDK 35.
- API-24 debug APK SHA-256 (Python `hashlib`; this shell lacks `Get-FileHash`):
  `2da9094b9f244da9abbb19b1b6a3af8af127d7c37b94cf2cbf92581f6f8de5d8`.

Logs remain outside the repository under
`C:\Users\Admin\AppData\Local\Temp\opencode\`:
`upstream-tests.log`, `upstream-tests-retry.log`, `upstream-api24.log` and
`upstream-whitespace.txt`. Existing Gradle/Kotlin deprecations and CMake/native
warnings did not fail either successful build. Missing addon secret warned as
designed; optional addon authentication cannot be exercised with this debug APK.

Release signing key/store-password inputs are absent from environment/local
configuration; release/R8/signature validation is not available in this setup.
Manual device gates remain: download a song, enable airplane mode, play it from
Downloads/library; inspect real PCM-driven VU needles, sticky overlays, fullscreen
VU and tablet landscape; check playlist locating/reordering and the new upstream
player, home, Jam and video flows. An API-24 compile cannot replace an API-24
device smoke test.
