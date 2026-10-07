# TODO — open points

Carried over from the 2026-09-28 upstream sync + v4.2.3 release session.

**State at hand-off:** `main` = `2d89ecd`, in sync with `origin/main`, working tree
clean. Tag `4.2.3` and release
[v4.2.3](https://github.com/Centurion-Rome/LastWave-Jnr/releases/tag/4.2.3) are live.
Upstream bug report filed as
[LastWave-Native#192](https://github.com/Clash-Projects/LastWave-Native/issues/192).

---

## 1. There is no real release keystore

Every Jnr build ever shipped — including 4.2.3 — is signed with the **Android
Debug** certificate. There has never been a release key for this fork.

- `app/build.gradle.kts:119` silently does `initWith(getByName("debug"))` when no
  keystore is configured.
- Confirmed by pulling the 4.2.2 APK and reading its certificate:
  `CN=Android Debug`, SHA-256 `fdabfa8d6637d43a3a8120c0b7c9c5fb3ff3b9629cfce5edd68d5c1354655a34`.
- That fingerprint matches this machine's `~/.android/debug.keystore`, which is why
  v4.2.3 could be built and shipped locally with no secrets and still upgrade in
  place for existing users.

**Trade-off to decide deliberately:** setting up a real release key fixes the Play
Protect warnings, but the signature must change, so **every existing user will have
to uninstall once**, losing their downloads and settings. Not a decision to make
casually. `app/build.gradle.kts:86-121` needs rework either way, and the `else`
branch should throw rather than fall back to debug signing.

## 2. The CI release path is broken

`Centurion-Rome/LastWave-Jnr` has **zero Actions secrets** (verified via API,
`total_count: 0`). Missing: `SIGNING_KEY`, `KEY_STORE_PASSWORD`, `ALIAS`,
`KEY_PASSWORD`. (`ADDON_CLIENT_SECRET` is also absent but only warns.)

Chain of failure:

1. `gradle.properties:16` hardcodes `RELEASE_STORE_FILE=release.keystore`, but
   `*.keystore` is gitignored (`.gitignore:36`) so the file is absent on a runner.
2. `app/build.gradle.kts:110` sees no keystore, so it falls to
   `initWith(getByName("debug"))`.
3. AGP looks for `~/.config/.android/debug.keystore`, which GitHub-hosted runners
   don't have.
4. `:app:validateSigningRelease` fails → whole release workflow fails.

Run `36470027317` is the resulting failure, left in history deliberately.

The fork has **only ever had that one workflow run**, so 4.2.2 (and presumably
earlier releases) were built locally and uploaded out-of-band. Until secrets are
configured, **keep releasing the local way**:

```powershell
.\gradlew.bat clean assembleRelease assembleRawRelease
# copy app/build\outputs\apk\release\app-release.apk        -> LastWave-Jnr-v<VER>-universal.apk
# copy app/build\outputs\apk\rawRelease\app-rawRelease.apk -> LastWave-Jnr-v<VER>-universal-uncompressed.apk
.\gradlew.bat assembleRelease -PminSdk=24
# copy app/build\outputs\apk\release\app-release.apk        -> LastWave-Jnr-v<VER>-android7.apk
# Get-FileHash -Algorithm SHA256 -> SHA256SUMS.txt   (note: sha256sum is NOT on Windows)
gh release create <VER> --repo Centurion-Rome/LastWave-Jnr --title "LastWave-Jnr <VER>" --notes-file <file> <apks> <sums>
```

Copy each APK out **before** the next build overwrites the output directory.

## 3. Three inconsistent APK naming schemes

| Source | Pattern |
| --- | --- |
| `release.yml:88-126` (publishes releases) | `LastWave-Jnr-v${TAG}-universal.apk` / `-universal-uncompressed.apk` / `-android7.apk` |
| `build.yml:99-139` (CI artifacts) | `LastWave-Jnr-v${VERSION}-release.apk` / `-raw.apk` / `-android7.apk` |
| Actual 4.2.2 release (single asset) | `LastWave-Jnr-4_2_2.apk` |

`README.md:85` currently says `LastWave-Jnr-v4.2.3-release.apk`, which matches
**build.yml**, not the name actually published. **Worth fixing** — pick one scheme
and align all three.

## 4. `release.yml` auto-generated notice names files that don't exist

`.github/workflows/release.yml:155-167` hardcodes `LastWave-v__TAG__-*.apk` —
missing the `-Jnr` prefix. That's why the 4.2.2 release page shows a download
table pointing at files that were never attached (and it promised three packages
when only one APK was uploaded).

The workflow only appends that notice when the supplied `release_notes` does
**not** contain the string `android7` (`release.yml:172-179`). v4.2.3 sidestepped
it by passing a complete body. Fix the literal rather than relying on that.

## 5. README screenshots are stale

They still show pre-widget-revamp UI and don't match the shipped app. Deliberately
deferred at release time. Needs a device or emulator to re-capture — overlaps
with task 1, so do them together.

## 8. Minor / low priority

- **Upstream `.gitignore` adds a broad `*.zip`** (from `6dc0756`). Harmless today
  since no `.zip` is committed, but it will silently ignore any future one.
  Consider narrowing to `lastwave-addon-service/**/*.zip`.
- **CHANGELOG `[4.2.2]` was hand-merged.** Both sides had written their own
  `[4.2.2]` section; the union is a reconstruction, not either original.
- **Widget upgrade path** — v4.2.3 tells users to remove and re-add the widget.
  Worth a follow-up issue/comment if people report confusion.

## 9. Upstream follow-ups

- [ ] Watch
  [#192](https://github.com/Clash-Projects/LastWave-Native/issues/192) for a
  response. The DASH `$Number$` fix is already applied locally; revert our
  `98979e7` if they land their own.
- [ ] Upstream's `main` is **5+ commits ahead** of the `4b2`/`fix-player-ui-and-discover-mix`
  branch we cherry-picked, and that branch is an unreviewed PR. Expect conflict
  surface in `PlayerHost.kt`, `PlaylistDetailScreen.kt` and `MainShell.kt` on the
  next sync.

---

### Note for the next upstream sync

`upstream/main` force-pushed a **full history rewrite**, so a plain
`git merge upstream/main` computes a merge base of `0ffd716` (2026-08-15) and
reports ~76 bogus conflicts. That was worked around with:

```powershell
git replace --graft 92e3922 82c8e8b   # 92e3922 and 82c8e8b have identical trees
git merge upstream/main
git replace -d 92e3922
```

This is now **self-healed** — `21edf34` is a parent of `main`, so
`merge-base(main, upstream/main)` returns `21edf34` and future merges are normal.
Only needed if history is rewritten again.
