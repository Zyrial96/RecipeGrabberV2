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
