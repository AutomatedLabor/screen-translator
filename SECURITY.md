# Security Policy

## Supported versions

Screen Translator is a small project; only the **latest release** receives security fixes.

| Version | Supported          |
|---------|--------------------|
| v1.7.0+ | :white_check_mark: |
| < v1.7.0 | :x: — update to the latest release. |

If you're on an older release, install the latest from
[Releases](https://github.com/AutomatedLabor/translation-app/releases/latest). Note: users on
v1.0.0 / v1.0.1 must **uninstall first** — the signing key changed in v1.1.0. v1.1.0+ users can
update in place.

## Scope

This policy covers:

- The **Screen Translator Android app** in this repository (`app/`).
- The **CI workflow** that builds and signs the release APK (`.github/workflows/build.yml`),
  including the signing keys and the fail-closed verification gate.
- The **Gradle wrapper** jar (validated in CI for supply-chain integrity).

It does **not** cover:

- Upstream dependencies (ML Kit, AndroidX, Compose, Kotlin) — report those to their maintainers.
- The original [`mahadhameed095/translation-app`](https://github.com/mahadhameed095/translation-app)
  this was forked from.

## Reporting a vulnerability

**Do NOT open a public GitHub issue for a security bug.**

Instead, report it privately via
[GitHub Security Advisories](https://github.com/AutomatedLabor/translation-app/security/advisories/new):

1. Go to the **Security** tab → **Advisories** → **New draft advisory**.
2. Fill in a title, a short description, and the steps to reproduce.
3. Add `AutomatedLabor` as a collaborator on the advisory so we see it.

We aim to acknowledge within **72 hours** and to ship a fix or mitigation within **30 days** for
high-severity issues. We'll coordinate a public disclosure date with you — default is 90 days
after the fix ships, or sooner if you prefer.

If GitHub Security Advisories aren't workable for you, email
**security@automatedlabor.dev** with an encrypted message (request our PGP key first).

Please include, where relevant:

- The app version (Settings → App Info, or `apksigner verify --print-certs`).
- Your Android version + device.
- Whether the Accessibility Service was enabled.
- A minimal repro (the app + the screen content that triggers it).
- Whether the issue could leak screen contents off-device.

## Verifying a release APK

Every release ships a signed APK, a `*.apk.sha256` sidecar, and an `apksigner-verify.txt` proof
file from CI. To confirm an APK is genuine (not a malicious re-sign) before installing:

```bash
apksigner verify --print-certs --verbose screen-translator-…-arm64-v8a-release.apk
# expect:
#   Verifies
#   v2=true, v3=true
#   Signer #1 certificate DN: CN=Screen Translator Release, O=AutomatedLabor, C=US
#   Signer #1 certificate SHA-256 digest: a0b5e5f35ee134d2816a24cc467297b3024ea466f1d5d98c0104b3e8b7e27fa2

sha256sum -c screen-translator-…-arm64-v8a-release.apk.sha256
```

**Pinned release signing certificate:**

| Field | Value |
|---|---|
| Subject DN | `CN=Screen Translator Release, O=AutomatedLabor, C=US` |
| Algorithm | RSA 2048 |
| Cert SHA-256 | `a0b5e5f35ee134d2816a24cc467297b3024ea466f1d5d98c0104b3e8b7e27fa2` |
| Signature schemes | v2 + v3 (v1 is impossible for minSdk 24) |

If `apksigner verify` shows a different DN or a different SHA-256, **do not install the APK** —
report it via the process above.

> Use `apksigner verify`, **not** `keytool -printcert`. `keytool` only reads v1 signatures and
> will print a misleading "Not a signed jar file" on a v2/v3-only APK.

## What the app does (and does not) touch

- **Reads** the on-screen accessibility tree of the topmost app window when you tap the tile.
- **Translates** that text on-device with ML Kit (no network after the one-time model download).
- **Renders** the translation as an overlay on top of the original text.
- `allowBackup=false` + `data_extraction_rules.xml` exclude every domain from backup/transfer.
- `network_security_config.xml` blocks cleartext traffic app-wide.
- Nothing about your screen contents leaves the device.

## Disclosure credits

We thank everyone who reports responsibly. With your permission, we'll credit you in the
release notes and in this file's changelog of disclosures.
