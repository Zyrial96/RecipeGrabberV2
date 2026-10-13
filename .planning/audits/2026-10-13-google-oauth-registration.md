# Google OAuth Registration

## Verified Change

- The user approved creating the prepared Android OAuth client before the final console submission.
- In the existing Recipe Grabber V2 Cloud project, the previous Android client registered `com.recipegrabber` with a different signing fingerprint. It was left unchanged, along with the existing web client.
- Created a separate Android client named `RecipeGrabber - Mac Debug` for package `com.recipegrabber` and public SHA-1 fingerprint `6A:34:6C:6E:B4:C2:1C:A7:CA:0B:18:8C:F0:64:88:8E:AB:D4:1F:B7`.
- The console displayed `OAuth-Client erstellt`; the client list subsequently showed the new Android entry dated 13 October 2026 alongside the previous clients.
- No client secrets were downloaded. Billing, browser profiles, Drive permissions and global CLI configuration were not changed.

## Device Evidence

- Xiaomi Mi 11 was the only connected ADB target used. The registration test used APK SHA-256 `d31ab413c353f2be72f5772f3ea123767a606e0036bdc9fcbd3c2a8448a9a5a4`.
- Fresh signingReport matched the new registration. No app rebuild or credential insertion was needed for this Cloud-side correction.
- Started `Connect Google OAuth` and selected the existing account matching the active Cloud CLI identity without publishing account details.
- The device now reaches Google's `Diese App wurde nicht ueberprueft` test-app security notice, instead of the previously observed registration error. The console confirms the project is still restricted to test users.
- The security notice was not bypassed automatically. The user subsequently confirmed completion of the security step.
- The actual authorization callback returned `RESULT_OK` with data and included `https://www.googleapis.com/auth/generative-language.retriever` in the granted scopes. The app showed `Google OAuth is connected for Gemini.` Its result handler requires the granted scope and a nonblank access token before marking authorization successful. Tokens and account details were not exported or published.

## Live Extraction Checks

- Tested the public Jamie Oliver cooking video `https://www.youtube.com/watch?v=yKUui_b7JO0` through the app's URL dialog with Gemini 3.8 Flash and Google OAuth.
- First call timed out after approximately 60 seconds. The sanitized stack identified `SocketTimeoutException` while OkHttp HTTP/2 was reading response headers, not a registration error or JSON parse failure.
- Commit `5d1dbfb` increases Gemini's read timeout to 180 seconds and adds a 240-second total call limit, retaining the existing connect/write limits. It does not claim to repair the remote response problem.
- Installed that build without clearing app data; verified APK SHA-256 `061f8965dc5174e9d2cac8484bb4bc116493d734759a68a878ac73a5918874ff`. All 58 unit tests, lint, debug/release builds and eight Mi 11 instrumentation tests passed locally (device suite: 9.067 seconds).
- A single repeat of the same video request timed out after approximately 180 seconds. No recipe was saved. The cached Android authorization reached the HTTP request without another consent prompt or token-acquisition error; successful inference remains unproven.
- A separate non-generating models-endpoint check from the Mac, using the existing CLI identity and explicit project header, promptly returned HTTP 403 / `ACCESS_TOKEN_SCOPE_INSUFFICIENT`. That CLI credential is not the Android AuthorizationClient token. This proves a Mac response path, not a successful Android API call or an Android scope diagnosis.

## Device Response-Path Diagnosis

- Added opt-in `GeminiNetworkProbeTest`, with all external requests disabled unless explicitly selected through instrumentation arguments. Its original connectivity/metadata checks do not generate content or change account permissions. The separate generation probe described below also requires an explicit opt-in.
- On the Mi 11, unauthenticated metadata requests returned expected HTTP 403 over negotiated HTTP/1.1 in 134ms and negotiated HTTP/2 in 138ms. This contradicts a general endpoint-connectivity or HTTP/2 transport diagnosis for the observed video timeout.
- With the actual Android AuthorizationClient token and existing consent, the Gemini 3.8 Flash metadata endpoint returned HTTP 200 both without a quota header and with the explicitly selected Cloud project. The earlier CLI-token scope failure does not apply to this Android token.
- Both opt-in diagnostic paths passed when explicitly activated. The ordinary device suite then passed all eight core tests, with the two diagnostics skipped (runner reports ten discovered tests). Unit/lint/debug/release checks also succeeded. No further potentially billed video inference was attempted in this diagnostic step.

## Minimal Generation Isolation

- One explicitly selected `minimalGenerationResponds` probe used the actual Android OAuth token and the production request factory to send a tiny synthetic JSON prompt, without video/fileData. No extra permissions, account details or recipe data were requested.
- The API responded after 1,036ms with HTTP 503 and structured status `UNAVAILABLE`. The manually enabled diagnostic test correctly failed; this is not a passing inference result.
- This proves a generation failure occurs without YouTube input. It does not prove that the earlier video timeouts have the same cause, nor establish a global Google outage. No automatic retry or provider/model substitution was added.
- The provider now maps server failures, rate limits, authentication and permission errors to distinct actionable messages, retaining the HTTP code and excluding raw server content. Four regressions failed against the old generic messages and then passed.
- Code commit `a28c2d6`: 62 unit tests, lint, debug/release builds passed; installed APK SHA-256 `f4f6d1cb937d45527540b1d19166f2e5002e7d4b025bd2268f86a78c8bd62ea7`. The Mi 11's default suite passed eight core tests with all three opt-in probes skipped (eleven discovered tests, 9.112 seconds).

## Follow-Up Isolation And CI

- [CI run 38371017392](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38371017392) at `772c557` completed successfully for build and both API 31/35 emulator jobs. Later diagnostic-only changes are not covered by that hosted result.
- The short text probe repeated without a quota header timed out at about 45 seconds. The explicit-project variant also timed out at 45 seconds; forcing HTTP/1.1 with that header did not fix the short test either.
- Google's public [status page](https://aistudio.google.com/status) showed all systems operational, while noting that availability varies by customer tier, model and features. This is not proof that this project's generation requests succeed.
- Read-only Cloud Console inspection showed no billing account linked to the Recipe Grabber project. The CLI billing-status query was unavailable because the Cloud Billing API was disabled; it was not enabled. [Google pricing](https://ai.google.dev/gemini-api/docs/pricing) includes a free tier for Gemini 3.8 Flash, so missing billing alone does not establish the cause. Billing and permissions were unchanged.
- Extended the opt-in diagnostic's configurable wait, bounded to 600 seconds, following the default generation timeout in Google's published client transport. With explicit quota project and HTTP/1.1, it returned HTTP 200 after 54,010ms and passed the expected synthetic JSON assertion. This proves an actual Android OAuth generation, not merely model metadata. It does not establish that the quota header or HTTP/1.1 is necessary: the earlier comparable request had a shorter time limit, and service availability can vary.
- Retried the actual cooking-video URL through the unchanged production flow afterward. It returned HTTP 503 after roughly nine seconds with the new actionable service-unavailability message; no recipe was saved. The successful synthetic generation is not substituted for video acceptance.

## Normal Configuration And Cancellation

- The comparison with the normal HTTP/2 transport and no quota-project header returned HTTP 200 after 507,497ms. The synthetic JSON assertion passed (507.653 seconds for the instrumented test). This shows neither forced HTTP/1.1 nor a quota header is required for that successful generation. It does not explain service-side latency or prove video extraction.
- [CI run 38376009585](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38376009585) completed successfully for build and API 31/35 instrumentation at the diagnostic checkpoint `91d6995`.
- The production read/total-call limits are now 600 seconds, based on the observed successful response exceeding the previous 180/240-second limits. Connect/write limits remain 30 seconds. HTTP failures still return immediately; no retry, billing change, extra scope, hardcoded project or model substitution was introduced.
- Replaced blocking response-header execution with a cancellable OkHttp callback bridge. Coroutine cancellation cancels the waiting call, and responses arriving after cancellation or before cancelled dispatch are closed. Five focused tests cover success ownership, network failure and cancellation races. All 67 unit tests, lint and debug/test/release builds passed locally.
- Installed main APK SHA-256 `01687c9b9123e1ba87d53cb6aee044dd2c770a560f8371d480f14967ba3ceee6` without clearing app data. A live video acceptance check is still required; the longer timeout is not claimed to repair HTTP 503 responses.
- The default Mi 11 suite completed in 9.038 seconds: eight core tests passed, and all three opt-in external probes were skipped (eleven discovered tests).
- With code commit `345a8be`, the same public cooking-video request started at 11:38:59 and returned HTTP 503 at 11:39:04 (approximately 4.6 seconds). Authorization completed without another consent screen. No successful extraction or persisted recipe is claimed. This server rejection precedes the timeout and is not fixed by increasing it; no automatic retries were made.

## Live Video Acceptance With Flash-Lite

- Added an explicitly selectable Gemini 3.1 Flash-Lite option from Google's [published model specification](https://ai.google.dev/gemini-api/docs/models/gemini-3.1-flash-lite), which supports video input and structured output. Existing choices are preserved; no automatic fallback or retry was added.
- Code checkpoint `ba3ca40`: all 68 unit tests, lint and debug/test/release builds passed. Installed main APK SHA-256 `5d6eebed853da37556680a164059b0a97c2802da745bf19dff8004050e146278` was verified on the Mi 11 without clearing data.
- Selected Flash-Lite in Settings for a controlled comparison with the same public Jamie Oliver video and existing Google OAuth consent. Production logs confirm `gemini-3.1-flash-lite` at 11:43:32.822, successful recipe parsing at 11:43:44.342 (approximately 11.5 seconds), and saving recipe ID 1 at 11:43:44.359.
- After stopping the app, a read-only snapshot of its Room database and WAL showed recipe ID 1 with the original public source URL, four ingredients and five steps. The app was restarted; the recipe was visible in the list and opened into its detail view. No user database was cleared or replaced.
- The imported recipe's portion control changed four portions to five: eight eggs became ten, and 20g butter became 25g. Returned to the original four portions afterward. Initial automation attempts used text selectors for icon descriptions and were rejected without changing the portion count; the corrected accessibility-description actions succeeded.
- This verifies authorization, actual video request, parsing, Room persistence, restart/readback and scaling for one public video. It is not independent proof that every generated quantity/time or all three cooking variations accurately match the source; README retains the source-review warning. It does not prove TikTok/Instagram or direct-file extraction.
- Flash-Lite is now the default for new/unset Gemini choices based on this measured working flow. Existing saved selections, including 3.8, are not migrated. This is a practical default choice, not a claim that 3.8 is universally unavailable or that Flash-Lite is universally more accurate.
- After the default change, all 68 unit tests, lint and debug/test/release builds passed. Installed final main APK SHA-256 `d4646d2db03ab397bf942ee12d5b6f4013cba912852307581a9bdd7d03e8481b` matched the build. The Mi 11 default suite passed eight core tests and skipped three external probes in 9.013 seconds. Temporary read-only database copies and device UI dumps were removed; the saved test recipe and user app data were retained.

## Remaining Scope And Limits

- The supported public-YouTube/Google-OAuth flow has a real positive device acceptance result. Other providers, social-video ingestion, direct-file URLs and Drive backup/restore retain the implementation/test limitations in README; they are not claimed as live-verified.
- The earlier 3.8 response failures remain external observations, not a proven quota, billing, network-protocol or OAuth-scope diagnosis. No speculative financial or permission changes were made.

The earlier 12 October device audit is a historical checkpoint. Its missing-client/browser-access blocker is resolved. Successful Flash-Lite video acceptance does not erase the earlier 3.8 failures.
