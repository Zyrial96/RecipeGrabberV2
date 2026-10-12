# Session Corrections: 2026-10-12

## Scope and Evidence

Reviewed the conversation and historical session
`019dde2c-6297-7ba2-9308-913c749e68c0` against the current repository.
Historical source type: session_codex.
Source URI:
`/Users/zyrial/.codex/sessions/2026/04/30/rollout-2026-04-30T13-35-44-019dde2c-6297-7ba2-9308-913c749e68c0.jsonl`.
Historical project: `/Users/zyrial/Desktop/RecipeGrabberV2`.

The old checkout no longer exists on the Mac. A clean GitHub checkout was
created under `/Users/zyrial/Projects/RecipeGrabberV2`.
CODEX.md and applicable parent AGENTS.md were read. No workspace Graphify
graph or project AGENTS.md was present in the new checkout.

## Corrections Applied

- Gemini OAuth bypasses the API-key-only gate. The provider still obtains and
  validates actual Google authorization; the stored display flag is not used
  as an access-control decision.
- Drive email alone no longer makes Gemini appear authorized.
- Disabling Drive sync does not implicitly clear Gemini OAuth configuration.
- Removed unused legacy Gemini sign-in methods.
- Extraction rejects recipes without a title, named ingredients, and
  nonempty preparation instructions before saving.
- Recipe copies preserve transient ingredients and steps.
- Extraction propagates coroutine cancellation, including cancellation
  returned as a failed Result.
- RecipeListViewModel delegates extraction and persistence to the same use
  case, including the scraping fallback. It no longer saves unvalidated
  provider output directly.
- Apify and LLM onboarding can be postponed, allowing users without API keys
  to reach OAuth settings.
- Restored the missing Gradle wrapper JAR and removed its ignore rule.

## Verification

- Six use-case regression tests cover OAuth without a key, missing API keys,
  invalid output, saved recipe details, and cancellation.
- Three list-view-model regression tests cover delegated errors, avoiding
  duplicate saves, and direct fallback.
- All 23 unit tests passed.
- `./gradlew testDebugUnitTest lintDebug assembleDebug` passed using JDK 17
  and Android SDK platform 35 / build-tools 34.0.0.
- AGP 8.5.2 and Gradle 8.7 were unchanged. AGP emits a compileSdk 35 support
  warning, but the published build configuration completed successfully.
- Wrapper JAR SHA-256 matched the official Gradle 8.7 checksum.
- Device checks use only the connected Samsung, selected explicitly via ADB.
  The BRAVIA ADB target was not used.
- A separate `com.recipegrabber.audit` debug package was installed. The
  original app and its database were preserved.
- The original APK signature was freshly checked. Its private signing key is
  absent on the Mac, so a replacement original-package update was not installed.
- Keyless onboarding was verified with the new postpone actions. The automated
  full-onboarding harness needed manual completion of its final click; its
  first attempts did not pass. This is not reported as a fully automated pass.
- The settings smoke test passed on the latest audit build after an app restart:
  Gemini OAuth selection, no API-key field, no false connected status, Google
  authorization UI, and return after cancellation without false authorization.
  Google reported cancellation status 16, not a successful token.

## Earlier Conclusions Corrected

- An account chooser opening is not evidence of successful OAuth or a
  successful Gemini request. Neither was established in the original session.
- Legacy Google Sign-In supported additional OAuth scopes. Choosing the modern
  AuthorizationClient is appropriate, but the old API choice was not proven
  to cause UNREGISTERED_ON_API_CONSOLE.
- RESULT_CANCELED with data can carry a Google error. Parsing those data
  recovered the registration failure; it did not demonstrate a valid token.
- The old scope-list test did not exercise the authorization flow.
- A text prompt containing a URL does not establish video-media access.

## Still Open

The last demonstrated login failure was UNREGISTERED_ON_API_CONSOLE. Google
Cloud project ownership, Android client registration, consent/test-user setup,
API access, and quota-project behavior require verification. A successful
consent/token-refresh/Gemini round trip is still outstanding.

The persisted OAuth display flag is a cached status, not a live proof of
authorization. Account selection, revocation, and reconnect need further
coverage.

Provider adapters still mostly submit URLs in text prompts. A real media-input
pipeline is required before claiming reliable TikTok/Instagram video analysis.
Invalid placeholder outputs are now rejected by both extraction paths.

## Primary References

- https://developer.android.com/identity/authorization
- https://ai.google.dev/gemini-api/docs/oauth
- https://services.gradle.org/distributions/gradle-8.7-wrapper.jar.sha256
