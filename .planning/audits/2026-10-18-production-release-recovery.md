# Actual Production Release Recovery On S25

## Scope

Following explicit authorization to replace the original app on the S25 Ultra,
the exact production APK was installed and tested on SM-S938B, Android 16/API 36,
Android user 0. This closes the production-package installation and recovery gap
from `2026-10-18-signed-release-recovery.md`; it is not an in-place upgrade from
an unrelated debug certificate or a Play distribution test.

Runtime/tooling remains `f82c2e4`. Only acceptance documentation changed.

## Backup Before Replacement

- Original production and audit database/WAL/SHM files and installed APKs were
  saved privately, outside Git, as described in
  `2026-10-18-production-install-preflight.md`. All eight raw file hashes matched
  again immediately before replacement. Validation copies passed SQLite
  integrity and foreign-key checks.
- The original app contained one failed-extraction record, without ingredients
  or steps and with an invalid source URL, rather than a usable recipe. It
  remains in the raw database backup; it was not silently represented as a
  restored recipe. The audit database was empty.
- After the user personally unlocked the phone, releaseCheck visibly contained
  all three previously recovered recipes. Its normal Drive backup action
  reported `Backup saved: 3 recipes`. The fresh snapshot was listed at
  `2026-10-18T06:21:45.435Z`, alongside both older three-recipe snapshots.
- No snapshot was removed. No credentials, account identifiers, recipe contents,
  raw database files or signing material were committed to the public repository.

## Exact Production Installation

- Only the original `com.recipegrabber` installation for user 0 was uninstalled,
  after the backups above. Audit and releaseCheck were not cleared or replaced;
  other Android profiles were not targeted.
- ADB successfully installed `app/build/outputs/apk/release/app-release.apk`.
  Onboarding began with an empty recipe list (`No recipes yet`). No database
  injection or host recipe fixture was used.
- Google Drive authorization succeeded in the actual production package using
  its already registered release-certificate OAuth client. No new Cloud client,
  scope, IAM, billing or consent-publication change was needed.
- The installed base APK was read back and hashed. Its SHA-256 exactly matches
  `c7872497a213ad3d17c3446c03692d86fe3ac542a9629e09f94f7093d18b9d66`.
  Release verification confirms the expected signer and package. Device package
  flags contain no DEBUGGABLE flag; version is 2.0.0/code 1, min SDK 31/target 35.

## Observed Recovery And Readback

- The production app listed the fresh snapshot and both historical snapshots.
  Restoring the fresh snapshot reported
  `Restored 3 recipes; 0 existing recipes kept`.
- Repeating that same restore reported
  `Restored 0 recipes; 3 existing recipes kept`.
- After force-stop/relaunch, exactly three recipe cards remained. Each had four
  ingredients, with eight, seven and five preparation steps respectively.
- All three independent detail screens opened. Ingredients were visible and
  every preparation sequence was scrolled through to its final step (8/7/5).
- On the eight-step recipe, increasing four portions to five scaled the visible
  amounts by 1.25 (including 200 ml to 250 ml). Returning to four restored the
  original quantities. No recipe was deleted during testing.
- A private screenshot of the final production recipe list was visually checked:
  cards and controls render without overlapping. Long list text is ellipsized;
  the full details remain available on opening each recipe.
- No exact production-package process entry was present in the available crash
  buffer. An unrelated historical fatal-exception line was not attributed to
  this app. This observation is not exhaustive crash or performance coverage.
- Ten Python release-tooling tests pass again. No runtime change required a
  rebuild; the installed binary is the previously built and verified artifact.

## Retained State And Limits

The real release app is installed with all three restored recipes. Audit and
releaseCheck remain installed, and private raw backups plus the fresh Drive
snapshot are retained. Temporary on-device UI captures were removed.

The malformed legacy failed-extraction record is preserved locally, not imported
into the recipe collection. Signing-key off-device backup, Play distribution,
same-certificate update testing, Android system-backup restore and broader
provider/media/lifecycle acceptance remain separate work. This is a successful
actual production-package manual Drive recovery test, not a full production-
readiness certification.
