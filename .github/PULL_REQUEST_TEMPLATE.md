<!-- Thanks for the PR! Read CONTRIBUTING.md if you haven't. -->

## Summary

<!-- One or two sentences: what does this change do, and why? -->

## Linked issues

<!-- "Closes #42" / "Refs #17". Delete if none. -->

## What changed

<!-- Bullet list of the user-facing or build-facing changes. Group by area (app / CI / docs). -->

-

## Tests run

Tick the ones you ran locally. The first two are required for every PR; the third is required
if your change touches the accessibility service, overlay, extractor, translation engine, or
tile.

- [ ] `./gradlew :app:testDebugUnitTest` — passes
- [ ] `./gradlew :app:lintRelease` — passes (no new Error-severity warnings)
- [ ] `./gradlew :app:connectedDebugAndroidTest` — passes on an emulator (API 24–35)
- [ ] `./gradlew :app:assembleRelease` — builds cleanly (R8 + ABI splits)

If you didn't run one of these, explain why below.

## Checklist

- [ ] Branch is up to date with `main`.
- [ ] Commits follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`,
      `fix:`, `test:`, `ci:`, `docs:`, `refactor:`, `chore:`, `perf:`, `build:`).
- [ ] **No secrets committed** — no keystore files, no `keystore.properties`, no passwords, no
      API keys. (If you accidentally staged one: `git reset <file>`; do not just delete it.)
- [ ] No new dependencies, OR new dependencies are pinned in `gradle/libs.versions.toml` and
      justified below.
- [ ] No new compiler warnings introduced.
- [ ] If user-facing behavior changed, this PR description explains before → after.
- [ ] If a new target language was added: wired into `TargetLanguages.kt` + a unit test added.
- [ ] If a new permission was added: justified here + reflected in the Privacy / Security
      sections of the README if needed.
- [ ] Read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — be kind in review.

## Notes for the reviewer

<!-- Anything non-obvious? Alternatives you rejected? CI-only behavior to watch for? -->
