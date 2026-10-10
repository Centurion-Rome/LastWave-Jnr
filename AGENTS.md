# AGENTS.md — LastWave-Jnr

Native Android music client (YouTube Music catalog). 100% Kotlin + Jetpack Compose (M3 Expressive), Media3 ExoPlayer, Hilt, Room, DataStore. Package `com.lastwave.app`.

## Modules & entrypoints

- `:app` — the whole app. Entrypoints: `LastWaveApplication.kt`, `MainActivity.kt` (nav host `LastWaveNavHost`, `Screen` routes), `playback/MusicPlaybackService`.
- `:audio:decent-usb-audio-driver` — bit-perfect USB driver, consumed as `implementation(project(...))`. Don't fold its logic into `:app`.
- Native DSP: `app/src/main/cpp/` (`AudioEngine`, `DspProcessor`, `NativeBridge`, `SecretsBridge`) → CMake lib `lastwave_audio`, Oboe 1.10 via Prefab + libsoxr 0.1.3 fetched at configure time (network required). 16 KB page alignment flags live in **both** `app/build.gradle.kts` (`cFlags`/`cppFlags`) and `cpp/CMakeLists.txt` — keep them in sync (Android 15+ requirement).
- Single-process app (no `android:process` in manifest). One writer thread / holder pattern is safe.

## Build / test commands

- Prereqs: JDK 17 (Temurin), Android SDK API 35, NDK 26+, CMake 3.22.1. On Windows use `.\gradlew.bat`; `sha256sum` doesn't exist there (use `Get-FileHash -Algorithm SHA256`).
- Unit tests: `./gradlew :app:testDebugUnitTest` (JUnit + Truth + Robolectric + MockK; `diagnostics/core` is pure JVM, no `android.*`).
- Release variants: `./gradlew assembleRelease assembleRawRelease` — `release` = R8 + shrink (most likely to catch reflection/string-resource errors); `rawRelease` = no minify. Copy each APK out of `app/build/outputs/apk/` **before** the next build overwrites it.
- Legacy APK: `./gradlew assembleRelease -PminSdk=24`. Default `minSdk` is 29 but CI ships an API-24 build — **all new code must run on API 24**; version-guard every API-gated call.
- CI (`.github/workflows/build.yml`): test → `assembleRelease assembleRawRelease` → `-PminSdk=24` build → `apksigner verify`. `release.yml` is manual-dispatch only and names APKs `*-universal(-uncompressed)/-android7`, while `build.yml` artifacts use `*-release/-raw/-android7` — check before renaming anything.
- Lint is non-blocking (`checkReleaseBuilds=false, abortOnError=false`); R8 failures are the real gate.

## Secrets & signing (gotchas)

- Secret resolution order in `app/build.gradle.kts:resolveSecret`: env → Gradle property → `local.properties` → `.env`. Local setup: `cp .env.example .env`. Keys: `ADDON_CLIENT_SECRET`, `RELEASE_CERT_SHA256`, `SIGNING_KEY` (base64 keystore), `KEY_STORE_PASSWORD`/`RELEASE_STORE_PASSWORD`, `ALIAS`, `KEY_PASSWORD`, `LASTWAVE_LYRICS_TOKEN/URL` (the only secrets allowed in `BuildConfig`/DEX).
- `tools/generate_native_secrets.py` runs automatically on `preBuild`/`configureCMake` and writes `app/src/main/cpp/SecretsBridge_generated.h` (auto-generated, do not hand-edit). Missing `ADDON_CLIENT_SECRET` only warns (empty lock) — never fail the build for it.
- Signing fallback trap: with no keystore configured, `release_config` silently does `initWith(debug)` — a local `assembleRelease` succeeds but is **debug-signed**. Never ship that as a release; real releases need the secrets above.

## Fork features — do not regress

This fork's two differentiators vs upstream. Any change touching playback resolution, player UI, or detail screens must leave these working.

- Analog VU meter: `ui/common/AnalogVuMeter.kt` (`AnalogVuMeter` + `PlaybackVuMeter` + `StickyVuMeter`). Real needle comes from `rememberRealBassLevel(isPlaying)` reading `playback/AudioLevelMonitor.levels` (PCM tap, 400 ms freshness); `level=null` = simulated 124 BPM groove fallback, needle rests when `!isPlaying`. Callers: `PlayerHost.kt` (2 sites), `PlaylistDetail/PlaylistScreen`, `Artist/AlbumDetailScreen`, `Discover/SearchScreen`. Load-bearing details: always wire `level = rememberRealBassLevel(isPlaying)`; keep `StickyVuMeter`'s opaque `containerColor` and overlay-below-top-bar placement (a `stickyHeader` pins to y=0 behind the status bar — do not "simplify" to it).
- Offline play of downloads: `playback/OfflinePlaybackResolver.kt` (pure Kotlin, JVM-testable) + resolution chain in `MusicPlayer` (~`findLocalDownloadStream`): Room lookup → local candidate URIs → MediaStore orphan probe → deep dir walk (skipped at startup via `deepScan=false`) → `TrackDownloadManager` naming probe; stale DB rows are deleted. `makeDownloadKey` (`"${artist}_${title}"` trimmed+lowercased) must stay in sync with `TrackDownloadManager.makeDownloadKey`; `toPlaybackUriString` normalization (`content://` passthrough, `/path` → `file://`) must stay intact. Never gate local-file playback on network availability.
- Verify: `./gradlew :app:testDebugUnitTest --tests "com.lastwave.app.playback.*"` plus `--tests "com.lastwave.app.data.download.*"`; resolver key/URI rules deserve a JVM unit test if touched. Manual gate: download a track → airplane mode → play from library/downloads must play the local file (only truly-absent tracks may show "Track not available offline").

## Conventions

- Logging migration in progress: use `AppLog.w/e` (file + logcat) for warn/error; leave `Log.d/i` as-is (logcat-only by design). Never do disk I/O on audio threads — enqueue only.
- New diagnostics-adjacent code goes in `diagnostics/core/` (pure JVM, unit-tested) + thin Android shell in `diagnostics/`; no new Gradle deps for it.
- New screen = `Screen` route + `NavGraph` registration + `SettingsSearchIndex` entry if discoverable, following the `HomeSectionsScreen` pattern (`PredictiveBackScreen`, `ExpressiveHeader`, shared `ui/common/ExpressiveGroup` rows). Keep composables stateless; state lives in ViewModels.
- Strings: add keys to `app/src/main/res/values/strings.xml` only — 12 locale dirs fall back to English; never hand-translate locale files.
- Deps: InnerTubeX must stay `runtimeOnly` (compatibility adapter; built with newer Kotlin metadata than the app). Core-library desugaring is required by the Jellyfin FFmpeg decoder — don't remove. Kotlin official style (`kotlin.code.style=official`).
- Branching: `feat/*`, `fix/*` off `main`; PR description must include problem analysis + testing steps (per `CONTRIBUTING.md`).
