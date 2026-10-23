# Original Video Navigation

The recipe detail view now offers **Originalvideo ansehen** directly below its
optional artwork. It uses the recipe's saved `sourceUrl`, not the thumbnail URL
or a scraper's temporary media URL. HTTPS sources open through Android
ACTION_VIEW/BROWSABLE in the installed handler or browser, preserving query
parameters such as timestamps. Missing, malformed, non-HTTPS and credential-
bearing URLs do not produce a button. Missing handlers/security rejection show
a snackbar rather than crashing. No permission, account, schema or provider
configuration changed.

## Verification

- Local JVM suite: 154 passes; lint/debug APK/test APK builds succeed.
- Three new native cases cover the emitted intent/exact timestamp URL, unsafe
  source rejection and absence of a dead button for an empty source.
- Final Mi 11 native suite: 35 passes, four external opt-in assumption skips,
  zero failures (39 discovered, 35.98 seconds).
- Same-certificate debug updates installed without clearing data. Installed APK
  SHA-256 matches the local artifact:
  `f0d6cb5b0934ecc6d91182bbd068a6f88fda14eabb36e65fdf95ae740576f552`.
- In the actual Mi 11 main app, opened the newly extracted recipe and pressed
  its visible source button. Android resumed the YouTube watch activity. A fresh
  device screenshot showed the expected cooking video and player, not merely
  an outgoing intent. No advertisement or unrelated recommendation was selected.
- UI Automator did not obtain a fresh external-app tree during the handoff;
  the previous XML was not treated as YouTube evidence. Foreground activity
  readback and the fresh screenshot provide the live handoff evidence instead.
- Private before/after SQLite comparisons confirm all fields/IDs of four
  recipes, fifteen ingredients and twenty-six steps are unchanged. Backups and
  screenshots remain outside Git. The main app was brought back to foreground
  afterward; no cloud/inference call or S25 update was performed.

This verifies one real installed YouTube handler. Routing to other installed
platform apps/browser fallbacks depends on Android defaults and was not tested
on every provider/device. Intent validation tests do not claim remote playback
availability for arbitrary URLs.
