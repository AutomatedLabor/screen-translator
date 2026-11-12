# Screen Translator

An Android accessibility-service app that translates **German on-screen text into English**
in near real-time, triggered from a **Quick Settings tile**. On-device ML Kit translation —
no cloud, no account, no network after the initial model download.

## How it works

1. You tap the **"Translate Screen"** Quick Settings tile while a German app is open.
2. `TranslatorAccessibilityService` walks the foreground app's `AccessibilityNodeInfo` tree,
   collects text regions, filters out numbers/dates/tracking codes/English, and groups
   adjacent text into line blocks.
3. ML Kit translates each block from German to English on-device (cached in an LRU of 500).
4. The translated text is rendered as **rounded cards directly on top of the original German**,
   via a `TYPE_ACCESSIBILITY_OVERLAY` window (no `SYSTEM_ALERT_WINDOW` permission needed).
5. A separate small tappable close button is drawn at the top-right. Scrolling, switching apps,
   or tapping the close button dismisses the overlay.

## Why an AccessibilityService (not MediaProjection)

- **No `FLAG_SECURE` black screens** — accessibility node traversal reads the text content
  directly, bypassing the secure-surface flag that blocks `MediaProjection` screenshots in
  banking / DHL / parcel-tracking apps.
- **No overlay permission** — `TYPE_ACCESSIBILITY_OVERLAY` is a trusted window type available
  to bound accessibility services on Android 12+, with reliable touch pass-through.
- `android:isAccessibilityTool="true"` is declared so `accessibilityDataSensitive` views in
  Android 14+ apps remain readable.

## Architecture

```
com.translator.screen/
├── App.kt                              — Application (placeholder)
├── ui/MainActivity.kt                  — Compose Material 3 setup screen (3 cards)
├── service/
│   ├── TranslatorAccessibilityService  — host: window pick, snapshot, translate, overlay
│   ├── QuickTileService                — thin tile trigger, state sync, unlockAndRun
│   └── OverlayManager                  — two TYPE_ACCESSIBILITY_OVERLAY windows
├── extract/
│   ├── BoundingBox, ScreenTextRegion, TranslatedTextRegion
│   ├── IScreenTextExtractor
│   └── ScreenTextExtractor             — DFS walk, dedup, spatial line grouping
├── translate/
│   ├── ITranslationEngine
│   └── MlKitTranslationEngine          — translate + language-id filter + LruCache
└── util/PackageUtils                   — launcher package detection
```

## Build

```bash
./gradlew :app:assembleRelease
# → app/build/outputs/apk/release/app-release.apk
```

Requirements: JDK 17, Android SDK with `platforms;android-35` + `build-tools;35.0.0`.

The release variant is signed with the **debug keystore** (see `app/build.gradle.kts`) so the
APK is immediately installable via `adb install` or by tapping the file. For Play Store
distribution you'd replace `signingConfigs.getByName("debug")` with a real release config.

## Test

```bash
./gradlew :app:testDebugUnitTest
```

41 JVM unit tests (Robolectric + Truth):

| Class | Tests | Covers |
|---|---|---|
| `BoundingBoxTest` | 17 | geometry, containment, union, overlap fractions |
| `ScreenTextExtractorTest` | 9 | DFS walk, dedup, line grouping, MAX_NODES cap |
| `MlKitTranslationEngineFilterTest` | 11 | regex + language-id pre-filter |
| `LruCacheBehaviorTest` | 4 | 500-entry LRU eviction, MRU promotion |

## Install on a phone

1. `adb install app-release.apk` (or tap the APK file on the phone).
2. **Android 13+ sideloaded APKs only:** long-press the app icon → App Info → three-dot
   menu → **"Allow restricted settings"**. Without this, the Accessibility toggle for the
   service is hidden.
3. **Settings → Accessibility → Screen Translator → enable.**
4. Open the app → tap **"Download model"** (German → English ML Kit model, ~30 MB).
5. Pull down the Quick Settings shade twice → edit → drag the **"Translate Screen"** tile
   into the active row.
6. Open any German app, pull down the shade, tap **"Translate Screen"**.

## Stack

- Kotlin 2.0.21, AGP 8.5.2, Gradle 8.9
- Jetpack Compose (BOM 2024.09.00) + Material 3
- ML Kit `translate:17.0.3` + `language-id:17.0.6` (on-device, offline after model download)
- `kotlinx-coroutines-android:1.8.1` + `kotlinx-coroutines-play-services:1.8.1`
- `minSdk = 31` (Android 12), `targetSdk = 35` (Android 15), `compileSdk = 35`

## CI

GitHub Actions workflow at `.github/workflows/build.yml`:
on every push to `main`, builds debug + release APKs, runs unit tests, and publishes a
GitHub Release with both APKs attached.
