---
last_mapped_commit: 3dfcf75a166b1af026a710d6e54c8de6b1c9c126
analysis_date: 2026-10-13
---

# Codebase Concerns

**Analysis Date:** 2026-10-13

**Full-map baseline:** `/Users/zyrial/Projects/RecipeGrabberV2` at `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`. Focused security/backup follow-up verified through `575b965dbf91bbd32d04762dab40a281127363d4`. Historical extraction observations remain scoped to their original audits.

**Last mapped commit:** `3dfcf75a166b1af026a710d6e54c8de6b1c9c126`.

**Verification checkpoint:** 150 JVM tests pass; twenty-five QA native passes with four external probes skipped. Keystore migration, bounded diagnostics, backup rules, UUID conflicts/rollback and Room 3-to-4 fixture migration have tests. Main Room upgrade preserved all recipe data. Real cloud acceptance/hosted CI status is recorded separately in `../audits/2026-10-13-drive-integration.md`; tests alone do not prove it. Prior media observations remain unchanged.

## Tech Debt

**Provider-specific extraction remains duplicated:**
- Issue: OpenAI, Claude, Kimi, and Gemini each own request construction, prompt handling, parsing, and mapping. Only Gemini has verified native video `fileData` handling.
- Files: `app/src/main/java/com/recipegrabber/domain/llm/OpenAiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/ClaudeProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/KimiProvider.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`
- Impact: Schema, timeout, redaction, cancellation, and capability fixes can drift.
- Fix approach: Centralize provider-neutral prompt/schema validation and expose explicit URL/file/video capabilities.

**Resolved: explicit Drive backup/restore coordinator:**
- `DriveBackupAuthorization`, `DriveBackupTransport` and `DriveBackupCoordinator` now back Settings/onboarding. Manual snapshots, bounded listing/download, validated transactional UUID merge, explicit confirmation and progress/result states exist. Existing UUIDs win conflicts; disconnect does not affect Gemini.
- Automatic sync/scheduling/deletion propagation remain intentionally absent. Cloud acceptance is distinct from connection state and documented in the Drive audit.

**List search loads all detailed data before filtering:**
- Issue: `RecipeListViewModel` collects every recipe with ingredients and steps and filters in memory; the DAO search query is unused by the display path.
- Files: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeListViewModel.kt:45-69`, `app/src/main/java/com/recipegrabber/data/local/dao/RecipeDao.kt:29-34`
- Impact: Room work, memory, and recomposition grow with recipe count.
- Fix approach: Query summary rows in Room, debounce input, and load details only for visible/detail screens.

## Known Bugs

**Resolved: persisted ingredient-count display:**
- At `cd783e4`, `RecipeCard` counts `recipeWithDetails.ingredients`, not the embedded transient field. Two Compose regressions failed before and pass after the fix. All three real imports now display four ingredients after restart; no schema/data rewrite was needed. See `.planning/audits/2026-10-13-recipe-list-verification.md`.

**Direct-file acceptance is specific, not universal:**
- After personal Google confirmation, the optional Files scope succeeded. A 21,223,793-byte public WebM reached ACTIVE, produced four ingredients/eight steps, saved with its original source and reopened after restart. The earlier large-file permission/acceptance blocker is resolved for this case; MP4/MOV, other origins and all provider/model combinations remain unverified.
- Historical failure: Google's direct URL fetcher returned HTTP 400 with `URL_UNREACHABLE-UNREACHABLE_5xx` for Commons media. The subsequent bounded inline/Files transfer avoids relying on that fetcher.
- Historical permission failure: retriever-only Files upload start returned HTTP 403 / `ACCESS_TOKEN_SCOPE_INSUFFICIENT`. The implemented separately optional combined-scope flow resolves this; the default YouTube/inline scope remains retriever-only.
- The small inline version separately saved four ingredients/seven steps without extra permission. Larger files retain the 100 MiB cap; this cap is not proof that every file below it works.
- Files: `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt`, `app/src/main/java/com/recipegrabber/domain/llm/GeminiProvider.kt`, `app/src/test/java/com/recipegrabber/VideoLinkParserTest.kt`
- Trigger: Extract a real remote MP4/WebM/MOV URL through each supported provider and device entry point.
- Workaround: Use the verified public YouTube plus Gemini Flash-Lite flow.

**Lifecycle fixes are unit-proven but broad device coverage remains deferred:**
- Symptoms: `RecipeExtractionViewModel` owns and cancels the active `Job`; the sheet resets through `DisposableEffect`; three lifecycle regressions are green. Broader lifecycle/device combinations are not covered.
- Files: `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`, `app/src/main/java/com/recipegrabber/presentation/ui/screens/RecipeExtractionBottomSheet.kt`, `app/src/test/java/com/recipegrabber/RecipeExtractionLifecycleTest.kt`
- Trigger: Configuration changes, process recreation, repeated share intents, or dismissal during provider/network completion.
- Workaround: None beyond the tested cancellation behavior.
- Status: Implemented and unit-proven; retain as a verification limit, not an unfixed defect.

## Security Considerations

**Resolved: plaintext credential reads/writes:**
- `data/security/` implements Android Keystore-backed AES-256-GCM and atomic five-key migration. Failed encryption preserves legacy input for recovery, but providers never read that plaintext fallback. Key loss/tampering fails closed with visible state. Synthetic JVM/native tests cover migration and cryptography.
- Residual risk: plaintext is necessarily present in process memory during use; failed migration retains legacy bytes, and historical disk/backup copies are not securely erased. Keystore does not protect a compromised app process.

**Resolved: broad sensitive diagnostic persistence/export:**
- Call sites omit recipe/account/source/response contents; `DiagnosticSanitizer`, `PrivateLogStore` and `AppLogger` sanitize before logcat/queue/disk, replace legacy unknown diagnostic contents, and bound queue/retention/export. Throwable messages/stacks are excluded; only class metadata remains.
- Sharing exposes one explicit diagnostic cache directory, not arbitrary cache/raw logs. Previous OS logcat, already shared exports and old external backups cannot be retroactively erased. No sanitizer guarantees recognition of every possible secret string; continue minimizing call-site data.

**Clipboard monitoring has permission and sensitive-data exposure:**
- Risk: A foreground service reads clipboard content and emits matching URLs. `POST_NOTIFICATIONS` is declared, but no runtime request is evident before service start.
- Files: `app/src/main/AndroidManifest.xml:5-8`, `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt:193-219`, `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`
- Current mitigation: Service is `exported=false`; startup is setting-gated.
- Recommendations: Request notification permission, centralize lifecycle, avoid clipboard payload logging, and verify clean Android 13+ installs.

**Resolved configuration; OS recovery still unverified:**
- Both platform backup rule formats allow recipe DB/WAL only, excluding keys/preferences/log/cache by allowlist. Cloud backup requires encryption support; FileProvider is restricted to diagnostic exports. XML and native content-URI tests are present.
- Actual Android system backup/restore and device transfer have not been exercised. Do not treat XML validation as OS recovery proof.

## Performance Bottlenecks

**Apify polling has a fixed two-minute upper window:**
- Problem: Instagram and TikTok poll 30 times with a two-second delay before result fetch.
- Files: `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt:55-84`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt:123-153`
- Cause: Duplicate loops lack a shared deadline, backoff, and status surface.
- Improvement path: Shared cancellable polling helper with bounded backoff and progress.

**Resolved: blocking/unbounded diagnostic export:**
- Sharing is suspend/IO-based, exports remain below 256 KiB and retention is at most four 256 KiB files. JVM/native privacy and limit tests cover the boundary.

## Fragile Areas

**Room migration history lacks matching historical schemas:**
- Files: `app/src/main/java/com/recipegrabber/data/local/Migrations.kt:6-85`, `app/src/main/java/com/recipegrabber/data/local/RecipeDatabase.kt`, `app/schemas/com.recipegrabber.data.local.RecipeDatabase/3.json`
- Why fragile: Schemas 3 and 4 are committed and 3->4 is native-tested; historical schemas 1/2 and full old upgrade-chain coverage remain absent.
- Safe modification: Export versions 1 and 2 and use `MigrationTestHelper` for every upgrade path.
- Test coverage: `RecipeBackupPersistenceTest` migrates an actual exported schema-3 fixture and verifies IDs/content/children/UUIDs. Main Mi 11 schema-3 data survived the upgrade; this does not establish old 1/2 paths.

**R8 acceptance is scoped, not universal:**
- Files: `app/proguard-rules.pro`, `app/build.gradle.kts:27-34`, `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/remote/apify/ApifyService.kt`
- Signed/minified non-debuggable releaseCheck and exact production APKs recovered real snapshots on S25 / API 36 with OAuth, JSON validation, Room writes, independent detail views and portion scaling. This covers that recovery path, not every provider/Retrofit flow or same-certificate update.
- Safe modification: Inspect R8 diagnostics, add only required rules, and install a minified APK for critical flows.
- Test coverage: `scripts/test_release.py` covers signer/package/non-debuggable failures; the real minified-device acceptance is recorded in the 18 October audit. Production-package update compatibility and broader provider release smoke tests remain unverified.

## Scaling Limits

**Local-first storage has no conflict/recovery model:**
- Current capacity: Recipes live in Room; `isSynced` is a boolean and no sync worker is wired.
- Limit: Multi-device edits, remote restore, deletions, and conflicts are not representable reliably.
- Scaling path: Add remote IDs, versions, dirty/deleted states, conflict policy, and durable attempts.

## Dependencies at Risk

**Google API client surface exceeds the active sync feature:**
- Risk: Legacy Drive Java-client helper methods remain unused while the active snapshot transport uses cancellable OkHttp. Broader dependency cleanup is a separate change.
- Files: `app/build.gradle.kts:72-74`, `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`
- Impact: Release size and shrinking complexity may exceed the active workflow's needs.
- Migration plan: Remove dormant legacy helpers/dependencies only after a dedicated compatibility review of Google authorization.

## Missing Critical Features

**Automatic synchronization and cross-device acceptance:**
- Manual backup/restore is implemented with UUID conflicts and transactions. No automatic scheduling/deletion propagation exists. Mi 11 plus isolated and exact production signed/minified S25 recovery are verified. Same-certificate release updates and Android system-backup recovery remain unverified. Malformed legacy failed-extraction records require separate preservation; they cannot pass strict recipe-backup validation.

**Native media ingestion for non-Gemini providers:**
- Problem: OpenAI, Claude, and Kimi accept URL-shaped input but have no live video-file ingestion proof.
- Blocks: Equivalent extraction behavior across provider selection.

## Test Coverage Gaps

**Extraction and URL-entry acceptance matrix is incomplete:**
- What's not tested: Every direct-file format/origin/provider combination and complete social scraper-to-persistence behavior. Two real WebM imports, shared/clipboard wiring, native payload/rejection tests, fallback error display and JVM lifecycle regressions have specific evidence; they do not establish the whole matrix.
- Files: `app/src/main/java/com/recipegrabber/domain/VideoLinkParser.kt`, `app/src/main/java/com/recipegrabber/presentation/MainActivity.kt`, `app/src/main/java/com/recipegrabber/service/ClipboardMonitorService.kt`, `app/src/test/java/com/recipegrabber/VideoLinkParserTest.kt`, `app/src/test/java/com/recipegrabber/RecipeExtractionLifecycleTest.kt`
- Risk: Network/CDN, process, and device-specific behavior can still diverge from unit-proven parsing and cancellation.
- Priority: High

**System recovery and notification gating remain unverified:**
- Keystore migration and export privacy/limits now have JVM/native behavioral tests. Actual OS backup/transfer and clean-install notification gating remain untested.
- Files: `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml`, `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/AndroidManifest.xml`
- Risk: Privacy regressions can pass current suites.
- Priority: High

**Broad lifecycle and security verification is deferred:**
- What's not tested: Clean-install notification permission flow, actual OS backup/transfer and process-death recovery across devices. Credential/privacy tests are no longer deferred.
- Files: `app/src/main/AndroidManifest.xml`, `app/src/main/java/com/recipegrabber/data/repository/PreferencesRepository.kt`, `app/src/main/java/com/recipegrabber/data/logging/AppLogger.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/RecipeExtractionViewModel.kt`
- Risk: Passing isolated tests does not establish every Android OS, backup or process-lifecycle behavior.
- Priority: High

**Drive acceptance is explicitly scoped:**
- Transport/coordinator/UI/merge tests exist; the gated live probe validates real upload/list/download and repeated non-destructive restore when enabled. See the Drive audit for observed acceptance rather than inferring it from the connected-account flag.
- Files: `app/src/main/java/com/recipegrabber/data/remote/GoogleDriveService.kt`, `app/src/main/java/com/recipegrabber/data/repository/RecipeRepository.kt`, `app/src/main/java/com/recipegrabber/presentation/viewmodel/SettingsViewModel.kt`
- Risk: Connected account or enabled setting can be mistaken for completed backup.
- Priority: Medium

---

*Concerns audit: 2026-10-13*
