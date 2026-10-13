# Files API Verification

## Implemented Transfer

- `a3b419d` adds a bounded direct-video transfer to Gemini Files API; `7ec05db` identifies the downloader according to origin policy; `3069fd9` adds structured upload failure handling.
- YouTube bypasses downloads/uploads. Direct MP4/WebM/MOV files stream to app cache, limited to 100 MiB. Preparation has a 180-second bound, readiness polling accepts ACTIVE only, and Google request/session/file origins are validated.
- Source requests contain neither Google bearer tokens nor API keys. Google requests do not follow redirects. Source DNS rejects private/loopback/link-local/CGNAT destinations; redirects cannot downgrade HTTPS or introduce URL credentials.
- The original source URL remains in the recipe/prompt. Temporary local files are removed in finally; only the newly uploaded remote file is targeted for best-effort cleanup. No successful remote delete is claimed from the current denied-upload test. No completed cloud file was observed.
- Cancellable callback response consumption now retains call cancellation while reading the body, closes it before returning data and propagates failures.

## Local And Hosted Evidence

- 116 JVM tests pass, zero failures/errors/skips. Full JDK 17 wrapper run of unit tests, lint, debug/test/release assembly succeeded (`/tmp/recipegrabber-files-build.txt`). Tests use synthetic responses/temporary directories, not paid APIs.
- Mi 11 / Android 12 / API 31 default suite: ten tests passed, three external probes skipped, 8.876 seconds (`/tmp/recipegrabber-files-device-tests.txt`).
- Installed final main APK SHA-256 `0a4c889b2dd5f62a38c728423dc39c958dd0c0ae191098f008b2d1c8eeaae800` matched the build, without clearing user data. Existing test APK SHA-256 `3e7ce5b30c18717d2834d7146cc5fe5e89088de4598bdccfa2718a67e8fdf6ae` was unchanged.
- [Hosted run 38381879227](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38381879227) at the preceding `bffe751` checkpoint completed successfully for build and API 31/35 emulator jobs. This is not hosted verification of the newer uploader commits.
- [Hosted run 38385620263](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38385620263) at `9b875e352c0ef49f1a3a984bee55c461d04c268c` subsequently completed successfully for all three jobs: build, instrumentation (31), and instrumentation (35). This covers the newer uploader, User-Agent correction and structured scope diagnostics. It does not enable live generation or grant the pending upload scope.

## Real Source And Permission Checks

- Reused the same public [Patata tortilla.webm](https://commons.wikimedia.org/wiki/File:Patata_tortilla.webm), by Elene B, CC BY-SA 4.0. No media copy or derivative is committed.
- The original generic OkHttp download was refused with HTTP 403 on both Android and a matching Mac ranged GET. [Wikimedia policy](https://foundation.wikimedia.org/wiki/Policy:Wikimedia_Foundation_User-Agent_Policy/en) requires descriptive application identification, not browser impersonation. Using `RecipeGrabberV2 (https://github.com/Zyrial96/RecipeGrabberV2)` returned HTTP 206 for a 1,024-byte Mac range and let the Android flow reach the Google upload endpoint.
- At 12:34, the Android download completed but Google upload start returned HTTP 403. No inference or recipe save followed.
- At 12:40:17-18, one repeat with structured error handling confirmed `ACCESS_TOKEN_SCOPE_INSUFFICIENT` from Google. The app explains that upload permission is additional to the working YouTube authorization. Raw server messages, URLs, token values and account details are excluded from these errors.
- Google's public [v1beta discovery metadata](https://generativelanguage.googleapis.com/$discovery/rest?version=v1beta) explicitly lists `https://www.googleapis.com/auth/devstorage.read_only` for media.upload and files get/list/delete. The existing Android request grants only the Generative Language retriever scope. This is direct metadata plus actual failure evidence, not an assumption from HTTP 403 alone.
- The user has been asked to approve a separately optional Google Cloud Storage permission flow. No such permission has yet been requested/granted by the app at this checkpoint. No billing, IAM roles, Cloud APIs or global CLI state were changed.
- No `recipe-video-*` temporary file remained in app cache after the denied operation. Other existing cache files were preserved.

## Remaining Acceptance

YouTube OAuth/video acceptance remains recorded in the earlier audit. Direct-file positive acceptance still requires an approved Files authorization path and a real extraction/persistence test. Do not mark the full goal complete from these isolated upload tests or from a working source download. An approval/consent dependency is now explicit; do not keep retrying the same scope-denied request.

## Inline Update At 3dfcf75

- Google's [video input guide](https://ai.google.dev/gemini-api/docs/video-understanding) also supports inline bytes for small one-off videos. The new default uses inline data up to 8 MiB, leaving room for base64/prompt overhead below the documented 20MB small-request guidance. The 100 MiB total download cap and larger Files path remain unchanged, not removed to redefine success.
- Small transfers do not create a Files object or require its Cloud Storage scope. Two new tests verify byte/MIME preservation with no Files calls, and selection of the upload branch above the threshold. 118 JVM tests plus lint/debug/test/release builds pass.
- Installed main APK SHA-256 `358dbb4b8ceeefa6af2a94f10e3962080ede86c244091157dcc8bdc5dd57aec4` matched the build without clearing data. Default Mi 11 suite: ten passed, three skipped, 8.869 seconds (`/tmp/recipegrabber-inline-device-tests.txt`).
- Sent the same public 7,482,355-byte WebM through ACTION_SEND. Logs confirm Gemini 3.1 Flash-Lite starting at 13:03:14.990, successful parsing of `Spanische Kartoffel-Tortilla` at 13:03:41.117 and recipe ID 2 saved at 13:03:41.129 (about 26 seconds total).
- After stopping the app, a read-only Room/WAL snapshot showed ID 2 with the original source URL, four ingredients and seven steps. Restarted the app, verified its list entry and opened its detail view. Temporary database copies/UI dumps were removed; user data and the two saved test recipes were retained. No `recipe-video-*` cache file remained.
- No extra Google scope, billing setting or IAM role was changed. The pending optional consent question now concerns only larger Files transfers. This small-video acceptance does not prove the 8-100 MiB branch or independently establish every generated quantity's source accuracy.
- The historical Remaining Acceptance paragraph above is superseded for small direct files. Larger Files upload/readiness/inference/persistence and latest-source hosted CI remain open gates; the full goal is not marked complete at this checkpoint.

## Hosted Verification And Approval Blocker

- [Hosted run 38388320816](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38388320816) at `1e2fe154b9affd1633a093e3b004cd3fbac1586b` completed successfully for build and both API 31/35 instrumentation jobs. This verifies the inline implementation and its 118-test source checkpoint in hosted CI, not only locally.
- The remaining 8-100 MiB Files path still requires additional consent and a positive real upload/readiness/inference/persistence check. Live source confirms the authorization factory still requests only the retriever scope; no user approval of the separately asked Cloud Storage permission has arrived.
- This same permission dependency persisted across three consecutive goal turns. The intervening inline change improved small-file support without removing the larger path. After current CI became terminal green, no required next step on the larger path could safely proceed without the user's decision. The goal is blocked, not complete; no repeated scope-denied requests or unapproved account changes are made.
- This final evidence update is documentation-only. Runtime/test/build/workflow differences against the verified `1e2fe15` checkpoint are checked empty before stopping; a newly triggered documentation CI run is not substituted for the already verified source run.

## Approved Optional Authorization Follow-Up

- The user explicitly approved the optional upload permission on October 13. The historical approval blocker above is superseded; billing, IAM roles, OAuth clients and Drive permissions remain unchanged.
- Settings now offers a separate `Authorize video uploads` command and a disclosure of Google Cloud Storage read access. It has its own activity-result launcher, so cancellation or partial grants cannot be treated as successful Files authorization. Both exact requested scopes and a nonblank token are required; cancelled authorization results are rejected.
- The default request remains retriever-only. The uploader asks for the separate Files token only above the 8 MiB threshold and uses it for upload, polling and cleanup. Generation uses the same selected token. YouTube, small inline video and API-key behavior remain separate; no bearer token is persisted.
- 121 JVM tests pass with zero failures/errors/skips; offline-cache JDK 17 lint and debug/test/release builds succeeded (`/tmp/recipegrabber-files-consent-build.txt`). A deliberate removal of the Files authorization callback made exactly the two new upload authorization tests fail; restoring it returned the full suite to green (`/tmp/recipegrabber-files-consent-red.txt`).
- Mi 11 default suite: ten passed, three opt-in external probes skipped, 9.101 seconds (`/tmp/recipegrabber-files-consent-device-tests.txt`). Installed APK hashes matched the build: main `cf64dc5e890937ab00527c66f394fc2c29b0bedfae7dbd30cfcab472eb1a556e`, test `1bf08b0abbb25fd7b9a71cedf41fd079277227d3305b19b5a6257f0b6c02255e`. App data was retained; the loopback installation server/reverse were stopped afterwards.
- On-device Settings and disclosure were exercised. Starting the optional authorization reached Google's `Google hat diese App nicht ueberprueft` warning. The user was asked to personally press Weiter; the agent did not bypass the warning. A grant, successful upload or successful large-file inference is not yet claimed.
- The selected follow-up source is the same CC BY-SA 4.0 tortilla video's public 480p WebM derivative, 21,223,793 bytes (origin HEAD HTTP 200). This exceeds the inline threshold. Its upload, ACTIVE readiness, generation, Room save and restart still require live acceptance.
