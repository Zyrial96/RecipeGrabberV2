# Security And Drive Implementation Plan

> Execute inline with tested, focused commits; user authorized autonomous implementation.

**Goal:** Protect credentials and diagnostics, and deliver a real optional Drive backup/restore without losing existing recipes.

**Architecture:** Platform Android Keystore AES-256-GCM protects credential values in the existing DataStore; migration replaces plaintext fields atomically only after successful encryption. Recipes remain Room aggregates. Drive snapshots contain only versioned recipe data, never credentials, logs, OAuth tokens or account details. Restore validates the entire archive before one Room transaction and merges without overwriting existing recipes.

**Stack:** Existing Kotlin/Compose/Room/DataStore/Google Identity/Drive APIs, JDK 17, API 31 minimum. Prefer existing dependencies and platform cryptography.

## Constraints

- Preserve user recipes, unrelated changes and existing OAuth flows. No billing/IAM changes or silent extra permissions.
- Commit and push each verified coherent change. Use synthetic credentials in tests, never inspect/print real key values.
- Encryption loss/tampering fails closed and is visible; no plaintext read fallback or automatic key regeneration on decrypt.
- Legacy plaintext is removed only after all migration encryption succeeds. If encryption is unavailable, preserve the original data, expose an error, and do not use legacy keys; retry on restart or explicit replacement.
- Existing recipes win restore conflicts. Repeating a restore must not duplicate unchanged recipes. No cloud deletion is required for the first implementation.
- Keep tests against isolated databases/ciphers/accounts; native tests must not reset the real app database.

## S-01: Credential Storage

- [x] Add `data/security/SecretCipher.kt`: standard AES/GCM/NoPadding, random IV, versioned envelope, preference-name AAD. Android key provider creates only on encryption; decrypt requires the existing key.
- [x] Add `data/security/CredentialMigration.kt`: map five legacy API-key fields to separate encrypted fields in a DataStore migration. Successful migration preserves all unrelated preferences and removes legacy fields atomically. Failure preserves input unchanged and never returns legacy plaintext to callers.
- [x] Update `PreferencesRepository` reads/writes to encrypted fields, expose unavailable-credential state, and surface save failures safely in settings/onboarding.
- [x] JVM tests: round-trip, random ciphertext, wrong-field/key/tampered/invalid envelope, five-key migration, idempotence, empty keys, failure retention.
- [x] Native tests: real Android Keystore with isolated QA alias and preferences; production device update retains recipes/OAuth without a credential warning. Build/lint/debug/release verified; publication in the S-01 commit. Actual production key values were not inspected.

## S-02: Diagnostics And Platform Backup

- [x] Remove URL/account/title/raw-response/error-message logging at call sites. Central sanitization before logcat, queue and file export; throwables retain safe class metadata, not messages/stacks.
- [x] Bound queue, entry size, total retained bytes/file count and export size; replace unknown historical diagnostic contents at privacy upgrade, including the old cached export; make sharing suspend rather than blocking UI.
- [x] Restrict both platform backup-rule formats to intended recipe database/WAL data; exclude credentials/settings/logs/cache and require cloud encryption support. FileProvider exposes only diagnostic exports.
- [x] JVM/native tests: credential/email/URL/header redaction, raw-response suppression, legacy exports, limits, rotation, XML allowlists and asynchronous sharing. 134 JVM passes; seventeen native passes, three skips. Publication in S-02/S-03 commit.

## D-01: Backup Format And Transactional Merge

**Decision:** A stable per-recipe UUID and unique index require Room version 4. Migration 3-to-4 must preserve all version-3 data/IDs/relations and assign UUIDs; no destructive fallback. An isolated `audit` variant (`com.recipegrabber.qa`) permits native preflight on the Mi 11 before upgrading its real library. Restore matches UUIDs, not titles/URLs, so distinct duplicate recipes survive. Existing UUIDs always win; identity collisions during direct insertion abort rather than replace.

- [x] Add `domain/backup/RecipeBackupCodec.kt`: versioned JSON, 2 MiB/500-recipe limits plus structural/text/numeric bounds, explicit field allowlist, no settings/credential fields. Reject unsupported versions, duplicate IDs/fields and incomplete/malformed archives before database writes.
- [x] Add transactional DAO snapshot/merge. Reset database IDs for newly inserted aggregates, preserve source/content/favorites/timestamps, skip existing stable UUIDs and retain local edits. Room 3-to-4 assigns UUIDs without rewriting recipe data; direct identity collisions abort instead of replacing.
- [x] JVM/native tests: round-trip, unknown version, negative/oversized/malformed data, UTF8/depth/duplicate-key rejection, idempotent restore, distinct duplicates, conflict preservation, full rollback and real schema-3 fixture migration. 140 JVM passes and twenty-two QA native passes with three probes skipped; publication in D-01 commit. Main app upgrade waits for integration/acceptance phase.

## D-02: Drive Transport And Settings

- [x] Separate Drive AuthorizationClient flow requests only appDataFolder permission, verifies actual grant/token, and preserves Gemini authorization.
- [x] Transport creates versioned appDataFolder snapshots, lists only owned supported backups, downloads with limits, and propagates cancellation. No broad Drive scope, automatic overwrite, IAM role or cloud deletion.
- [x] Settings offers Backup now, archive selection, restore confirmation, progress/errors/result counts, and actual account/authorization state without implying automatic sync.
- [x] JVM/native tests for serialization/transport errors/cancellation and UI states. 150 JVM passes; twenty-five QA device passes and four external skips. Debug/release/lint pass; publication in D-02 commit.

## D-03: Real Acceptance And Documentation

- [x] On Mi 11: credential/privacy native checks pass without real key inspection; Room 3-to-4 retains all prior content; actual Drive upload/list/download and two idempotent restores preserve all three recipes. UI backup/confirmed restore and disconnect/Gemini independence are checked; restart retains recipe cards. No additional personal warning was shown.
- [x] Hosted runtime CI 38441275898 is green for build plus API 31/35. README/CONTRIBUTING/seven GSD map documents/audits updated with verified capabilities and limits; publication in final documentation-only evidence commit.
- [x] All required implementation/verification gates passed; goal completion follows successful final documentation publication. System-backup, second-device and minified-release limits remain explicit, not falsely claimed as verified.

## Findings

| ID | Finding | Classification |
| --- | --- | --- |
| S-01 | Five plaintext DataStore credentials | Implement with fail-closed migration |
| S-02 | Persisted/exported sensitive logs and unbounded retention/export | Implement sanitization, call-site minimization and bounds |
| S-03 | Broad shared-pref backup rules without explicit credential policy | Replace with a narrow recipe-data policy |
| D-01 | Drive helper methods do not provide actual backup/restore | Implement versioned snapshots, merge and UI |
| D-02 | Actual Google account consent and recovery evidence | External verification; personal warning cannot be bypassed |
