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

- Added opt-in `GeminiNetworkProbeTest`, with all external requests disabled unless explicitly selected through instrumentation arguments. It does not generate content or change account permissions.
- On the Mi 11, unauthenticated metadata requests returned expected HTTP 403 over negotiated HTTP/1.1 in 134ms and negotiated HTTP/2 in 138ms. This contradicts a general endpoint-connectivity or HTTP/2 transport diagnosis for the observed video timeout.
- With the actual Android AuthorizationClient token and existing consent, the Gemini 3.8 Flash metadata endpoint returned HTTP 200 both without a quota header and with the explicitly selected Cloud project. The earlier CLI-token scope failure does not apply to this Android token.
- Both opt-in diagnostic paths passed when explicitly activated. The ordinary device suite then passed all eight core tests, with the two diagnostics skipped (runner reports ten discovered tests). Unit/lint/debug/release checks also succeeded. No further potentially billed video inference was attempted in this diagnostic step.

## Remaining Acceptance

- Diagnose the device/API response-header timeout and verify successful extraction of an actual supported video. Avoid repeated potentially billed generation attempts until the response-path problem is understood.
- Google login, token acceptance and authenticated model-metadata access are now demonstrated. Inference and recipe persistence for a real video are separate gates and have not passed. Next isolate a minimal generation request from the YouTube file-input path; do not treat a metadata GET as successful generation.

The earlier 12 October device audit is a historical checkpoint. Its missing-client/browser-access blocker is resolved; the remaining failure is the live video API response path described above.
