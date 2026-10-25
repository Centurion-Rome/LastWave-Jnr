# Builds

- **Release Artifact:** `LastWave-Jnr-v4.3.3-universal.apk` (published release; CI uses `LastWave-Jnr-v4.3.3-release.apk`)
  - Variant: `release` (`:app:assembleRelease`)
  - Optimizations: R8 code shrinking, dead-code elimination, resource shrinking (`isMinifyEnabled = true`, `isShrinkResources = true`)
- **Debug Artifact:** `LastWave-Jnr-v4.3.3-debug.apk` (not included in published releases)
  - Variant: `debug` (`:app:assembleDebug`)
  - Uncompressed full DEX with all debug symbols, no R8 shrinking
- **Version:** `4.3.3` (versionCode `33`)
- **Feature:** Full Screen Cover Art implementation (static & animated canvas full-bleed hero with dynamic bottom fade and scrim protection)
- **Toolchain:** Gradle 9.3.1, OpenJDK 17, Android compileSdk 37, targetSdk 35, minSdk 29
