# Signed Releases

Use JDK 17, SDK platform 35 and build-tools 34.0.0 as in the README. Signing is local: no key or password is uploaded to GitHub/CI. Ordinary CI builds stay unsigned.

```bash
python3 scripts/release.py init-key
python3 scripts/release.py build --offline
```

Omit `--offline` when dependencies are not yet cached. `init-key` creates one RSA-3072/PKCS12 identity under the user's private `~/.local/share/recipegrabber-signing/` directory and an ignored `.env.release-signing` file in the repository. Both files are owner-only; repeated initialization preserves the identity and refuses to replace an orphaned keystore. Keep a secure encrypted backup of the keystore and configuration outside this working copy. Losing either can prevent future updates of signed installations. The helper does not provide an off-device key backup or Play App Signing enrollment.

The generated env file is parsed as literal `NAME=value` lines, not executed as shell code. It supplies these four environment variables to Gradle:

- `RECIPEGRABBER_KEYSTORE_FILE` (absolute PKCS12 path)
- `RECIPEGRABBER_STORE_PASSWORD`
- `RECIPEGRABBER_KEY_ALIAS`
- `RECIPEGRABBER_KEY_PASSWORD`

Partial configuration fails closed. The helper always passes `-PrequireReleaseSigning=true`, so a missing key cannot silently produce an unsigned deliverable. With no signing environment, ordinary `assembleRelease` remains unsigned for public CI. Do not commit the env file, keystores, Gradle caches or signed binaries to source history.

## Outputs And Verification

- `app/build/outputs/apk/release/app-release.apk`: production package `com.recipegrabber`, minified and non-debuggable.
- `app/build/outputs/bundle/release/app-release.aab`: signed production bundle. It is not installable directly or automatically published.
- `app/build/outputs/apk/releaseCheck/app-releaseCheck.apk`: minified, non-debuggable isolated test package `com.recipegrabber.releasecheck`, using the same private signing identity and release configuration.

The build helper runs JVM tests/lint/builds, checks APK signatures using `apksigner`, compares the signer certificate to `keytool`, and checks the exact expected package plus non-debuggable manifest using `aapt`. It checks bundle cryptographic verification with `jarsigner` and its embedded signer with `keytool`. Reported SHA-256 hashes identify the exact artifacts, not just their filenames.

```bash
python3 -m unittest discover -s scripts -p 'test_*.py' -v
python3 scripts/release.py verify --apk app/build/outputs/apk/release/app-release.apk
python3 scripts/release.py verify --apk app/build/outputs/apk/releaseCheck/app-releaseCheck.apk \
  --package com.recipegrabber.releasecheck
```

Private signing command output is withheld on failure to avoid exposing credentials. Check configuration ownership/permissions, JDK/SDK paths and local Gradle reports. Never publish raw signing errors or credential files.

## OAuth And Non-Destructive Device Testing

Google Android OAuth registration must match each package and the actual signing certificate SHA-1 in the existing Cloud project. Release and releaseCheck require separate Android clients; debug registration does not authorize the new release certificate. Do not replace existing clients, broaden scopes, change billing/IAM or publish the consent screen as part of a signing change. Play App Signing can introduce a different distribution certificate and needs its own matching registration.

Android cannot update an existing debug-signed app using an unrelated release key. Do not uninstall/clear that app merely to make a test pass. The isolated releaseCheck package can coexist and recover the same project's authorized Drive snapshots into empty app storage without touching the existing app.

Record the device/API, exact APK hash, package, signer, initial empty state, actual Drive listing/download, first nonempty restore, second idempotent restore, recipe detail readback and restart. A releaseCheck result is evidence for the isolated minified variant; it is not evidence that the exact production-package APK was installed or can upgrade a debug installation. Do not infer account identity or successful recovery merely from an OAuth picker/connected label. Personal warnings and account login remain user-controlled.

See `.planning/audits/2026-10-18-signed-release-recovery.md` for the observed acceptance checkpoint and limits.

When the user explicitly authorizes replacing a debug installation on a named
device, first preserve its installed APK and a consistent database/WAL/SHM
snapshot privately. Verify file hashes and SQLite integrity on separate copies,
and create/verify a current Drive snapshot of valid recipes before uninstalling
only the authorized package/profile. An unrelated debug certificate cannot be
upgraded in place. Never treat an old cloud snapshot as proof that all current
local records are covered. Malformed legacy records must remain preserved even
when they cannot pass the strict recipe-backup format.

The subsequent actual production-package installation and Drive recovery on the
S25 passed; see `.planning/audits/2026-10-18-production-release-recovery.md`.

Version 2.1.0/code 2 reuses the same release identity and includes the visual
refresh and original-video button. The exact production APK passed fresh
four-recipe recovery, idempotent repeat, restart, scaling and UI/source-link
checks on the Mi 11. See `.planning/audits/2026-10-23-mi11-release-2.1.0.md`.
MIUI's fresh USB install restriction was handled through the normal Files/package
installer with successful Play Protect and Xiaomi scans, without disabling
protections.

GitHub release publication is a separate explicit operation after build/device
verification, not a side effect of `release.py`. Versioned notes and artifact
checksums are tracked in `docs/releases/`; signed binaries are attached only to
the GitHub release, never committed to source history. Do not attach keys,
credential files, recipe backups or private screenshots. The AAB is an optional
distribution artifact, not an automatic Play-store submission.
