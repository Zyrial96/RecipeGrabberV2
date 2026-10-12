# Mi 11 Verification And Maintenance

## Verified State

- Actual checkout: `/Users/zyrial/Projects/RecipeGrabberV2`. Direct Mac execution works with the correct working directory; earlier startup failures at the removed Desktop path did not prove unavailable Mac access.
- Xiaomi Mi 11, Android 12 / API 31, explicitly targeted by ADB serial. Other connected devices were not used.
- App minimum API is now 31; receiver registration uses AndroidX compatibility.
- Xiaomi rejected both USB-install/input-debug approval with `Insert your SIM card`. Those switches were not activated. MIUI optimizations, bootloader state and global Play Protect settings were unchanged.
- Updates used an APK download through an ADB reverse tunnel to a loopback-only Mac HTTP server, then the normal package installer. The temporary server/tunnel were removed afterward.
- Installed onboarding-fix APK SHA-256: `6597ffd50b7b7fd024ad6635d10119b722df7604bd39920862200f9197a7dcb9`, matching the host build.
- App opens the recipe list. The URL dialog accepts input; extraction without a Gemini key displays the missing-key error and leaves the recipe list empty.
- Google OAuth opens an account picker with two accounts. No account was selected automatically; no usable token or successful live extraction has been demonstrated.

## Fixes And Checks

- Gemini now attaches video fileData rather than only URL text. YouTube/direct MP4/WebM/MOV requests and rejected unsupported URLs have fixture tests.
- API keys use headers. Responses close reliably; cancellation is rethrown.
- Malformed, blocked, truncated and incomplete recipes do not become placeholder successes. Thought parts are excluded; detail IDs are zero for Room allocation.
- Onboarding persists its DataStore status before calling completion. The regression test explicitly delays persistence; prior device behavior exposed unsafe callback ordering.
- Local results: 40 JUnit tests, zero failures/errors; lintDebug, assembleDebug and minified assembleRelease succeeded with JDK 17.
- Hosted CI was added. Its first run failed because sdkmanager was assumed to be on PATH; explicit SDK provisioning was added. Hosted success must be checked independently.

## Remaining Work

- Hosted CI verification; real Room integration tests for multiple recipes and transactional rollback.
- Portion-scaling edge cases, shared input/result validation and duplicate Gemini model choices.
- OAuth with the intended account and matching Cloud registration. An account picker is not authorization proof.
- Native video ingestion for other providers and automatic Drive backup/restore remain unimplemented. Social scraper/CDN compatibility is unverified.
