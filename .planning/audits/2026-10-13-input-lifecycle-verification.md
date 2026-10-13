# Input And Lifecycle Verification

## Scope And Source

- Actual checkout: `/Users/zyrial/Projects/RecipeGrabberV2`, branch `main`; the old Desktop location is not current.
- The user's later device instruction replaces the goal's historical Samsung reference: target is Xiaomi Mi 11, Android 12 / API 31. No other device was used.
- Production fixes: `48b5fc4` (shared input) and `8324c13` (extraction lifecycle). Diagnostic update: `b510ee5`. Mapping source checkpoint: `8b01356`.
- All seven GSD documents were refreshed by the four mapper roles, reviewed against source and current results, date/commit stamped, checked for nonempty output and scanned for secret patterns. Generated Kotlin build cache is ignored.

## Implemented And Tested

- `VideoLinkParser` replaces duplicated substring matching in MainActivity and ClipboardMonitorService. It recognizes HTTPS YouTube watch/shorts/live/embed links, direct MP4/WebM/MOV links, and existing social-scraper entry points inside shared text. Credential-bearing URLs, HTTP URLs and lookalike hosts are rejected. Clipboard payloads are no longer logged by that entry point.
- Seventeen new JVM cases cover link recognition, media request compatibility and rejection. Two new native tests validate Shorts/direct-media payloads and unsafe input on Android without network requests. They do not claim that dummy fixture origins are downloadable.
- The extraction sheet's ViewModel now cancels its previous Job on replacement/reset; DisposableEffect resets it when the sheet leaves composition. Progress collection is cleaned up in finally. Direct scraping fallback displays its actual follow-up error.
- Three lifecycle tests failed against the preceding implementation and passed after the fix: cancellation on dismissal, replacement cancellation and correct fallback-error routing. These are coroutine boundary tests, not a claim of exhaustive process-death or orientation coverage.
- All 88 JVM tests pass with zero failures/errors/skips. JDK 17 `testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease` succeeded (`/tmp/recipegrabber-file-diagnostic-build.txt`). `git diff --check` passed.
- Installed production APK SHA-256 `a52ed41e912fb25fe219ec6f445aaf5c3ddaefb9918d0d4a3e78b9206e674b65` matched the build without clearing data. Installed final test APK SHA-256 `3e7ce5b30c18717d2834d7146cc5fe5e89088de4598bdccfa2718a67e8fdf6ae` matched the diagnostic build.
- Final Mi 11 default suite: ten passed, three external probes skipped, 8.918 seconds. Thirteen discovered tests must not be described as thirteen passes. No paid API calls run in default CI or this suite.

## Real Direct-File Failure

- Additional source: [Patata tortilla.webm](https://commons.wikimedia.org/wiki/File:Patata_tortilla.webm), Elene B, CC BY-SA 4.0, original cooking demonstration, 159.282 seconds. No copy or derivative was committed.
- Exact public file used: `https://upload.wikimedia.org/wikipedia/commons/7/73/Patata_tortilla.webm`. A read-only Mac HEAD request returned HTTP 200, `video/webm`, 7,482,355 bytes. It is below the 15MB external-file limit in [Google's current input guide](https://ai.google.dev/gemini-api/docs/file-input-methods).
- Sent the direct file through Android ACTION_SEND to the installed app. The new parser opened the extraction sheet; actual logs confirm Gemini 3.1 Flash-Lite. The request returned HTTP 400 in about 0.9 seconds, and the sheet displayed the failure. No successful parse/save was observed.
- One explicitly selected media diagnostic repeated that request with a tiny JSON output target. It correctly failed its assertion: HTTP 400, `INVALID_ARGUMENT`, approximately 999ms, with bounded explanation `Cannot fetch content from the provided URL... URL_UNREACHABLE-UNREACHABLE_5xx`. URLs are redacted from diagnostic explanations. This is not a passing test or an OAuth scope diagnosis.
- The Mac's HTTP 200 does not prove Google's fetcher can read that origin. Nor does one inaccessible origin prove all direct files fail. No automatic retries, extra scopes, billing changes or model substitution were introduced.

## Completion Checkpoint

| Requirement | Evidence | Current status |
| --- | --- | --- |
| Thorough current code map and actionable concerns | Seven refreshed `.planning/codebase/` documents with source checkpoint and concrete paths | Verified |
| Central persistence/input/lifecycle regressions fixed | Current source, 88 JVM tests, native Room/Compose/input tests | Verified for described changes; broader lifecycle/migration gaps documented |
| Reproducible local build/test/release commands | JDK 17 wrapper build and reports | Verified locally |
| CI on supported minimum/current APIs | Hosted run 38385620263 at 9b875e3 completed successfully for build and API 31/35 | Verified for the source checkpoint; no live authorization/inference runs in CI |
| Google OAuth and real YouTube extraction/persistence/scaling | Positive Mi 11 acceptance in `2026-10-13-google-oauth-registration.md` | Verified for one public video and Flash-Lite |
| Supported direct-file extraction and error paths | Native payload/rejection tests plus actual ACTION_SEND/HTTP-400 failure | Entry/error handling verified; positive remote-file extraction remains open |
| Honest README and contributor instructions | Current README/CONTRIBUTING with external prerequisites, tests, limitations and no invented metrics | Verified |
| Focused commits published to GitHub | Source commits pushed to main; map/audit publication follows this checkpoint | Verify clean synchronization after final documentation commit |

The full goal is not marked achieved at this checkpoint. The remaining media acceptance must be resolved or genuinely blocked; documentation and synthetic fixtures do not substitute for a successful direct-file import. Investigate a robust Files API ingestion path or a demonstrably accessible origin without repeatedly retrying the same failing URL. Do not infer a universal OAuth, quota or billing failure from this response.

## Later Acceptance Follow-Up

The historical remaining direct-file gate above is now satisfied for two real WebM transfers: the 7.5MB inline import and, after personally granted optional OAuth permission, the 21.2MB Files import. Both saved structured recipes and survived restart on the user's Mi 11. The larger import reached ACTIVE and saved four ingredients/eight steps. See `2026-10-13-files-api-verification.md` for the exact source, timestamps and persistence evidence. A separately discovered persisted ingredient-count display bug was fixed and verified at `cd783e4` with two red/green Compose regressions; the current suite has 121 JVM passes, twelve native passes and three external skips. Remaining broad format/provider/lifecycle/security limits are explicitly documented, not hidden by these positive cases.

## Final Objective Verification

- The user's newer device instruction supersedes the original Samsung wording: final device evidence is from the connected Xiaomi Mi 11, Android 12/API 31.
- Current runtime `cd783e4c4154f900a614eacd1776aee4f8fa8da6` is green locally and in hosted run [38421782788](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38421782788) for build and Android 31/35 instrumentation. Native external API probes remain opt-in; the real authorization/generation tests are separately recorded.
- The seven GSD map documents exist; focused integration/architecture/test/concern updates record the subsequent optional OAuth and persisted-card changes. README and CONTRIBUTING explain reproducible commands, external prerequisites, actual capabilities and limitations without invented user/download metrics.
- Central persistence/input/cancellation/display regressions are covered, all three real imported recipes remain saved, and the final on-device APK matches its recorded build hash. No user database clearing, billing/IAM changes or silent permission expansion occurred.
- Focused code commits and acceptance evidence were pushed to the public repository. The last evidence/map commit is checked to contain no runtime/test/build/workflow changes against the green source checkpoint.
- The bounded maintenance/testability/contributor-readiness objective is satisfied; this is not a production-readiness certification. Drive backup/restore, secret-storage/log-redaction hardening, broad lifecycle/migration coverage and all social-platform/provider/format combinations remain explicit next-phase work, not claims of completed functionality.
