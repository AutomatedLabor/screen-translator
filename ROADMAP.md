# Roadmap — Screen Translator Android app

Living document. Updated every improvement cycle. Lives at
`github.com/AutomatedLabor/translation-app` (fork of `mahadhameed095/translation-app`).

**Current release: v1.2.0** (signed with `CN=Screen Translator Release`, cert SHA-256
`a0b5e5…7fa2`). v1.3.0 in progress.

---

## The two APK variants (v1.3.0+) — what each is for

From v1.3.0 the release ships **two APKs** via ABI splits, so you pick the smallest one
that works for your device:

| APK | ABIs bundled | Size | Install on |
|---|---|---|---|
| **`screen-translator-v1.3.0-arm64-v8a-release.apk`** | arm64-v8a only | ~40 MB | **Any phone from ~2017 onward** — Pixel, Samsung, OnePlus, Xiaomi, Oppo, Vivo, Realme, Motorola, etc. (64-bit ARM). This is what ~99% of users want. |
| **`screen-translator-v1.3.0-universal-release.apk`** | arm64-v8a + armeabi-v7a + x86 + x86_64 | ~107 MB | **Everything else** — 32-bit ARM phones (pre-2017), Android Studio x86_64 emulators, rare x86 tablets. The fallback. |

**Which to install?** Try `arm64-v8a` first (much smaller download). If install fails with
`INSTALL_FAILED_NO_MATCHING_ABIS`, your device is 32-bit or x86 — use `universal`.

**Why two APKs?** ML Kit's on-device translation bundles native libraries
(`libtranslate_jni.so`, `liblanguage_id_l2c_jni.so`) for 4 CPU architectures. A single
"universal" APK carries all 4 copies (~107 MB). An arm64-v8a-only APK carries one copy
(~40 MB) — a 63% size cut with zero functionality loss on any 64-bit ARM phone.

---

## What's DONE (v1.1.0 + v1.2.0)

### v1.1.0 — release-signing fix
- Dedicated release keystore (`CN=Screen Translator Release, RSA 2048, 25y`), stored as
  GitHub Secrets, replacing the throwaway debug keystore that signed v1.0.0/v1.0.1.
- CI fail-closed on signing: `apksigner verify` gate requires `Verifies` + v3=true +
  DN=`CN=Screen Translator Release` + cert SHA-256 matches pinned baseline (rotation
  detection) + explicitly rejects the debug key.
- SHA-256 sidecar + `apksigner-verify.txt` + `badging.txt` as release artifacts.

### v1.2.0 — fresh-eyes review: 1 P0 + 14 P1 fixes
- **P0: shade-open race** — `pickTargetWindow()` filtered by `isActive`, but the target
  app window is NOT active while the notification shade is open (you tap the tile from the
  shade), so the core feature silently no-opped. Fixed: pick by layer.
- **Leaks:** overlay `tv` view leak on non-BadToken throws; native ML Kit translator leak
  when `warmup()` races with `close()`; root `AccessibilityNodeInfo` recycled on API 31-32.
- **Concurrency:** `CancellationException` rethrown across 5 catch blocks.
- **UX/a11y:** edge-to-edge for Android 15; model status on resume; close-button TalkBack
  label + cutout mode; overlay text respects font-size setting.
- **Tile:** inline `dismiss()` (no stale STATE_ACTIVE); API 34+ `startActivityAndCollapse`.
- **Hardening:** `allowBackup=false`; launcher package cached once per process.

---

## What's IN PROGRESS (v1.3.0)

- **ABI splits** — arm64-v8a + universal APKs (see table above).
- **R8 minification + resource shrinking** for release — smaller APK + code obfuscation
  (harder to reverse-engineer). Generous keep rules for ML Kit / GMS / the app's classes;
  AGP auto-applies Compose/AndroidX consumer rules.
- **Target language picker** — currently hardcoded German→English. v1.3.0 adds a dropdown
  in MainActivity: German→{English, Urdu, French, Spanish, Italian, Portuguese, Russian,
  Arabic, Turkish, Hindi, Chinese, Japanese}. Persisted to SharedPreferences; the service
  picks up the change live (recreates its engine) without re-enabling accessibility.
- **Security hardening** — `usesCleartextTraffic=false` + `network_security_config.xml`
  (block cleartext app-wide; ML Kit's HTTPS model download is unaffected); explicit
  `data_extraction_rules.xml` excluding all domains from backup/transfer (belt-and-suspenders
  on `allowBackup=false`).
- **`FlagSecureTestActivity`** moved from the androidTest sourceset to the debug sourceset
  (it was declared in the debug manifest but compiled into the test APK → dangling exported
  activity in the debug APK).
- **CI hardening** — gitleaks secret scan; OSV dependency-vuln scan; GitHub Actions pinned
  to commit SHAs (supply-chain); `./gradlew lint` report step.
- **Unit tests** for the language-pref helper + the isActive fix.
- versionCode 3→4, versionName 1.2.0→1.3.0.

---

## Roadmap (deferred — not blocking, with rationale)

| Item | Why deferred |
|---|---|
| **Real-device install + tile-tap verification** | No physical device in the sandbox; the APK is verified installable via apksigner/aapt/41 unit tests, but the actual install + accessibility flow on a phone is the user's step. |
| **Instrumented test for the shade-open window pick** | Needs an emulator in CI (android-emulator-runner action) — adds ~3 min + flakiness; the P0 isActive fix is covered by a Robolectric unit test instead. |
| **Copy-translation-to-clipboard** (tap a card) | The overlay is `FLAG_NOT_TOUCHABLE`; making individual cards tappable requires per-card hit-testing + a touchable region, conflicting with the pass-through design. |
| **OCR fallback for image-rendered text** | Fundamentally incompatible with the spec: OCR needs a screenshot (MediaProjection), which re-introduces the `FLAG_SECURE` black-screen problem the accessibility approach was chosen to bypass. |
| **Translation history (Room DB)** | Low value for an ephemeral screen translator; would add a persistence layer + schema migrations. |
| **i18n of the UI strings (German, Urdu, …)** | The UI is English-only; the source language translated is German. Adding UI translations is a documentation/UX task, not a functional gap. |
| **Foreground-service notification** | AccessibilityServices are exempt from the foreground-notification requirement; adding one would be cosmetic. |
| **`android:largeHeap` / `configChanges` tuning** | Not needed; ML Kit manages its own memory; Compose handles config changes. |
