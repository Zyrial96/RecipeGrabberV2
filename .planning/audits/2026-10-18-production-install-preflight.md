# Production Installation Preflight

This follow-up records the request to replace the original debug-signed app on
the explicitly named Samsung S25 Ultra with the actual signed production APK.
It does not supersede the isolated-variant acceptance results in
`2026-10-18-signed-release-recovery.md`.

## Verified Before Replacement

- Wireless ADB identifies the target as SM-S938B. Two transport aliases refer
  to this same phone, not two separate devices. Operations target one explicit
  transport and Android user 0; other profiles are not touched.
- Three installed packages were found: production, audit and releaseCheck.
- The original production and audit apps were force-stopped for consistent
  private database snapshots. Their installed APKs and all three SQLite files
  (database, WAL and SHM) were copied into owner-only storage outside Git.
- The definitive raw backup directory is
  `~/.local/share/recipegrabber-backups/2026-10-18-s25-production-raw/`.
  Each package has a SHA-256 manifest covering the APK and database files;
  all eight saved file hashes match. Validation uses separate working copies
  so SQLite cannot checkpoint or change the raw evidence.
- Both database copies pass `integrity_check` and `foreign_key_check`.
  The original database has one previously saved failed-extraction record,
  with no ingredients or steps. The audit database has no recipes. The
  failed-extraction record remains preserved in the raw backup; it is not
  represented as a successfully extracted recipe or a valid Drive snapshot.
- Original and current debug certificates differ. No debug update was attempted.
  The production release uses a third, deliberately established release identity.
- Release tooling independently verifies the exact production package,
  non-debuggable manifest and expected signing identity. APK SHA-256 remains
  `c7872497a213ad3d17c3446c03692d86fe3ac542a9629e09f94f7093d18b9d66`.
- All ten release-tooling unit tests pass again. No runtime source was changed.

## Current Gate

The device requires its personal unlock pattern. Opening the releaseCheck app
and normal `wm dismiss-keyguard` do not dismiss that secure lock. The user was
asked to unlock the phone once; no credential guessing or lock bypass is used.

The additional fresh Drive backup of the three recipes previously observed in
releaseCheck has **not** been created in this follow-up. Its current recipe
contents cannot be re-observed through the locked UI. The existing historical
Drive snapshots and prior isolated acceptance are not substituted for that check.

No package has been uninstalled, replaced or cleared. Actual production
installation and production-package restore/idempotence/restart tests remain
pending. These preflight checks alone are not a successful restoration test.

## Resume After Unlock

1. Re-observe releaseCheck recipes and create a fresh Drive snapshot; verify
   its recipe count and successful upload. Preserve existing snapshots.
2. Re-check raw backup hashes and exact target before replacing only the
   original user-0 production app with the verified production APK.
3. Authorize Drive in the real production package and restore the verified
   nonempty snapshot. Repeat restore to verify duplicate protection.
4. Inspect ingredients, steps and portion scaling; force-stop/relaunch and
   verify persistence. Compare the installed APK hash and package flags.
5. Record actual results and commit/push updated evidence. Retain private
   backups and the untouched audit/releaseCheck installations.
