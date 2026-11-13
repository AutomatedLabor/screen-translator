# Contributing to Screen Translator

Thanks for your interest in improving **Screen Translator** — an Android AccessibilityService
app that translates German on-screen text on-device from a Quick Settings tile. This is a fork of
[`mahadhameed095/translation-app`](https://github.com/mahadhameed095/translation-app), maintained
at [`AutomatedLabor/translation-app`](https://github.com/AutomatedLabor/translation-app).

PRs of every size are welcome — bug fixes, new target languages, accessibility improvements,
test coverage, docs. Please read this file before opening one.

---

## 1. Set up your environment

### Prerequisites

- **JDK 17** (Temurin / OpenJDK). Check with `java -version`.
- **Android SDK** with:
  - `platforms;android-35` (compileSdk / targetSdk 35)
  - `build-tools;35.0.0`
  - `platform-tools` (latest)
- **Git** ≥ 2.30.
- An **Android 7+ device or emulator** if you plan to run instrumented tests.

### Install the SDK components

```bash
# via sdkmanager (Android Studio bundles this)
sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"
```

### Clone & build

```bash
git clone https://github.com/AutomatedLabor/translation-app.git
cd translation-app
./gradlew :app:assembleDebug          # build a debug APK
```

If the wrapper fails to download Gradle, check your JDK version — AGP 8.5.2 needs JDK 17.

---

## 2. Run the tests

Before opening a PR, all three of these must pass locally:

```bash
# 50 JVM unit tests (Robolectric + Truth) — runs in seconds, no device needed
./gradlew :app:testDebugUnitTest

# Lint report — catches AccessibilityService / permission / Compose issues
./gradlew :app:lintRelease

# Instrumented test on a real device or emulator (needs the emulator running)
./gradlew :app:connectedDebugAndroidTest
```

For `connectedDebugAndroidTest`, start an emulator first (API 24–35 all supported). The CI
workflow boots an API-35 emulator with KVM enabled; see `.github/workflows/build.yml` for the
exact recipe.

---

## 3. Signing

### Local development

Debug builds are signed with the default Android debug key automatically — no setup needed.

For local **release** builds, the app reads an optional `keystore.properties` at the repo root
(gitignored). This is **for local dev only** — CI re-signs with the real release key:

```properties
# keystore.properties (do NOT commit this file)
storeFile=/absolute/path/to/your.jks
storePassword=...
keyAlias=...
keyPassword=...
```

### CI signing (the real release APKs)

The published APKs are signed in CI from an **unsigned** build via `zipalign` + `apksigner`:

- Keystore is held as **encrypted GitHub Secrets** (RSA 2048, CN=Screen Translator Release, 25y).
- Both **v2 + v3** signature schemes are forced (v1 is impossible for minSdk 24; v2 covers
  Android 7-8, v3 covers Android 9+).
- CI fails closed if the resulting APK doesn't verify, isn't signed with the release key, or the
  cert SHA-256 doesn't match the pinned baseline
  (`a0b5e5f35ee134d2816a24cc467297b3024ea466f1d5d98c0104b3e8b7e27fa2`).

**Contributors do not need and cannot get the release key.** Your PR builds a debug or unsigned
release APK; CI handles signing on merge to `main`.

See [SECURITY.md](SECURITY.md) for the full disclosure on what is signed and how to verify it.

---

## 4. Commit style — Conventional Commits

We follow [Conventional Commits](https://www.conventionalcommits.org/). Keep the subject line
short, imperative mood, lowercase after the type:

```
feat(tile): collapse notification shade on API 30+ on tap
fix(extract): skip nodes with empty text content
test(translate): add LruCache eviction assertions
ci: pin emulator-runner to a SHA
docs(readme): add architecture diagram
chore: bump Kotlin to 2.0.21
```

Common types: `feat`, `fix`, `test`, `ci`, `docs`, `refactor`, `chore`, `perf`, `build`.

- **One logical change per commit.** Don't bundle a feature + a refactor + a dependency bump.
- Reference issues in the body if relevant: `Closes #42`.
- Avoid `--no-verify` — pre-commit hooks (gitleaks) exist to catch secrets.

---

## 5. Pull request checklist

Before you click **Open pull request**:

- [ ] Branch is up to date with `main`.
- [ ] `./gradlew :app:testDebugUnitTest` passes.
- [ ] `./gradlew :app:lintRelease` passes (no new warnings of severity Error).
- [ ] `./gradlew :app:connectedDebugAndroidTest` passes on an emulator (if your change touches
      the accessibility service, overlay, extractor, or tile).
- [ ] Commits follow Conventional Commits.
- [ ] **No secrets committed** — no keystore files, no passwords, no API keys, no
      `keystore.properties`. If you accidentally stage one, `git reset` it; do not just delete
      the file (it stays in history). Re-run gitleaks: `gitleaks detect --source .`.
- [ ] If you added a new dependency, it's pinned in `gradle/libs.versions.toml` and justified in
      the PR description.
- [ ] If you changed user-facing behavior, the PR description explains the before/after.
- [ ] If you added a new target language, it's wired into `TargetLanguages.kt` + has a unit test.

The PR template at `.github/PULL_REQUEST_TEMPLATE.md` mirrors this checklist.

---

## 6. Process lessons

This repo's release process (signing, CI hardening, the v1.1.0 → v1.7.0 audit trail) is
documented as reusable lessons in the companion repo
[`AutomatedLabor/build-learnings`](https://github.com/AutomatedLabor/build-learnings). If you're
curious why CI does something a particular way — SHA-pinned actions, fail-closed `apksigner
verify`, the Council review format — that's where the rationale lives.

---

## 7. Code of conduct

Everyone participating in this project is expected to uphold the
[Contributor Covenant 2.1](CODE_OF_CONDUCT.md). Be kind, be patient, assume good intent.

---

## 8. License

By contributing, you agree your contributions are licensed under the [MIT license](LICENSE) that
covers this project. The original app is © mahadhameed095; contributions to this fork are
© AutomatedLabor and the respective contributors.
