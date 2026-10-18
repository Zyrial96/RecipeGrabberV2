# Signed Release And Recovery Plan

**Goal:** Build and independently verify a signed, minified release and test Drive recovery on the S25 Ultra without deleting its installed debug app or recipes.

**Architecture:** Optional Gradle environment signing keeps ordinary unsigned CI builds working and fails closed when signing is explicitly required. A local Python helper generates/reuses one private signing identity, stores secrets only in an ignored owner-only env file and verifies the resulting APK against that identity. The S25 already has a debug-signed app, so a non-debuggable `releaseCheck` variant with a separate application ID exercises the same minified release code without uninstalling user data. Record that package difference explicitly; do not claim the production-package APK was installed.

**Stack:** Existing AGP/Gradle, JDK 17 keytool, SDK apksigner/aapt and Python 3.9-compatible stdlib.

## Constraints

- No existing app/data removal, user-profile creation, IAM/billing changes or key publication.
- Release key stays outside the repository; passwords remain in ignored `.env.release-signing`, owner read/write only. Never print credentials.
- Register required Android OAuth package/certificate identities in the existing project, preserving clients/scopes. Personal Google warnings/login remain account-owner actions.
- No second-device success claim without actual download, nonempty restore, repeat restore and restart observations on the S25.

## Work

- [x] Add secure release helper, signing configuration, isolated releaseCheck variant and ten regression tests; required-signing-without-config and wrong-variant verification both fail closed. Credential files are ignored and owner-only.
- [x] Generate/reuse the release key, build APK/AAB and releaseCheck; independently verify certificates, exact package and non-debuggable manifest plus AAB JAR signature. 150 JVM tests/lint/build gates and ten tooling tests pass.
- [x] User approved both new Android OAuth clients; created and verified without changing scopes/IAM/billing/existing clients. S25 releaseCheck began empty, restored three actual Mi 11 recipes, repeated with zero inserts, retained all after restart, opened details and scaled portions. Installed hash/non-debuggable flags verified; original app path unchanged.
- [x] Fresh review completed, verification concerns addressed with regressions; runtime/tooling commit `f82c2e4` pushed and hosted build plus API 31/35 jobs all green in run 39102779893. Explicit package/signing/recovery limits and evidence documented; final publication is documentation-only. Original apps/data remain untouched.

## Evidence And Rulings

- Actual checkout is `/Users/zyrial/Projects/RecipeGrabberV2`; baseline `4caa518`, clean worktree. No signing config/key was found in the scoped build storage/configuration.
- Wireless pairing succeeded using the mDNS-advertised private address; device verified as Samsung SM-S938B / API 36. The supplied pairing credential is not retained here.
- Ruling: use an isolated releaseCheck install because the S25 has a debuggable production-package app. A production signing-key change cannot update that app in place. Deleting it is outside this non-destructive test; the final evidence must distinguish package-equivalent minified testing from installation of the exact production APK.
