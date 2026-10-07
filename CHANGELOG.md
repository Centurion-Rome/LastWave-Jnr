# Changelog

## [4.2.7] - 2026-10-07

Fifth upstream sync from `Clash-Projects/LastWave-Native`, merging 72 commits
(`e3bec2be`..`55b6bb89`) on top of the v4.2.6 fork point. **72 upstream
commits, 11 conflicted files resolved.**

### Added
- **Discord Rich Presence** (upstream 4425d05, 7c54ca9). Shows what's playing
  as Discord Rich Presence via pure AIDL IPC.
- **Quick Access feed section** (upstream 64599b8). Ported quick access section
  options, styling, and navigation.
- **Stretchy artwork fade** (upstream bef22b9, 5a7fb6c). Responsive stretchy
  artwork fade on Now Playing full bleed view.
- **Audition stream hot-swap** (upstream d07518b). Seamless audition-backed
  mid-track hot-swap.
- **Lyricify Apple Music lyrics provider** (upstream 74311df, 8efb5cf). Integrated
  with CI secrets.
- **Kugou KRC lyrics sync** (upstream 29b7203). Restored Kugou KRC sync with
  offset parsing, credit filtering, and whitespace token handling.

### Changed
- **Version bump** to 4.2.7 (versionCode 27).

### Fixed
- **Dolby Atmos playback** (upstream 911ae24, 21ca44d, d74f8f7). Restored Dolby
  Atmos stream selection and decode capability detection.
- **FLAC bit depth/rate display** (upstream c554d2b, 6339779). Retain full bit
  depth and clock rate from addons on quality pill.
- **Lyrics sync stability** (upstream b1abfaa, 52ce223, c6329da). 120fps lyrics
  vsync rendering, lossless candidate matching, and playback stability.
- **CSV import** (upstream def707d). Fixed CSV import parsing and prevented YouTube
  song mismatches.
- **DASH manifest parser** (upstream 0318e7c). Resilient XML attribute matching,
  case-insensitive tag handling, and strict URL scheme checks.
- **YouTube sign-in** (upstream 9f8ef7f). Hardened with cookie re-read backoff.
- **Seekbar and lyrics** (upstream ac19d06, 97c68d5). Restored FLAC bit depth/rate
  pill on all devices via STREAMINFO fallback; preserved 24-bit quality badge and
  resolved seekbar stalls.
- **Static cover art** (upstream e3bec2b). Prevented static cover art from bleeding
  through translucent canvas.
- **Feed author nullability** (upstream 0d642ea). Fixed null author handling.
- **Quick Access order/icons** (upstream 63fbf24). Fixed Quick Access order/icons,
  My Mix playback, lyrics native FPS.

### Notes
- **Conflict surface was 11 files.** All resolved to keep the fork's version (4.2.6→4.2.7)
  and Jnr branding while accepting upstream's new features and fixes.
- The fork's VU meter, DOWNLOADS tab, branding and reorder-lock all survived.

## [4.2.6] - 2026-10-03

Fourth upstream sync from `Clash-Projects/LastWave-Native`, in two merges
(`9efe06f` = 14 commits, `12492ba`..`cfff4ff8`; `112651f` = 7 commits,
`cfff4ff8`..`1d67649c`) on top of the v4.2.5 fork point. **21 upstream
commits, 66 files, +5836/-978.**

### Added
- **Crash and operational logging, split into two channels (#213, upstream
  b76608f).** Replaces the 382 raw `android.util.Log` calls with a pure-JVM
  core (`diagnostics/core/`: formatting, rotation, retention, drop accounting,
  crash lead-up ring) behind a thin Android shell. The crash channel writes
  one self-contained file per crash, newest 10 retained, covering uncaught
  exceptions, ANRs (5s main-looper ping that captures the main thread stack
  *before* Android writes its own trace), native crashes / low-memory kills /
  system-recorded ANRs via `ApplicationExitInfo` (API 30+, deduped), and
  unclean terminations via a running/clean session marker — each with version,
  device, memory, storage, thread, stack and the 40 log lines preceding the
  failure. The old crash log truncated at 128KB and destroyed all history; the
  operational channel now rotates at 256KB across 5 files (1.25MB ceiling),
  persisting errors and warnings always but debug/info only in debuggable
  builds or with verbose logging on. Every record is a non-blocking enqueue on
  a bounded queue drained by one writer thread, so nothing on an audio callback
  waits on disk; overflow drops the *newest* record to preserve the lead-up and
  reports the loss instead of silently truncating. `CrashGuard` is deleted.
- **Multi-select playlist deletion and playlist sorting** (upstream b27e85d,
  e009c79). Long-press selects, the header reports `N selected`, delete
  cascades to the YouTube playlists and local rows, and a sort menu offers
  Newest/Oldest first, Name and Track count.
- **Draggable sleep-timer overlay** (upstream c001db0), plus a playlist
  operation toast.

### Changed
- **Cover-art resolution enforces a strict provider priority: Apple Music →
  Tidal → Deezer → Spotify → YouTube** (upstream 6640da7, corrected by 71681e6).
  Nothing is accepted on a similarity score alone — a candidate's own title and
  artist must agree with what was requested. `ArtworkNormalizer` grew the
  normalization rules this needs, `ITunesArtworkProvider` was corrected, and
  `ArtworkImage` now distinguishes a real cover from a fallback.
- **Crossfade now survives lossless** (upstream eb9297a). It was never a
  crossfade bug: the hand-off needs the standby player at `STATE_READY` and
  nothing arranged that for provider-module FLAC. Arming is decoupled from the
  fade window (the standby prepares seconds into the track instead of ~10s
  before the hand-off), signed module URLs are refreshed ~30s ahead of expiry
  instead of being discovered dead, and background preloads get an unbounded
  resolve budget via a new `LosslessBudget` enum while interactive playback
  keeps its 3.5-12s cap. Overlap is bounded by `standbyBufferedPosition`, so a
  slow link yields a short complete fade rather than a 12s fade that dies at
  second 4. `onDecodedPcmFormatConfigured` is gated on `handleAudioFocus` so
  only the live player publishes into the pill. Policy extracted to
  `CrossfadeSchedule.planCrossfade`, mirroring the `SignalPath.kt` pattern.
- **Widget layouts** (square, expanded and the fork's obsidian square) resized
  and aligned to the Kittytune reference (upstream b27e85d, 5b709df).
- **Settings regrouped** into `Library & Content` alongside the existing
  groups, now reporting the live download count (upstream 5b709df).
- Quick-access tiles gain distinct Discover / New Releases / Mix gradients and
  icons instead of one shared treatment (upstream 5b709df).

### Fixed
- **App missing from Settings → Special app access → Notification access.**
  Commit `cff12ddc` had deleted `MediaScrobbleListenerService` (the
  `NotificationListenerService`) along with its manifest declaration, and
  `4e1bd34d` removed `ScrobbleDebugLog` it injects — so the merged APK
  contained no listener service and Android hid the app from Notification
  Access entirely, breaking media-session scrobbling. The service
  declaration (`BIND_NOTIFICATION_LISTENER_SERVICE` +
  `android.service.notification.NotificationListenerService` intent
  filter) and both classes are restored byte-identical to upstream
  `release/4.1.1`; no scrobbling logic was changed.
- **Songs skipping out 3-4 seconds into a track** (upstream 0d53ccd). This was
  the most-reported symptom in the batch. `effectiveDuration` accepted the
  previously published duration as its last fallback, so on a track whose
  container had not parsed yet — `C.TIME_UNSET`, routine for YouTube
  WebM/MP4 and provider-module FLAC — the incoming track inherited the
  outgoing one's length. `updateCrossfade` then crossed the fade threshold
  seconds in and swapped the *next* item in, abandoning the rest. The
  duration's owning item is now tracked and cross-track reuse is refused
  outright (reported as 0 — no denominator beats a wrong one), while
  everything keyed to the item in hand is still trusted across a transition so
  a mid-track rebuffer cannot freeze the bar at 0:00. Policy extracted to
  `DurationSelection.selectDuration`.
- **Seek bar advancing through a stall or a transition** ("playing while
  buffering"). `advancePlayhead` free-ran on the wall clock whenever ExoPlayer
  was IDLE or still held the previous item, because `_state.isPlaying` is
  deliberately held true across resolve windows to keep the wake lock alive.
  The playhead now freezes when nothing is rendering; USB-exclusive output
  keeps its own stream clock.
- **24-bit Hi-Res badge clobbered back to 16-bit** (upstream ecd20b4). The
  queue preloader and the format listeners were still overwriting the badge
  after the 4.2.5 depth fix — the same class of bug as the one fixed there,
  one layer out.
- **Seekbar freeze and lyrics sync instability** (upstream e9cf372, kept by
  5e17598).
- **CSV import failing in six independent ways, each costing songs** (upstream
  1d67649). A single unclosed quote threw out of the parser and discarded every
  row in the file; links were never checked against their row, so a stale link
  imported whatever it pointed at; the fuzzy fallback took a single unchecked
  60%-similar pick, substituting live cuts and remixes for the requested song;
  any bare 11-character token in *any* column was treated as a video id;
  title-only rows could never match, so a plain list of song names imported
  nothing; a one-column file headed "Song" lost that song to header detection;
  and delimiter ties broke toward a comma, shifting every column of a tab- or
  semicolon-separated file containing a comma. Transport errors were
  indistinguishable from "no match" (`searchSongs` folds them into an empty
  list), so rows now retry with backoff and walk several query shapes,
  validating every candidate rather than the top hit. A bad row now costs a
  skipped entry instead of the wrong song.
- **Near-mute and no-signal output on universal bit-perfect DACs** (upstream
  c00c2ba). UAC 2.0 clock topology is now traversed properly
  (AS_GENERAL → Terminal → Multiplier → Selector → Clock Source) for
  dual-oscillator DACs; the `AudioStreaming` playback interface is isolated by
  verifying isochronous OUT endpoints, which stops headset mic capture; the
  64-sample silence heuristic in 24/32-bit packing is replaced by a full-buffer
  scan (that heuristic was the source of the −48dB near-mute); the xHCI
  pipeline is primed with silence URBs and synchronized with `CLOCK_VALID`/PLL
  settling; explicit hardware unmute is issued on attach with a guard against
  Android's USB-detach 0-volume transient; and `ExclusiveUsbWriter` runs at
  `THREAD_PRIORITY_URGENT_AUDIO`.
- **Static cover art stacked under the animated canvas** (upstream df11b4b).
  The full-bleed hero composed `ArtworkImage` unconditionally and drew
  `CanvasArtworkPlayer` on top, so the still cover stayed visible under motion
  artwork. The canvas is now the artwork: the cover is a loading/error
  placeholder crossfaded out and dropped from the tree once a real frame
  arrives. The hero melt seam was separate — `heroPx` clamps at
  `min(w*1.08, h*0.62)`, so on a tall screen the hero bottom landed past the
  fixed `0.62` foundation ramp; the ramp is now keyed to `heroPx`. Motion
  artwork also no longer renders with the Animated Album Canvas toggle off.
- **Navigation tab labels ellipsized** in the bottom dock (upstream b27e85d,
  plus the `TextOverflow` import fix in 00aa9e0).

### Removed
- **The "Get Addons" Telegram card and its `Don't have an addon?` link** in
  module settings (upstream e9cf372, undoing their own 92e3922). The Support
  section in `SettingsScreen.kt` still links the channel, so the path is not
  lost. This was the only overlap with fork content in the whole sync — nothing
  was dropped from the Jnr side.

### Notes
- **Conflict surface was almost nil.** Only `MusicPlayer.kt` and `PlayerHost.kt`
  were touched by both sides and both auto-merged, with zero manual
  intervention across both merges. The fork's VU meter, DOWNLOADS tab,
  branding and reorder-lock all survived; `versionCode 25 / 4.2.5` unchanged.
- **`:app:testDebugUnitTest` is green** (3m52s) with the new tests from this
  batch: `ArtworkMatchingTest`, `CrossfadeScheduleTest`,
  `DurationSelectionTest`, `CsvPlaylistImporterTest`, and five
  `diagnostics/core/` suites (`LogStoreTest`, `LogQueueTest`,
  `LogFormatterTest`, `LogTailTest`, `RetentionTest`).
- **The cover-art DstIn dissolve landed and was then reverted.** `bae2ab7`
  dissolved the full-bleed cover via an offscreen `DST_IN` blend mask; `5e17598`
  reverted it while keeping the seekbar fix from the same PR. It is the same
  save-stack underflow that killed the process in 4.2.5 — do not reintroduce
  a second blend pass over the hero.
- **Upstream's history was not rewritten this time**, so no `git replace
  --graft` workaround was needed; `merge-base` is `12492ba` as expected.
- The stale `## Unreleased` section further down this file is upstream's
  September 16 author-transition note and shipped in 4.1.0. Left untouched.

## [4.2.5] - 2026-10-01

Third upstream sync from `Clash-Projects/LastWave-Native`, merging 10 commits
(`a0d60e1`..`12492ba`) on top of the Jnr fork. Merge base was `a0d60e1` — no
history rewrite this time, so no graft workaround was needed and all seven
overlapping files auto-merged.

### Added
- **Resizable lyrics font scale** with live controls and persistence (upstream
  bb0b246). Includes a new `LyricsSizeDialog` and a `FastBlur` fast-path.
- **Option to disable the rotating background** in the player (upstream f77be2a).

### Fixed
- **The quality pill no longer fabricates bit depth** (upstream 92c76ad, c3f42d2).
  Two causes: the decoder's *output* width (`pcmEncoding`, only ever
  `ENCODING_PCM_16BIT` or `ENCODING_PCM_FLOAT` in the bundled media3-ffmpeg
  1.2.1) was written into `MusicPlayerState.bitDepth`, so every 24-bit track
  measured 16 once the sink configured; and the fallback guessed depth from
  bitrate, which relabelled ordinary 24-bit FLAC as 16. An unknown depth is now
  shown as unknown, all sources default to null instead of 16, and one shared
  `resolveDepthForDisplay` rule drives the pill, the detailed badge, the track
  details sheet and the Signal Path row so they cannot disagree. A reported 16
  beside a hi-res rate is corrected to 24.
- **Player crash from the artwork bottom fade** — the Compose `BlendMode.DstIn`
  rect plus the `createBlendModeEffect` on the motion-artwork `TextureView`
  underflowed the canvas save stack ("Underflow in restore") and killed the
  process. Both masks are gone; the fade is a single source-over pass at every
  API level, and `FadingBottomFrame` hands the save stack back via
  `restoreToCount` when a driver refuses the layer (upstream 6fd20cd).
- **Queue state on `playNext` / `addToQueue`** — the queue is now synchronized,
  the next track is preloaded, and session state is handled rather than dropped
  (upstream 9123c0d).
- **Downloads hardened across all tiers** — cooldown skip, Atmos stereo
  fallback, DASH truncation now fails loudly, YouTube budget capping, and honest
  completion reporting (upstream ec11a43).
- **YouTube sync no longer pushes unadded songs** into user playlists
  (upstream 43a78f5).
- **Clearing data now stops playback and removes downloads**, instead of leaving
  a playing queue and orphaned files (upstream 12492ba).
- **Launcher and splash icon foreground** is scaled into the 66dp circular safe
  zone so it no longer clips on circular launchers or during the Android 12+
  splash animation; fourth waveform bar thickness normalized (upstream d29835e).

### Notes
- **Conflict surface was nil.** All seven overlapping files
  (`TrackDownloadManager`, `SettingsPreferences`, `SegmentedDashBridge`,
  `MusicPlayer`, `PlayerHost`, `SettingsScreen`, `strings.xml`) auto-merged, and
  the fork's own YT download resolve path from `a0d60e1` came through intact.
- **Quality badge needs a device pass.** The bit-depth fix touches both
  `MusicPlayerState.bitDepth` writers and lands alongside the 4.2.4
  stream-lock change, which auto-merged into the same ~300 lines of
  `MusicPlayer.kt` without conflict. `d29835e` also had to fix an
  uncompilable `resolvedBitDepth.takeIf { it > 0 }` on an `Int?` left by
  `c3f42d2`, so this area was churning across three commits.
- **`resolveDepthForDisplay` corrects 16 to 24 above 48kHz.** That is a
  deliberate inference, not a measurement — genuine 16-bit content at 88.2kHz+
  will display as 24-bit.
- `origin/feat/export-diagnostics` was deleted on the remote during the fetch.

## [4.2.4] - 2026-09-29

Second upstream sync from `Clash-Projects/LastWave-Native`, merging 14 commits
(`21edf34`..`a0d60e1`) on top of the Jnr fork's 352 local commits.

### Added
- **Widget redesign:** the now-playing widget was restyled to match the reference
  design while preserving existing functionality (upstream 496b60c).
- **Liked-songs healing sync:** stronger song identity, a locked YouTube-liked
  merge, and self-healing sync so liked tracks no longer drift out of the library
  (upstream e19fe40).
- **Full-bleed cover melt:** a seamless cover transition in the player with a
  square-capped hero (upstream 22042f4).
- **Settings search cleanup:** the scrobbler entries were removed from search and
  the search index now covers themes (upstream d87e652, 1bde560).

### Changed
- **Stream format is now locked** for the duration of a track, and **mid-track
  hot-swap was removed** (upstream 2ad7ec1). This is a deliberate reversal of the
  background lossless hot-swap introduced in 4.2.3 — see "Notes" below.
- **Spatial-to-stereo fallback** now cascades, so lossless playback degrades
  gracefully instead of failing, with a tighter end-of-queue resume guard
  (upstream 3ea8a70).
- **Downloads reuse the playback YouTube resolve path** with a broad search, so
  songs that are playable can actually be downloaded (upstream a0d60e1).
- **YouTube and lossless playback** resolve in parallel, starting with no delay
  (upstream 21edf34, carried from 4.2.3).

### Fixed
- **Number regex in the download manager** no longer breaks DASH `$Number$`
  segment addressing (upstream 2ad7ec1).
- Build fixes across `NavGraph`, imports, and `ScrobbleDebugLog` removal
  (upstream 391762e, 41837f5, f4ae961, d87e652).

### Removed
- **Mid-track hot-swap** and the leftover app queries / scrobbler debug log, the
  latter to reduce Play Protect warnings (upstream 2ad7ec1, 4e1bd34).
- `ScrobbleDebugLog.kt` (upstream 41837f5).

### Notes
- **Conflict resolutions favour the fork.** Four conflicts were resolved to the
  local `main` side: the `TrackDownloadManager` DASH `$Number$` regex comment, the
  `PlayerHost` create-playlist button styling, and both `PlaylistDetailScreen`
  hunks (the "locate currently playing" button and the track-index width).
- **Playback needs a real test pass.** `2ad7ec1` reverses the hot-swap design that
  4.2.3's racing work was built on, and it auto-merged into `MusicPlayer.kt`
  without conflict — so git raised no warning despite the ~260-line semantic
  change. This is the highest-risk area of the release.
- **`reference zip/` was excluded.** Upstream commit `496b60c` accidentally
  committed an extracted copy of the entire project (145 files, ~16.4k lines).
  That path was not imported, and the `reference screenshot` image the same
  commit deleted was restored.
- **Play Protect:** 4.2.3 told users to remove and re-add the widget. Since the
  widget was redesigned again here, expect the same one-time re-add.

## [4.2.3] - 2026-09-28

A large sync with upstream `Clash-Projects/LastWave-Native` (36 commits, `21edf34`),
plus the Jnr fork's own features. Upstream force-pushed a full history rewrite, so
this release merged against a reconstructed fork point (`82c8e8b`, grafted) rather
than a normal fast-forward. See the merge commit `e383162` for the full breakdown.

### Added
- **Instant playback:** YouTube and lossless streams resolve in parallel, so playback
  begins with no waiting; if lossless resolves later it hot-swaps in the background
  without losing the start time (upstream 21edf34).
- **Settings search:** every setting is indexed and fuzzy-searchable from the settings
  screen (upstream 5e7c243).
- **Animated album canvas:** motion artwork for Apple Music, Tidal and community
  canvases, now full-bleed 9:16 edge-to-edge with a hero fade (upstream 31230ec, b70c9fe).
- **Obsidian glass widget:** a single adaptive now-playing widget rebuilt on classic
  `RemoteViews` (upstream 58bb697, 6cd4050, 702324f).
- **System audio effects mode** (Experimental, default OFF) publishes the audio session
  to external equalizer apps and OEM Dolby, and a settings reorganisation
  (upstream 815bead).
- **Resizable lyrics** and a dedicated lyrics sync-offset dialog (upstream 4265a83, 5687220).
- **Quick Tiles:** Discover Mix sits at the end of the grid before New Releases and is
  clickable; the Radio tile now plays infinite radio instead of opening Discover
  (upstream branch d0db614, 4baf6c7).
- **Liquid-glass dialog blur:** blur radius and scrim dim follow the liquid glass setting
  — a wider, softer blur (150 / 0.18) with glass on, the tighter 120 / 0.28 with it off.

### Changed
- **Analog VU meter** retained across the player, queue, discover, search, playlist,
  album and artist screens, with the real-bass PCM tap still wired through
  `NativePcmAudioProcessor`.
- **Offline playback recovery:** orphaned downloads are found via MediaStore after a
  reinstall, with a `READ_MEDIA_AUDIO` runtime request on Android 13+.
- **Downloads tab** opens automatically when the device starts without connectivity.
- **Playlist reorder lock** is persisted per playlist, reconciled with upstream's new
  session-scoped lock toggle and playlist search.
- **Profile avatar** in the feed header gained a 2dp primary border ring.
- **Downloads header action** label is now "Downloads & Offline Music".

### Removed
- **External scrobbler:** `MediaScrobbleListenerService` and the ScrobblerApps /
  ScrobblerDebugLog screens were removed upstream. Last.fm still backs the taste
  profile, mixes and discovery — only third-party scrobble forwarding is gone.
- **Large Now Playing widget** and the Jetpack Glance widget internals
  (`InMemoryWidgetState` and its WorkManager proguard keeps), both superseded by the
  new widget implementation.

### Fixed
- **Transient playback errors** hold the track instead of auto-skipping on 403/throttle,
  and tail-pinned tracks advance (upstream cfd542c, 45e4098).
- **YouTube login** no longer reports a false "session rejected" on slow devices
  (upstream bf74b74).
- **Lossless honesty:** prefers 24-bit masters over parking at 16-bit, with
  sample-rate-driven bit-depth inference across resolve, quality pill, signal path and
  track details. Atmos falls back to Hi-Res → Lossless → 320k, and stereo tiers never
  upscale to Dolby (upstream 8719d1a, 0945d1b, 4333d77).
- **Lyrics pipeline:** backing-vocal rows, phrase grouping, faster scroll chase and
  corrected sync-offset guidance (upstream d9e02cd, da3d33d, e848f25).
- **Playlist duplication races,** auto-suffixing of same-title tracks and monotonic IDs
  (upstream 815bead).
- **Notification stability** during playback (upstream fc25a41).
- **Playlist drag reorder** keeps the fork's key-based lookup against upstream's
  `displayKeys` rename, alongside upstream's `runCatching` guards at the viewport edge.
- **DASH segment substitution did not compile** on upstream: `Regex("""\$Number…""")`
  in a raw string reads `$Number` as a template on `kotlin.Number`. Fixed locally with
  `${'$'}`; not yet reported upstream.

### Notes
- **The home-screen widget was rebuilt from scratch**, moving off Jetpack Glance onto
  classic `RemoteViews`, changing size, and removing the large Now Playing widget.
  Users should **remove and re-add the widget** after updating.
- A stray 0-byte file committed upstream at
  `app/src/main/java/com/lastwave/app/data/addon/sedXzeBMg` was deleted.
- The README screenshots still show the Jnr branding and VU meter; they have not been
  re-captured since the widget revamp and should be refreshed.
## [4.2.4] - 2026-10-05

### Added & Improved
- **Resource Usage & Smooth UI:** Optimized rendering pipelines, reduced background resource contention, and smoothed UI interactions.
- **Fast YouTube Search:** Restored instant 2-pass YouTube search resolution, eliminating multi-stage search fallback delays.
- **Dolby Atmos Downloads:** Aligned spatial audio capability detection in `TrackDownloadManager` with `MusicPlayer`'s system `Spatializer` check, resolving Atmos download skips on Android 12L+ devices.
- **DASH Manifest Parser Robustness:** Added resilient XML attribute matching, case-insensitive tag handling, and strict URL scheme checks to prevent malformed segment URLs during segmented downloads.
- **Playback Warmup & Retry Gating:** Implemented first-song OPUS warmup and tap-to-retry gating to prevent premature auto-skips on transient stream hiccups.

### Changed & Fixed
- **Bit-Perfect Audio Issues:** Resolved bit-perfect DAC routing, format lockups, and hardware sample rate mismatches.
- **Lyrics Sync:** Stabilized lyrics synchronization, eliminated drift, and improved word-by-word timing alignment.
- **Stretchy Cover Arts:** Fixed stretchy full-bleed cover art visual artifacts, canvas fade transitions, and fluid backdrop rendering.
- **Version Bump:** Bumped version to 4.2.4 (versionCode 24).

## [4.2.3] - 2026-10-03

### Added
- **Stretchy Artwork Fade:** Implemented responsive stretchy artwork fade on Now Playing full bleed view, vertically stretching and smoothly dissolving into the dark backdrop with an offscreen gradient mask and dark scrim.
- **Addon Bit Depth & Clock Rate Extraction:** Parsed bit depth and clock rate metadata across camelCase and snake_case properties from addons, preserving source-reported 24-bit depth on standard 44.1/48kHz tracks.

### Changed & Fixed
- **Settings Reorganization:** Relocated System Audio Effects from Appearance to Audio → Output & Loudness right beside Equalizer.
- **Version Bump:** Bumped version to 4.2.3 (versionCode 23).
>>>>>>> upstream/main

## [4.2.2] - 2026-09-27

### Added
- **System Audio Effects mode (Experimental, default OFF):** publishes the audio session for external equalizer apps and OEM Dolby, flattens in-app DSP airtight on mixer routes, auto-suspends on bit-perfect / USB exclusive.
- **Diagnostics capture:** crash-guard log and startup trail included in the diagnostics export; startup stage breadcrumbs for instant-kill diagnosis.
- **Settings search (fuzzy):** `FuzzyMatcher` + `SettingsSearchIndex` index every setting for quick navigation.
- **Animated album canvas:** Apple Music, Tidal and community canvas motion artwork in the player.
- **Resizable lyrics** and a lyrics sync-offset dialog.
- **Obsidian glass widget:** single adaptive now-playing widget rebuilt on classic `RemoteViews`.

### Changed
- **Merged upstream 4.2.1:** Pulled in `Clash-Projects/LastWave-Native` v4.2.1 - true bit-perfect USB audio pipeline, dual-crystal UAC2 clock switching with smart sample-rate fallback, fluid artwork background, Large Now Playing widget, addon system with native request signing, and five new lyrics providers with TTML syllable parsing.
- **Analog VU Meter:** Backlight gradient flipped to a vintage lamp-lit face - deep amber at the top of the dial washing out to pale yellow at the bottom, with the shading overlay moved to the top so the bright lower half stays luminous.
- **Settings organization:** Equalizer and Studio Clarity moved to Audio → Output & Loudness, liquid glass to Appearance, Experimental reindexed.
- **External scrobbler removed:** the `MediaScrobbleListenerService` and its settings screens were dropped upstream. Last.fm still drives taste profile and discovery.
- **Lyrics providers:** dropped the dead LyricsPlus provider; BiniLyrics/Musixmatch hardened with a karaoke line splitter and smarter matching.

### Fixed
- **Playlist drag reorder:** Upstream renamed the track key list to `displayKeys` (now aligned to display order), which broke the fork's key-based reorder lookup and broke the build. Drag now keeps the key-based lookup *and* upstream's `runCatching` guards around `layoutInfo` reads at the viewport edge.
- **Playlist duplication races:** serialized add/remove, auto-suffix same-title siblings, monotonic ids.
- **Atmos honesty:** JOC-only capability check plus spatial-manifest veto with step-down cascade to stereo / Opus. Atmos downloads fall back down; stereo tiers never upscale to Dolby.
- **Hi-res reporting:** sample-rate driven 24-bit depth inference across resolve, quality pill, signal path, and track details.
- **Lyrics readability:** lyrics-tab blur boost plus readability veil; adaptive transport row no longer clips Next on small screens.
- **Transient playback errors:** the player holds instead of auto-skipping on 403/throttle, and tail-pinned tracks advance.
- **YouTube login:** no longer reports a false rejected session on slow devices.
- **Lossless matching:** prefers 24-bit masters instead of parking at 16-bit.
- **Build:** missing YouTubeMusicTrack import, composable-safe backdrop guard, public DSP constants, restored native library build.

### Notes
- Upstream removed the `USB_DEVICE_ATTACHED` intent-filter from the main activity, so the app no longer auto-claims a USB DAC on plug-in.
- The dead native key getters were removed upstream (`NativeSecrets.baseUrl`/`apiKey`, `NativeModuleKey`, `ModuleCrypto.appKeyId`). Builds now need `ADDON_CLIENT_SECRET` supplied by CI, and encrypted `.lwp` module configs are rejected.
- Upstream's Playwright/browser-driven stream racing and the animated canvas land here; the `LargeNowPlayingWidget` was superseded by the single adaptive obsidian widget.

## [4.2.1] - 2026-09-26

### Added
- **True Bit-Perfect Audio Pipeline:** Introduced true bit-perfect hardware output with native C++ round-to-nearest-even conversion (`lrintf` / `llround`) in `usb-audio-output.cpp`, eliminating 1-LSB negative sample truncation on 24-bit FLAC streams. Added direct C++ integer bit-depth packing to bypass JVM conversion overhead.
- **Dual-Crystal UAC2 Hardware Clock Switching:** Integrated automatic parsing of UAC2 `CLOCK_SOURCE` and `CLOCK_SELECTOR` descriptors to switch dynamically between 44.1 kHz and 48 kHz oscillator families on supported DACs. Added smart Soxr resampling fallback for single-crystal DACs.
- **Expanded Lyrics Ecosystem:** Added support for Apple Music, Better Lyrics, Bini, Musixmatch, and SimpMusic lyrics providers, accompanied by TTML Rich Sync syllable parsing and word-by-word karaoke line splitting.
- **Fluid Artwork Background & Large Widget:** Added animated fluid gradient mesh backgrounds to the player sheet and introduced a new Large Now Playing home screen widget.

### Changed & Improved
- **Audio Download Matching Precision:** Overhauled the download matching engine to apply strict artist penalties, expected duration validation (within 5 seconds), and direct video ID routing from track menus, preventing cross-artist mismatches during downloads.
- **Signal Path Diagnostics:** Signal path dialog now displays exact DAC resampled status, original source bit depth (preventing 32-bit float decoder representation from masking track depth), active hardware sample rates, and detailed USB exclusive open failure diagnostics.
- **Volume & Output Unity Gain:** Removed the restrictive `ignoreStreamMusicMax` cap in `ExclusiveUsbOutput` so 100% volume consistently achieves true 1.0f unity gain.

### Fixed
- **YouTube Liked Music & Playlist Crash:** Fixed a fatal `LazyColumn` key collision and layout edge-reorder crash when opening connected YouTube playlists (`LM` / `yt_liked`). Replaced unstable keys with synchronized, collision-proof occurrence suffixes and guarded layout info reads during progressive track streaming.
- **USB Audio Timing & Glitches:** Corrected exclusive output timing to eliminate buzzing, forward-seek audio cutoffs, runaway seekbar animations, and pause spinner hangs.
- **Player UI Fixes:** Resolved seekbar buffering clock glitches, repeat-one track skipping, and pill button white-out visual issues.

## [4.2.0] - 2026-09-23

### Added
- **Dolby Atmos Playback:** Added Dolby Atmos playback support for compatible devices and audio configurations, providing an enhanced spatial audio experience.
- **Enhanced Liquid Glass:** Completely reworked the Liquid Glass system. The previous implementation was primarily a translucent blur effect; the new implementation introduces significantly improved depth, translucency, reflections, layering, highlights, and overall visual polish for a more refined glass experience.
- **Playlist Downloads:** Added support for downloading complete playlists for offline listening, making it easier to manage and save large collections.
- **True Bit-Perfect Playback:** Improved bit-perfect playback with untouched DAC routing, preserving the audio output path without unnecessary modification or processing.
- **YouTube Recommendation Algorithm:** The entire recommendation system has been shifted from Last.fm to YouTube, providing a new recommendation backend while making Last.fm completely optional.
- **Guest Login:** Added Guest Login, allowing users to use LastWave without creating or connecting an account.
- **Native Spotify & Apple Music Playlist Import:** Added native playlist importing from Spotify and Apple Music, allowing users to bring their existing playlists directly into LastWave.

### Changed & Improved
- **Playback & Audio Improvements:** Improved playback reliability, audio handling, DAC behavior, and various edge cases across the playback pipeline.
- **UI & Performance Improvements:** Refined multiple parts of the interface with improved responsiveness, animations, visual consistency, and overall performance.
- **Under-the-Hood Improvements:** A large number of architectural, performance, reliability, and quality-of-life improvements have been made throughout the app.

### Fixed
- **Stability & Bug Fixes:** Fixed numerous reported issues across playback, downloads, recommendations, authentication, UI, and background behavior.

## Unreleased

### Added
- **One-tap "Save album to library" on the album detail screen (#79).**
  Previously an album could only be kept by adding its songs one by one to
  a custom playlist. The album hero now has a save button that stores all
  tracks via `PlaylistRepository`, mirroring the feed playlist detail's
  save flow (saving / saved / error states). Shared-playlist pages already
  had this via the feed detail screen.

  Follow-up: the button now detects an already-saved album on every load
  (reopening shows "Saved to library") and saving is idempotent — a
  same-title library copy with overlapping tracks is reused instead of
  stacking duplicates when the tracklist loads progressively.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/album/AlbumViewModel.kt`
  `app/src/main/java/com/lastwave/app/ui/album/AlbumDetailScreen.kt`
  `app/src/main/java/com/lastwave/app/data/playlist/PlaylistRepository.kt` (`findByTitle`)

### Fixed
- **Search returning no results for Cyrillic / non-Latin queries (#102).**
  Title/artist matching used a Latin-only word pattern (`[^a-z0-9]+`),
  reducing every non-Latin query to blank, and the Songs-filtered search had
  no fallback when the filter matched nothing. Matching now uses a
  Unicode-aware pattern (letters + numbers from any script) with
  locale-independent case folding, and an empty filtered search retries once
  unfiltered. Matching helpers were extracted to `TextMatch` with unit tests
  covering Cyrillic, CJK and Arabic input.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/music/TextMatch.kt` (new),
  `app/src/main/java/com/lastwave/app/data/music/InnerTubeMusicApi.kt`,
  `app/src/test/java/com/lastwave/app/data/music/TextMatchTest.kt` (new)

### Fixed
- **Home-screen widget stuck on the previous song (#94).**
  Two publishers wrote the same widget snapshot with independent dedup
  guards: the playback service (authoritative, fires on every player-state
  transition) and the scrobble listener (watches all media sessions,
  including our own, on an async binder timeline). During a track change
  the listener could still see the previous track's session metadata and
  overwrite the fresh snapshot last, with nothing re-firing afterwards.
  The listener now yields while our own session is active and otherwise
  only elects external packages, and snapshot write + refresh in
  `WidgetUpdater` is mutex-serialized against interleaved publishers.

  Files changed:
  `app/src/main/java/com/lastwave/app/service/MediaScrobbleListenerService.kt`
  `app/src/main/java/com/lastwave/app/widget/WidgetUpdater.kt`

### Fixed
- **Logged-out YouTube Music playlists collapsing to a dead "Tap Retry" error.**
  `InnerTubeMusicApi.fetchPlaylist()` treated ANY single continuation-page
  failure (rate-limit/offline blip while paging a large playlist) as a total
  failure and returned null — discarding tracks already loaded — so the
  detail screen showed `Error` and every retry re-fetched from scratch into
  the same failure. Continuation failures now keep the collected prefix and
  return it with `isComplete = false` (null is kept only when zero tracks
  loaded, preserving every caller's existing contract); failures are logged
  to logcat under `LastWavePlaylist` with browseId + stage for diagnosis.
  `FeedPlaylistDetailViewModel` shows truncated results with an inline
  "Some tracks couldn't load. Retry." affordance instead of the dead error,
  and `YtMusicLibraryManager` no longer poisons the disk cache / track
  count with truncated snapshots (null count forces a network refresh next
  open).

  Files changed:
  `app/src/main/java/com/lastwave/app/data/music/InnerTubeMusicApi.kt`
  `app/src/main/java/com/lastwave/app/ui/feed/FeedPlaylistDetailViewModel.kt`
  `app/src/main/java/com/lastwave/app/data/ytmusic/YtMusicLibraryManager.kt`

### Author: musaibbhat120605
**Date:** September 16, 2026

###Musaib Bhat will step down as the developer of LastWave on 16 September 2026. His contributions have been invaluable, and his work will always remain a cornerstone for our community.

#### Added
- **SongPlayStatsEntity + SongPlayStatsDao** — local per-track listening stats (total play time, skip count, play count, last played).
- **SongPlayStatsRepository** — records listened time and skips.
- **LocalTasteSuggestionEngine** — local, Last.fm-free recommendation engine (ported scoring model: play time, skip penalty, liked bonus, recency, time-of-day), seeded via existing `InnerTubeMusicApi.fetchRelatedSongs`.
- **Room migration 12 → 13** — creates `song_play_stats` table without wiping existing data.

#### Changed
- **MusicPlayer.kt** — hooks `onMediaItemTransition` to record listened duration and detect skips (non-AUTO transition + <85% played).
- **AppDatabase.kt / DatabaseModule.kt** — registers the new entity/DAO, bumps DB version, adds migration.
- **FeedScreen.kt** — redesigned `QuickTilesGrid`/`QuickTileCard` (Liked Songs / Mix / New Releases): equal-width scrollable cards with per-type gradient icons, replacing the old grid-chunked layout that left an orphaned half-row.

#### Not yet wired
- `LocalTasteSuggestionEngine.run()` isn't called from any screen yet — integration notes are in the file itself (Feed's discover section or as a Last.fm fallback in `GenerateRepository`).

### Fixed
- **Player state and cached track lost after leaving the app in the background with nothing playing.** (Fix by [@musaibbhat120605](https://github.com/musaibbhat120605))

  `MusicPlaybackService.onTaskRemoved()` unconditionally called
  `musicPlayer.stopAndClear()` whenever the task left Recents — even when
  nothing was playing. `stopAndClear()` wipes both the in-memory player
  state and the persisted session in SharedPreferences
  (`clearPersistedPlaybackSession()`), so the next launch had nothing to
  restore: the player appeared closed and the last track wasn't cached,
  regardless of battery-optimization settings.

  Fixed by (1) skipping the stop entirely when something is actively
  playing, so playback isn't killed just because the task left Recents,
  and (2) giving `stopAndClear()` an optional `clearSession` parameter
  (default `true`, unchanged for every other call site) so the
  paused/idle case can stop the service without deleting the persisted
  session — leaving it restorable on the next launch.

  Files changed:
  `app/src/main/java/com/lastwave/app/playback/MusicPlaybackService.kt`,
  `app/src/main/java/com/lastwave/app/playback/MusicPlayer.kt`

- **Songs silently disappearing from the Home listing during background polling.** (Fix by [@musaibbhat120605](https://github.com/musaibbhat120605))

  `HomeViewModel`'s 12-second background refresh loop merged newly
  polled recent tracks with the existing in-memory history and then
  truncated the **entire combined list** to `HOME_TRACK_HISTORY_CAP`
  (500 entries). `loadNextPage()` (triggered by scrolling) appends
  paginated tracks without any cap of its own — so once a user
  scrolled far enough to load more than 500 tracks, the very next
  background poll would silently drop everything past position 500,
  including tracks the user had just scrolled into view seconds
  earlier. This made songs appear to randomly vanish from the Home
  listing with no user action to explain it.

  Fixed by making the background poll's cap dynamic: it now only
  bounds organic growth from polling (`max(HOME_TRACK_HISTORY_CAP,
  currentListSize)`), so it can never truncate below what pagination
  has already legitimately loaded into view.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/home/HomeViewModel.kt`

- **Duplicate/overlapping "now playing" notification on Android 10 (One UI 2.x).**
  `buildNotification()` used `Notification.DecoratedMediaCustomViewStyle`
  with a `MediaSession` attached, alongside a fully custom `RemoteViews`
  player (own artwork, title, artist, transport buttons). On Android 10 +
  Samsung One UI 2.x, SystemUI's older media-notification renderer drew
  its own full media chrome as a second layer instead of just framing the
  custom view, producing two overlapping players in the notification
  shade/quick controls.

  Now version-gated: Android 11+ keeps `DecoratedMediaCustomViewStyle`
  with the session attached as before; Android 10 and below uses
  `Notification.DecoratedCustomViewStyle` (no session tag on the
  notification itself). Lock screen controls, Bluetooth, Android Auto,
  and the in-app widget are unaffected, since they all read from
  `mediaSession` directly rather than this notification's `Style` object.

### Added
- **Offline playback priority across all screens (Fixes #31).**
  `MusicPlayer.resolveTrackAudioStream()` now checks the local Room database (`DownloadedTrackDao`) and verifies file presence on disk before attempting remote network resolution (Lossless / YouTube Music / InnerTube). When a track has already been downloaded, LastWave plays the local media file directly without making network calls, enabling seamless offline playback across Home, Search, Playlists, Album, and Artist screens and saving cellular data when online.

  Files changed:
  `app/src/main/java/com/lastwave/app/playback/MusicPlayer.kt`

- **Download state awareness and duplicate download prevention.**
  - Added `DownloadedTrackDao.findByTrackKey` and deduplication checks in `TrackDownloadManager.downloadTrack` to prevent duplicate download jobs, redundant network requests, and duplicate files (e.g. `(1).flac`) in MediaStore.
  - The 3-dot context menu sheet (`TrackContextMenuSheet`) now dynamically reflects the track's status (`Downloaded` with check icon, `Downloading…`, or `Download (Max Quality)`), giving instant visual feedback and preventing accidental re-downloads.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/local/db/DownloadedTrackDao.kt`
  `app/src/main/java/com/lastwave/app/data/download/TrackDownloadManager.kt`
  `app/src/main/java/com/lastwave/app/ui/common/TrackContextMenuSheet.kt`

- **Direct navigation from download notifications to the Downloads screen.**
  `TrackDownloadManager` now fires pending intents with `ACTION_VIEW_DOWNLOADS` targeting `DownloadsScreen` (`AppRoute.Downloads`). Integrated an `AppRouteNavigator` singleton and `AppRouteNavBridge` into `NavGraph` and `MainActivity` so tapping download notifications opens the Downloads screen directly instead of only bringing the app to the foreground.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/download/TrackDownloadManager.kt`
  `app/src/main/java/com/lastwave/app/MainActivity.kt`
  `app/src/main/java/com/lastwave/app/ui/navigation/AppRouteNavigator.kt`
  `app/src/main/java/com/lastwave/app/ui/navigation/NavGraph.kt`

## 2026-09-02 — musaibbhat120605

### Fixed
- **WebM/Opus downloads not showing metadata in external players/file managers.**
  `AudioTagWriter.embedIntoWebm()` previously appended the `Tags`/`Attachments`
  elements at the very end of the file, after all audio `Cluster` data. This
  produced structurally valid EBML, but many players and file managers only
  scan the header region of a WebM/Matroska file (stopping once they reach
  audio data) instead of reading the whole file, so the tags were effectively
  invisible outside the app.

  Metadata is now spliced in right before the first `Cluster`, matching where
  real muxers place it:
  - Any existing `SeekHead` is dropped instead of left with stale offsets —
    compliant readers fall back to a normal sequential scan when it's absent.
  - Any existing `Tags`/`Attachments` elements are removed so duplicates
    aren't left behind.
  - The `Segment` size field is patched to match the new layout.
  - If a `Cues` index is present (rare for YouTube's DASH audio, but possible
    for other muxed sources), the old end-of-file append is used instead,
    since rewriting `Cues` byte offsets safely is out of scope for this fix.

  Files changed: `app/src/main/java/com/lastwave/app/data/download/AudioTagWriter.kt`

### Fixed
- **Home screen (Last.fm) lag / stutter.**
  `HomeUiState.visibleRows()` (filters, day-groups, sorts, and dedupes the
  full track history) was being recomputed inline inside a Compose
  `remember` block, which runs on the UI thread. Last.fm's now-playing and
  recent-tracks polling ticks every 12–30 seconds, so every time a track
  scrobbled in, this fairly expensive rebuild ran right on the frame meant
  to update the screen, causing a visible stutter tied directly to
  scrobbling. On top of that, the "Recent" track list was never capped, so
  it kept growing (and getting more expensive to rebuild) the longer Home
  stayed open in a session.

  - `HomeViewModel` now recomputes the row list on a background dispatcher
    (`Dispatchers.Default`) and exposes it as its own `StateFlow`, so the UI
    thread just collects a finished list instead of building it.
  - The 30-second recent-tracks poll now caps the merged track history at
    500 entries instead of growing it indefinitely.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/home/HomeViewModel.kt`,
  `app/src/main/java/com/lastwave/app/ui/home/HomeScreen.kt`

### Fixed
- **Downloaded tracks appearing twice in the Downloads list.**
  `TrackDownloadManager.downloadTrack()` had no check against tracks that
  were already downloaded — it only guarded against the *same* download
  running twice concurrently (`activeKeys`), which is cleared as soon as a
  download finishes. Re-downloading a track you already had correctly
  overwrote the file on disk, but `downloadedTrackDao.insert()` always
  created a brand-new row: `DownloadedTrackEntity.id` is an
  autoincrement primary key with no other unique constraint, so
  `OnConflictStrategy.REPLACE` never had anything to actually collide
  with.

  - Added a normalized `trackKey` column (`"${artist}_${title}"`,
    lowercased/trimmed) with a **unique index**, so the database itself
    can no longer hold two rows for the same track.
  - Migration `10 → 11` backfills `trackKey` for existing rows, deletes
    any duplicate rows already present (keeping the most recently
    downloaded copy of each), then creates the unique index.
  - `downloadTrack()` now checks the database first; if the track is
    already downloaded and its file still exists, it skips re-downloading
    entirely instead of re-fetching and duplicating. If the file was
    removed outside the app, it falls through and re-downloads, and the
    unique index makes that insert safely `REPLACE` the stale row instead
    of duplicating it.

  Files changed:
  `app/src/main/java/com/lastwave/app/data/local/db/DownloadedTrackEntity.kt`,
  `app/src/main/java/com/lastwave/app/di/DatabaseModule.kt`,
  `app/src/main/java/com/lastwave/app/data/download/TrackDownloadManager.kt`

### Fixed
- **Skipping to the next track did nothing when playing from Downloads.**
  `DownloadsViewModel.playTrack()` started playback via `MusicPlayer.play()`,
  which always builds a single-track queue (`listOf(track)`) regardless of
  how many tracks are downloaded. So the moment you played anything from the
  Downloads screen, the player's queue had exactly one item — there was
  never a "next" track to advance to, so `next()` correctly found no
  following item and silently did nothing.

  `playTrack()` now builds the full queue from every currently downloaded
  track (in the same order shown on screen), starting at the tapped
  track's position, via `MusicPlayer.playQueue()` instead of `play()`.
  Next/previous now move through the rest of your downloads normally.

  Files changed:
  `app/src/main/java/com/lastwave/app/ui/settings/DownloadsViewModel.kt`
