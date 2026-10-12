# Mi 11 Verification And Maintenance

## Verified State

- Actual checkout: `/Users/zyrial/Projects/RecipeGrabberV2`. Direct Mac execution works with the correct working directory; earlier startup failures at the removed Desktop path did not prove unavailable Mac access.
- Xiaomi Mi 11, Android 12 / API 31, explicitly targeted by ADB serial. Other connected devices were not used.
- App minimum API is now 31; receiver registration uses AndroidX compatibility.
- Xiaomi rejected both USB-install/input-debug approval with `Insert your SIM card`. Those switches were not activated. MIUI optimizations, bootloader state and global Play Protect settings were unchanged.
- Updates used an APK download through an ADB reverse tunnel to a loopback-only Mac HTTP server, then the normal package installer. The temporary server/tunnel were removed afterward.
- Latest installed APK at code commit `44bf7ee`, SHA-256: `df4c08150512260cde4c01ee5b2886370683793e2f2c3516b2eabe3e27514cac`, matching the host build.
- App opens the recipe list. The URL dialog accepts input; extraction without a Gemini key displays the missing-key error and leaves the recipe list empty.
- Google OAuth opens an account picker with two accounts. After API activation, the account uniquely matching the active Cloud CLI identity was selected without publishing account details. The result returned data with status 8 / `UNREGISTERED_ON_API_CONSOLE`; no usable token or successful live extraction has been demonstrated.

## Fixes And Checks

- Gemini now attaches video fileData rather than only URL text. YouTube/direct MP4/WebM/MOV requests and rejected unsupported URLs have fixture tests.
- API keys use headers. Responses close reliably; cancellation is rethrown.
- Malformed, blocked, truncated and incomplete recipes do not become placeholder successes. Thought parts are excluded; detail IDs are zero for Room allocation.
- Onboarding persists its DataStore status before calling completion. The regression test explicitly delays persistence; prior device behavior exposed unsafe callback ordering.
- Local results: 50 JUnit tests, zero failures/errors; lintDebug, assembleDebug and minified assembleRelease succeeded with JDK 17. Unit tests, lint and debug build passed again after the logging-only change at `44bf7ee`.
- Recipe aggregate writes are transactional; fresh detail IDs avoid cross-recipe collisions. Four Room tests cover independent recipes, insert/update rollback, favorites and deletion cascades.
- Integer portion controls, localized ingredient amounts and delete-after-persistence behavior have regression tests. Four Compose tests verify controls, favorites and confirmation/navigation, including a nonblank synthetic-recipe screenshot.
- Latest installed build passed all eight instrumentation tests on the Mi 11 (8.934 seconds). Tests use isolated in-memory databases, not user recipes. This is not live extraction evidence.
- Hosted CI was added. Its first run failed because sdkmanager was assumed to be on PATH; explicit SDK provisioning was added. [Run 38284346956](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38284346956) at `b4a8e8f` succeeded for build and both API 31/35 instrumentation jobs. This does not establish the status of later commits.
- The Generative Language API was disabled in the verified Recipe Grabber Cloud project. It was enabled with an explicitly scoped CLI command and read back as enabled. Billing and global CLI project settings were unchanged.
- Debug signingReport succeeded. The installed app requires Android client package `com.recipegrabber` and SHA-1 `6A:34:6C:6E:B4:C2:1C:A7:CA:0B:18:8C:F0:64:88:8E:AB:D4:1F:B7`. This public certificate fingerprint is not a private key. Google rejected that identity as unregistered after API activation. Consent/test-user configuration remains unverified.
- Cloud client inspection is blocked by unavailable connected browser control: browser discovery returned no sessions, and native Chrome inspection did not return a UI before cancellation. Launching a console URL was not counted as verified access. No browser profiles, credentials or security settings were changed.
- Full video URLs are no longer written to Gemini startup logs.

## Remaining Work

- Shared input/result validation and duplicate Gemini model choices.
- OAuth with the intended account and matching Cloud registration. An account picker is not authorization proof.
- Native video ingestion for other providers and automatic Drive backup/restore remain unimplemented. Social scraper/CDN compatibility is unverified.
