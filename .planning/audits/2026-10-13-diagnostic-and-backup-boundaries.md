# S-02/S-03 Diagnostic And Backup Boundaries

## Diagnostics

- Removed actual source URLs, account addresses, recipe titles, scraper run IDs, arbitrary model labels, raw provider responses and exception messages from production log call sites. Error type and controlled status/count metadata remain useful without response/stack payloads.
- Central sanitization runs before logcat/queue and again before persistence/export, removing URL/email/header/common-key/JWT/named-secret forms. Raw-response and oversized messages are omitted; control characters cannot forge new log entries. Pattern matching is defense in depth, not a promise to recognize every arbitrary secret someone might add to a future log call.
- During the first privacy upgrade, existing app-owned log contents and the old cached export are replaced with a non-sensitive notice. Unknown old payloads cannot be reliably repaired by regex. This intentionally loses old diagnostic details, not saved recipes or API credentials. The production privacy marker was observed on the Mi 11. Startup cleanup runs asynchronously on IO, even if the user does not open the viewer.
- Queue: at most 256 records. Sanitized messages: at most 1,024 characters, with oversized input omitted. Files: at most four app-owned logs of 256 KiB each. Export: at most 256 KiB including header. No device model or account data is included in the export header.
- Sharing now suspends on IO instead of runBlocking on the UI thread. Only the dedicated diagnostics cache subtree is exposed by FileProvider; raw files/logs and arbitrary cache entries are not mapped. Export intents grant read permission only and are not automatically sent to an external recipient.

## Platform Backup

- Legacy and Android 12+ rules include only `recipe_grabber_db` and its WAL; other domains/files, including DataStore preferences, legacy Google shared preferences, logs, keys and cache, are excluded by the explicit allowlist. SQLite rebuilds its shared-memory file rather than transferring it.
- Legacy cloud inclusion requires client-side encryption; Android 12+ cloud backup sets `disableIfNoEncryptionCapabilities=true`. Device transfer is recipe-data-only. Android shuts the app down during system backup; see the [official Auto Backup guide](https://developer.android.com/identity/data/autobackup).
- Keystore keys/credentials are not portable. New-device setup/reinstall may require entering API keys and authorizing Google again. XML/source tests prove the configured boundary, not a completed live Android system-backup round trip.

## Evidence

- 134 JVM tests pass with zero failures/errors/skips. Seven new tests cover sanitizer/store behavior and exact backup/FileProvider allowlists. Deliberately bypassing sanitization made two diagnostic tests fail; restoring it returned the full suite to green (`/tmp/recipegrabber-diagnostics-red.txt`). Full JDK 17 unit/lint/debug/test/release assembly passes (`/tmp/recipegrabber-diagnostics-build.txt`).
- Final Mi 11 suite: seventeen passed, three opt-in external probes skipped, 13.258 seconds (`/tmp/recipegrabber-diagnostics-device-tests.txt`). Two native tests use isolated QA directories to verify legacy/new-log privacy, exception-message removal, bounded export and readable content-URI sharing. They do not inspect user credentials or share diagnostics externally.
- Main installed APK SHA-256 `039aeddd74b4f50f6109aeb8683e7b7e0eec35b8941d0c54b902351dceaeb005`; test `e26ae6772ab000617e308454c0eb3b533c34d5f9f89b906278d62baa764d26d0`; hashes matched builds. The preceding on-device check retained all three imported recipes/counts, and the final change adds only asynchronous startup cleanup.
- Installation server/reverse were stopped; temporary UI dump was removed. Historical Android/system logcat buffers, already-shared exports and previously stored cloud backups are not retrospectively erased. No raw existing log/credential contents were printed during verification.
- Read-only Google Cloud CLI check with explicit `--project=recipe-grabber-v2` confirmed `drive.googleapis.com` already enabled. No Cloud configuration, global selected project, billing or IAM changed.

Actual Drive snapshot/restore implementation and real acceptance remain required under the active goal; this checkpoint is not goal completion or production-security certification.
