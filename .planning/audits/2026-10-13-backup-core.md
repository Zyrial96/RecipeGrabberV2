# D-01 Backup Core And Non-Destructive Restore

## Contract

- Format `recipegrabber-recipes`, version 1. Maximum 2 MiB and 500 recipes, bounded nesting/node counts and typed field/text/numeric limits. Strict UTF8 and JSON preflight reject malformed input, duplicate object fields, duplicate recipe UUIDs, trailing content and unsupported versions before any database writes.
- Explicit fields preserve structured recipe content, favorites, timestamps and safe HTTPS source/image links. SQLite row/child IDs, sync flags, preferences, credentials, OAuth tokens, account metadata and diagnostics are not serialized. Encoder uses a bounded UTF8 writer and validates the resulting archive too.
- Room version 4 adds a unique stable `backupId` UUID. Migration 3-to-4 preserves existing row IDs, recipes, children and favorites while assigning UUIDs. No destructive fallback was introduced.
- Coherent DAO snapshots use transactions. Merge executes the whole archive in one transaction, assigns fresh local row/child IDs and skips already-present UUIDs. Local records win even when an older snapshot differs. Distinct recipes with identical titles/URLs remain distinct. Direct insert conflicts abort rather than invoking SQLite REPLACE and cascading away an existing aggregate.

## Verification

- 140 JVM tests pass, no failures/errors/skips; six new codec tests cover content/identity round-trip, field exclusion, versions, duplicate identities/fields, invalid rows/URLs, size/count limits, malformed UTF8, strict JSON/depth and empty archives.
- Full JDK 17 unit/lint/debug/test/release and audit assembly succeeded (`/tmp/recipegrabber-backup-core-build.txt`). Audit instrumentation is built with `./gradlew -PqaTests=true assembleAuditAndroidTest`; default debug instrumentation and hosted CI remain unchanged.
- Isolated audit variant `com.recipegrabber.qa`, label `RecipeGrabber QA`, was installed on the Mi 11. It has separate app data and Keystore UID from `com.recipegrabber`, so preflight does not upgrade/reset the real recipe library. No QA OAuth registration or live API probe was requested.
- Final QA suite: twenty-two passed, three external opt-in probes skipped, 13.659 seconds (`/tmp/recipegrabber-backup-core-qa-tests.txt`). Five new native tests verify repeat-restore idempotence, preserved distinct duplicates, local conflict precedence, whole-batch rollback and migration of two duplicate-content version-3 recipes with their original child rows. The fixture is constructed from the actual exported `3.json` schema, not an assumed table definition.
- Deliberately reverting insert conflict handling to REPLACE made the targeted identity-collision native test fail (`/tmp/recipegrabber-backup-core-red-test.txt`). Restoring ABORT returned all native tests to green.
- Final audit APK SHA-256 `ed0ac8a50a466c06c32652e92fdc0918d6ee7b6dbb43d499ec6da87a79267b8c`; audit test APK `64f75eef25bcc8d732f8128ed1a1c8e15dc6c489a194d73c6150c26c8ec98592`; installed hashes matched builds. Installation server/reverse were stopped.

## Historical D-01 Checkpoint

The actual installed main app remains the S-02/S-03 checkpoint with its version-3 library. Main-app migration/restart, real Drive upload/list/download/restore, usable progress/error/confirmation UI and final hosted CI are still required under the active goal. Backup transport and UI are not claimed from codec/QA success. Prior 1-to-2/2-to-3 migration coverage is not established by the new 3-to-4 fixture.

**D-02/D-03 follow-up:** Those required integration gates are now verified in the [Drive audit](2026-10-13-drive-integration.md): main schema-4 content preservation, real cloud round trip, confirmed UI actions, independent Drive disconnect and hosted API 31/35 CI. The main app is no longer at the historical schema-3 checkpoint above. A later audit rebuild exposed stale generated REPLACE code from the mutation test; forced KSP regeneration restored ABORT in all variants and the installed final QA/main suites passed. Source restoration alone was not sufficient cache invalidation evidence.
