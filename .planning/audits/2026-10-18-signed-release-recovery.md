# Signed Release And Second-Device Recovery

**Later production acceptance:** The subsequent explicitly authorized replacement
and actual production-package recovery passed. See
`2026-10-18-production-release-recovery.md`. The checkpoint and limits below
describe the earlier isolated-variant test.

## Scope And Artifact Identity

Runtime/tooling commit: `f82c2e4`. Local JDK 17 build runs unit tests, lint, signed minified production APK/AAB and isolated releaseCheck APK. No release was published to Play/GitHub Releases, and no signing credentials were uploaded.

| Artifact | SHA-256 |
| --- | --- |
| Production APK (`com.recipegrabber`) | `c7872497a213ad3d17c3446c03692d86fe3ac542a9629e09f94f7093d18b9d66` |
| Release-check APK (`com.recipegrabber.releasecheck`) | `0fa7ac3c7050029a5d45075dde288113610de7c4bc2cfb87f90b0ec87f25af82` |
| Production AAB | `1fc36aae1ace5484fbaf4987a0be49cf1ab8f7b09fc2ea4e2f53ecf08fac757a` |

Public release certificate SHA-256: `62cf463e5f183178bd60cf7079891157a793c90bc582e2087195eed21c43cf65`. APK signature verification (`apksigner`) matches the configured keystore certificate (`keytool`); exact package IDs and non-debuggable flags are checked with `aapt`. Bundle JAR signature (`jarsigner`) and embedded certificate are verified independently too. This is signing identity evidence, not a Play Store publication or a guarantee of reproducible byte-for-byte builds on other hosts.

## Signing And Regression Gates

- New local RSA-3072/PKCS12 identity is owner-only in a private directory outside the repository. Ignored `.env.release-signing` is mode 600; no password/key contents are printed, stored in documentation or committed. No existing signing identity was overwritten. An off-device encrypted key backup is still an operator responsibility.
- Explicitly required signing without environment configuration fails during Gradle configuration; incomplete configurations also fail closed. Ordinary unsigned public CI remains supported.
- Ten Python tests cover config completeness, duplicate fields, symlinks, relative paths, permissions, certificate mismatch, wrong package/variant, debuggable rejection, unsigned bundle and withheld subprocess output. A real CLI verification of the releaseCheck APK as the production package correctly failed.
- 150 JVM tests pass; local lint and APK/AAB assembly pass. Independent fresh review found no Important/Critical issues; its verifier/coverage concerns were addressed with exact variant checks and artifact regression tests.
- CI now runs release tooling tests and assembles releaseCheck, still without signing credentials. [Android Checks 39102779893](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/39102779893) for `f82c2e46c87c9c5a32f1d9dd0244123c45765935` completed successfully: build/unit/lint/releaseCheck/tooling plus both API 31/35 instrumentation jobs. External OAuth/Drive probes remain gated; the physical S25 acceptance above was performed separately. The final evidence update is documentation-only and does not change the tested runtime/tooling.

## Device And OAuth

- Physical second device: Samsung S25 Ultra, SM-S938B, Android 16 / API 36, reached using normal paired TLS wireless debugging. Pairing codes, network addresses, Google account identifiers and cloud file IDs are deliberately excluded.
- The existing production-package app is debuggable. It and the preexisting audit app were not uninstalled, replaced or cleared. The new minified, non-debuggable releaseCheck package was installed alongside them, and its first recipe-list screen explicitly showed `No recipes yet`.
- With specific user confirmation, two Android OAuth clients were created in the existing Google Cloud project for the new certificate: production package and releaseCheck package. Existing clients remained; no IAM, billing, scope or consent-publication changes were made.
- The isolated app authorized Drive using the existing account without another unverified-app warning. Settings loaded both actual snapshots from 13 October, each with three recipes. This verifies cross-client visibility in this project's appDataFolder, not merely an account-picker result.

## Observed Second-Device Acceptance

- The first confirmed restore downloaded an existing Mi 11 snapshot and reported `Restored 3 recipes; 0 existing recipes kept`. No host fixture, debug database or manual recipe insertion was used.
- The same snapshot was restored again and reported `Restored 0 recipes; 3 existing recipes kept`.
- After force-stop/relaunch of only the isolated test package, all three cards remained: each had four ingredients, with eight/seven/five steps respectively. Their recipe detail screens opened independently; ingredients and preparation instructions were present. The seven-step detail was scrolled through to its final visible step, and the five/eight-step details were checked too.
- On the eight-step recipe, portion controls changed four to five portions and scaled the ingredient display proportionally, then returned to four. A screenshot of the restored list verified readable, non-overlapping cards. No releaseCheck crash entry was observed in the device crash buffer during the check.
- Installed APK SHA-256 matched the releaseCheck artifact above exactly. Android package flags did not contain DEBUGGABLE. The production-package APK path on the S25 remained the same as before testing; neither original app was uninstalled/cleared/replaced.
- Re-running signing initialization retained the existing identity, and both independent APK verifications still matched the recorded hashes. No private signing file is tracked in Git. Temporary device UI dumps and host account-selection/Cloud text captures were removed after testing; the installed isolated app and restored recipes were retained for the user.
- One automated navigation sequence raced a transition and did not open the intended detail. The check was repeated with fresh UI observations between navigation actions and passed; no application change or security workaround was needed.

This is actual cross-device cloud download/nonempty restore/idempotence/restart evidence for the explicitly identified minified variant, not a conclusion drawn from local tests or OAuth alone. No new cloud backup, snapshot deletion, API generation request or paid-provider call was needed on the S25.

## Limits

The exact production-package APK has **not** been installed on the S25: its unrelated signing key cannot upgrade the existing debug-signed installation. The isolated minified variant differs by application ID/label/version suffix and is explicitly identified as such. No destructive app removal or production update compatibility is claimed. System backup/device transfer, old Room 1/2 upgrade chains, Play distribution and every provider/media combination remain outside this acceptance.
