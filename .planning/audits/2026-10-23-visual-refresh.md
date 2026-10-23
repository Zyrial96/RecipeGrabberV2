# Compose Visual Refresh Acceptance

Runtime/test checkpoint: `52b0490`. Actual checkout is
`/Users/zyrial/Projects/RecipeGrabberV2`. The UI uses existing Material 3, Coil
and Navigation Compose dependencies; no framework/dependency, permission,
provider, signing identity or database schema changed.

## Delivered

- Neutral light/dark surfaces, green actions, coral accents, semantic container
  colors, zero letter spacing and consistent component shapes. Adaptive launcher
  artwork uses the same palette; legacy raster launcher assets were not regenerated.
- Image-backed recipe cards with wrapping titles, ingredient/step/time metadata,
  responsive grid columns, search clearing, favorite filtering and separate
  empty/search states. List bottom padding keeps final items clear of the FAB.
  Favorite and overflow actions do not trigger card navigation; deletion still
  requires the library/detail confirmation, not a single destructive card tap.
- Existing HTTPS thumbnails are used when available. Missing YouTube artwork
  is derived only from recognized HTTPS hosts and validated public video IDs;
  tracking/query values are not forwarded to the thumbnail request. Actual
  YouTube thumbnails loaded on the phone. Missing/unreachable artwork has a
  stable book-icon fallback, never an unrelated stock meal.
- Detail titles are in the scrollable content instead of competing with toolbar
  actions. Optional video artwork, stable portion controls, checkbox ingredients
  and numbered preparation steps form a clear cooking view. Checklist state is
  UI-only and does not modify recipe data or backup contents.
- Settings use unframed sections/dividers rather than nested cards; entire
  switch rows are accessible toggles with one callback. Backup status has a
  distinct semantic surface. Existing connection and restore gates remain.
- Onboarding has a smaller brand header, animated progress, directional step
  transitions and scrollable optional setup steps. Extraction gains a stage
  timeline, animated progress, scrollable content and wrapping success metadata.
  Diagnostics use compact toolbar actions without duplicated action buttons.
- Short navigation fades/slides, list placement, favorite selection and progress
  animations use Compose animation primitives and platform duration scaling.
  No custom infinite decorative animation or animation-scale override was added.

## Verification

- Local JDK 17 offline build passes JVM tests, lint, debug APK/test APK assembly
  and minified ordinary release assembly. The final command completed successfully;
  154 JVM tests have zero failures/errors/skips. Ten release-tooling tests pass.
  This does not claim a newly signed release APK/AAB or store publication.
- Seven new Compose regressions cover nested card actions, compact 320dp
  large-font action bounds, favorite filter selection, ingredient checking,
  280dp/1.6x-font portion controls, single-callback switch rows and reachable
  success actions in a compact scrollable sheet. Four JVM tests cover safe
  artwork selection/YouTube forms/host rejection/credential-bearing URLs.
- Physical Mi 11, Android 12/API 31: final main-app suite discovers 36 tests:
  **32 pass, four external opt-in assumptions skip, zero failures** in 31.166s.
  Skips are not counted as passing cloud/inference probes.
- Same-certificate `adb install -r` updates succeeded without uninstalling or
  clearing the app. The installed debug APK was read back and matches SHA-256
  `e5928639d5b685f3cfe07dc25918911784dba5be61b7ea412ee3556e30941cc8`.
- Fresh UI observations and private screenshots confirm actual dark/light
  library layouts, loaded thumbnails and readable light-mode detail content.
  Settings toggled light and back to the original dark mode successfully. The
  real favorite filter showed its empty state and returned to all four recipes.
  These observations are not a tablet, exhaustive accessibility or performance
  benchmark. Onboarding was built, but existing onboarding state was not reset
  merely to obtain screenshots.
- Private before/final database snapshots were compared using read-only SQLite
  working copies: all fields and primary IDs of **four recipes, fifteen
  ingredients and twenty-six steps are identical**. No recipe was deleted or
  generated in this visual-only phase. Existing Drive snapshots remain untouched.

## Retained State And Limits

The Mi 11 runs the updated debug UI, with all four recipes and its original dark
mode. Private backups/screenshots remain outside Git; temporary device UI dumps
were removed. No personal recipe contents, Google account data or keys were
committed. The S25 installation was not modified.

Cloud/inference functionality was not re-run as a paid or authenticated probe
for this visual change; its separate real share-to-recovery acceptance remains
in `2026-10-23-mi11-practical-roundtrip.md`. Existing SDK/deprecation warnings,
full localization, signed-release update acceptance and broader device coverage
remain separate maintenance work.
