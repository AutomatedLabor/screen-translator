---
name: Bug report
about: Something isn't working as expected
title: "[bug] "
labels: bug
assignees: ''
---

## What happened?

**Steps to reproduce:**
1.
2.
3.

**Expected:** what you thought would happen.

**Actual:** what happened instead.

## Environment

- **Android version:** (e.g. 13, API 33)
- **Device:** (e.g. Pixel 7, Samsung S22)
- **App version:** (find it in Settings → App Info → Screen Translator, or in the APK filename)
- **APK variant:** [ ] arm64-v8a  [ ] universal
- **Did you download the target-language model?** [ ] Yes  [ ] No  [ ] Not sure
  (Open the app → does the "Download model" card show a green checkmark?)

## Accessibility service

- **Is the Accessibility Service enabled?** [ ] Yes  [ ] No
  (Settings → Accessibility → Screen Translator → toggle)
- **On Android 13+, did you grant "Allow restricted settings"?** [ ] Yes  [ ] No  [ ] N/A
  (Long-press the app icon → App Info → ⋮ → Allow restricted settings)

## Tile behavior

- **Does the "Translate Screen" tile appear in your Quick Settings shade?** [ ] Yes  [ ] No
- **When you tap the tile, what happens?**
  - [ ] Nothing (tile doesn't flip to active)
  - [ ] Tile flips active but no overlay appears
  - [ ] Overlay appears but is empty / wrong position
  - [ ] Overlay appears but translation is wrong / missing
  - [ ] Overlay doesn't dismiss when I scroll or tap ✕
  - [ ] Other: ___________
- **On Android 7-10: did you swipe the notification shade away after tapping the tile?**
  (The shade doesn't auto-collapse on API 24-29.) [ ] Yes  [ ] No  [ ] N/A

## Logs / screenshots

If you can, attach:
- A screenshot of the screen **before** you tap the tile.
- A screenshot of the overlay (or the empty space where it should be).
- Logcat output filtered to `TranslatorAccessibility` / `ScreenTextExtractor` /
  `MlKitTranslationEngine` / `OverlayManager`:

  ```
  adb logcat -s TranslatorAccessibility:* ScreenTextExtractor:* MlKitTranslationEngine:* OverlayManager:*
  ```

> ⚠️ **Do not paste screen contents into a public issue if they contain personal data**
> (banking, messages, addresses). Replace with placeholder text. The app reads your screen, so
> logs may contain visible text.

## Anything else?

Add any other context here.
