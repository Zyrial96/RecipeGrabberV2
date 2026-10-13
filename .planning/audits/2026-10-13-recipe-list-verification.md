# Persisted Recipe Card Verification

## Cause And Fix

- The Mi 11 displayed zero ingredients for each saved recipe, despite successful Room/WAL and detail-view checks confirming four ingredients each.
- `RecipeCard` counted the embedded entity's transient `Recipe.ingredients`, which is empty after Room loads persisted data. `RecipeListViewModel` already supplies the correct `RecipeWithDetails.ingredients` relation. Step counts already use their loaded relation.
- Changed only the card's ingredient-count source to `recipeWithDetails.ingredients.size`. No database/schema changes, data clearing or extraction changes were needed.

## Regression Evidence

- Added two Compose `RecipeCardTest` cases: a nonempty loaded relation with an empty embedded list, and an empty loaded relation with a stale nonempty transient list.
- Before the production fix, both tests failed on the Mi 11 (`/tmp/recipegrabber-card-red-device.txt`, two failures). After the one-line fix, the full device suite passed twelve core tests and skipped three external opt-in probes, 12.401 seconds (`/tmp/recipegrabber-card-final-device.txt`).
- 121 JVM tests pass with zero failures/errors/skips. Full JDK 17 cached-dependency lint/debug/test/release assembly succeeded (`/tmp/recipegrabber-card-final-build.txt`).
- Installed final APK SHA-256 matched the local build: main `107e2f96da55417b3120f19aee6974d9fd4cf640b83e22641c5dce7b4ac4c49c`; test `9520d26d9a637e80affc7708c39943e85e6c64565e351b24de627d4bef4081ab`.
- Restarted the actual app without clearing user data. The large-file tortilla recipe shows four ingredients/eight steps, the inline tortilla recipe four/seven, and the YouTube recipe four/five. All three previous real imports remain present; native tests use their own fixtures rather than rewriting these records.
- Loopback installation server and adb reverse were stopped after installation. Temporary database snapshots from the live import were removed.

## Scope

This verifies persisted ingredient-count display on the Mi 11 and the two regression fixtures. It does not independently prove model-extracted quantities or implement Google Drive backup. The separate real Google Files import is recorded in `2026-10-13-files-api-verification.md`.
