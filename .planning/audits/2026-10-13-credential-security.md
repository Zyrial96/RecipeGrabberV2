# S-01 Credential Security

## Implemented

- Five API-key fields now use Android Keystore-backed AES-256-GCM envelopes, random IVs and encrypted-preference-name authenticated data. The AES key is device/app-bound and nonexportable. Platform cryptography is used, not a custom encryption algorithm or embedded master password.
- DataStore migration constructs all transformed fields before one atomic update. Unrelated settings are preserved. Legacy plaintext fields are removed only after successful encryption; repeat migration is idempotent. Encryption failure preserves the entire original preference transaction for recovery and readers never use retained legacy plaintext.
- Encryption/decryption run on IO dispatchers. Decrypt does not create a replacement key. Missing keys, wrong fields/keys, corruption and unsupported envelopes return no usable credential and are visible in Settings. Explicit replacement/clearing affects only that provider's fields.
- Settings reports save failures without raw exception/key contents and cancels obsolete pending writes per provider. Onboarding catches save errors and disables successful completion while an error is reported; optional skip behavior remains available.
- No credentials are included in these docs/tests, except explicitly synthetic fixture values. Existing API-key values on the device were not inspected or printed.

## Verification

- 127 JVM tests pass with no failures/errors/skips, including six new encryption/migration tests. Replacing the migration encryption call with plaintext storage made two tests fail; restoring encryption returned the full suite to green (`/tmp/recipegrabber-credential-red.txt`).
- JDK 17 cached-dependency unit/lint/debug/test/release assembly succeeded (`/tmp/recipegrabber-credential-build.txt`).
- Mi 11, Android 12/API 31: fifteen tests pass and three external probes skip, 12.921 seconds (`/tmp/recipegrabber-credential-device-tests.txt`). Three new native tests verify actual nonexportable Keystore keys, randomized encryption, missing-key failure without regeneration, and persisted DataStore migration of five isolated synthetic keys. QA aliases/files are unique and cleaned; production recipes/settings/keys are not reset.
- Installed main APK SHA-256 `5430e836595e9f13477e02151f1dfa7808d87453dee55e91e3680f0158acd318`; test APK `f8248b18c9beb0bd67b58c10a25f11bb2007a54f8bfa7f86ba78657ef12e8c6d`; device hashes matched builds.
- Restarting the real app retains all three imported recipes with four ingredients and their eight/seven/five steps. Settings retains Gemini OAuth connected state and shows no unavailable-credential warning. This confirms production startup/settings compatibility, not inspection of any real key value.
- Loopback installation server/reverse and temporary UI dump were stopped/removed after testing.

## Limits And Next Steps

Keystore protects stored key material; it does not prevent a compromised app process from using a key. Authentication-gated keys/biometrics and rotation are not introduced. If migration encryption is unavailable, legacy plaintext is deliberately retained unchanged for lossless recovery, but never used by providers; restart retries migration or the user may replace affected keys. There is no claim that an encryption failure has already erased historical plaintext disk blocks.

S-02/S-03 logging minimization and platform backup-rule hardening, actual Drive backup/restore, and their hosted/device acceptance remain required work under the active goal. This checkpoint is not the completed high-level security/Drive objective.
