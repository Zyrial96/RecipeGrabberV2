# Google OAuth Registration

## Verified Change

- The user approved creating the prepared Android OAuth client before the final console submission.
- In the existing Recipe Grabber V2 Cloud project, the previous Android client registered `com.recipegrabber` with a different signing fingerprint. It was left unchanged, along with the existing web client.
- Created a separate Android client named `RecipeGrabber - Mac Debug` for package `com.recipegrabber` and public SHA-1 fingerprint `6A:34:6C:6E:B4:C2:1C:A7:CA:0B:18:8C:F0:64:88:8E:AB:D4:1F:B7`.
- The console displayed `OAuth-Client erstellt`; the client list subsequently showed the new Android entry dated 13 October 2026 alongside the previous clients.
- No client secrets were downloaded. Billing, browser profiles, Drive permissions and global CLI configuration were not changed.

## Device Evidence

- Xiaomi Mi 11 was the only connected ADB target used. The installed APK remained SHA-256 `d31ab413c353f2be72f5772f3ea123767a606e0036bdc9fcbd3c2a8448a9a5a4`.
- Fresh signingReport matched the new registration. No app rebuild or credential insertion was needed for this Cloud-side correction.
- Started `Connect Google OAuth` and selected the existing account matching the active Cloud CLI identity without publishing account details.
- The device now reaches Google's `Diese App wurde nicht ueberprueft` test-app security notice, instead of the previously observed registration error. The console confirms the project is still restricted to test users.
- The security notice was not bypassed automatically. User handoff is required before continuing through that notice.

## Remaining Acceptance

- Complete the user-approved security/consent flow on the Mi 11 and observe the actual authorization result.
- Verify token acquisition without exporting or logging the token; then verify an actual supported video extraction separately.
- Reaching a security or consent screen does not prove successful login, granted permissions, API access or recipe extraction.

The earlier 12 October device audit is a historical checkpoint. Its missing-client/browser-access blocker has progressed to the security/consent checkpoint described here.
