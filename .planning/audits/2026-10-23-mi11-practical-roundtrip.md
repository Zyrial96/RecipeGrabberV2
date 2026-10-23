# Mi 11 Practical Share-To-Recovery Acceptance

## Scope And Identity

Physical Xiaomi Mi 11 (M2011K2G/venus), Android 12/API 31, main package
`com.recipegrabber`. The checkout began clean at `5441ccb` on `main`.
No runtime source, credentials, model selection or Cloud configuration changed.
This acceptance uses the existing debug installation, not the signed production
APK previously accepted on the S25.

Installed APK SHA-256:
`498bde20819dd6d2f3de6535f60b9f6185de9b8233f5a1e222c71ef0664985b7`.
Installed test APK SHA-256:
`d2ee1e38e64414ef674a9c8cdac9a9c2112dd0b7abe5a1bc716f5a19fe6127c6`.
Both match their corresponding local APK artifacts.

## Preservation Before Testing

- Consistent private copies of the main database/WAL/SHM and installed APK were
  taken after force-stop. The separate QA installation had no database; its APK
  was also preserved. All five saved file hashes matched their private manifests.
- Raw files remain outside Git under
  `~/.local/share/recipegrabber-backups/2026-10-23-mi11-e2e/`, with owner-only
  access. SQLite validation uses separate copies, not the raw backup files.
- Initial main-library counts: three recipes, twelve ingredients, twenty steps.
  SQLite integrity and foreign-key checks passed.
- Additional snapshots were taken after extraction, after removing only the
  newly created test recipe, after recovery and after all final checks.
- Existing recipes, cloud snapshots, app settings, accounts and keys were not
  deleted, cleared or replaced. No APK installation was required.

## Actual Workflow

1. Delivered the public cooking-video link
   `https://www.youtube.com/watch?v=yKUui_b7JO0` through Android `ACTION_SEND`,
   MIME `text/plain`, `Intent.EXTRA_TEXT`, to the app's real MainActivity.
   This exercises the receiving share-intent path; the YouTube application's
   own share-sheet UI was not separately operated.
2. The real extraction bottom sheet displayed video analysis. The configured
   Gemini 3.1 Flash-Lite/Google OAuth path produced and saved one new structured
   recipe with three ingredients and six steps. No response fixture, synthetic
   recipe insertion or host database injection was used.
3. The persisted library now contained four recipes/fifteen ingredients/twenty-six
   steps. The new recipe opened with ingredients, amounts, servings and steps.
4. In Settings, the normal **Backup now** action reported
   `Backup saved: 4 recipes`. The new four-recipe snapshot appeared with timestamp
   `2026-10-23T20:36:36.789Z`, alongside the older snapshots.
5. After preserving another local snapshot, removed only that newly generated
   recipe through its detail-screen confirmation. Readback proved the remaining
   database exactly equaled the original three-recipe library, including children.
6. Refreshed the actual Drive listing, selected the fresh four-recipe snapshot
   and confirmed restore. The UI reported
   `Restored 1 recipes; 3 existing recipes kept`.
7. Repeated the same restore: `Restored 0 recipes; 4 existing recipes kept`.
8. Force-stopped/relaunched the main app. All four recipe cards remained. The
   restored recipe opened independently with all three ingredients and six step
   instructions. Increasing two portions to three changed four eggs to six and
   one unit of butter to 1.5; returning to two restored the original display.

## Data-Level Assertions

Read-only SQLite comparisons on private validation copies confirmed:

- All fields and primary IDs of the three original recipe aggregates and their
  ingredients/steps are unchanged after extraction, temporary test-recipe
  deletion, restore and final checks.
- Exactly one new recipe was created by extraction. Its stable backup UUID
  occurs exactly once after recovery.
- All restored recipe metadata matches the extracted snapshot, except the
  deliberately reallocated SQLite primary ID. All ingredient/step fields match
  after excluding reallocated primary/parent IDs.
- Final counts remain four recipes, fifteen ingredients and twenty-six steps.
  The final screenshot shows four readable, non-overlapping recipe cards;
  long list titles/descriptions are ellipsized rather than overlapping controls.

## Regression Verification

- `testDebugUnitTest --offline --rerun-tasks`: 150 tests, zero failures/errors/
  skips, freshly executed with KSP regeneration.
- `lintDebug`: successful. Existing AGP/compile-SDK and deprecated-API warnings
  remain maintenance work, not a failure of this tested flow.
- Python release-tooling suite: ten passes.
- Existing matching main-app instrumentation APK, rerun without UI automation:
  twenty-five passes, four opt-in external assumption skips, zero failures.
  Runner summary discovers twenty-nine; that is not twenty-nine passes. The
  real inference/Drive workflow above is separate from those skipped probes.
- Installed main/test APK hashes match the local artifacts. No new APK was
  installed, no release identity rotated, and no permissions/billing changed.

## Automation Observations And Limits

MIUI rejected shell input injection with `INJECT_EVENTS`. Existing on-device
UI Automator accessibility actions operated the normal application controls
successfully; security switches, SIM requirements and Play Protect were unchanged.
An attempted scrcpy/native-app-control route was not usable and was stopped.
One navigation attempt while instrumentation owned the UI failed; the native
suite was repeated without competing UI actions before final acceptance.

No functional application defect was reproduced in this bounded workflow, so no
speculative runtime patch or model change was made. The generated content was
one recipe variant; this is not an assertion of exhaustive culinary/transcript
accuracy. Other providers, TikTok/Instagram, offline inference, process death,
same-certificate release updates and signed Mi 11 acceptance remain outside
this test. Backups are manual, not background synchronization.

The phone retains all four recipes, and the fresh Drive snapshot/private local
backups are retained. Private contents, account identifiers, cloud file IDs and
credentials are excluded from this public report. Temporary on-device UI dumps
were removed after observation.
