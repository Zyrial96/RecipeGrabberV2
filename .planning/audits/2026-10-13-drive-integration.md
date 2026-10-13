# Drive Backup Integration

## Implemented Boundary

- Separate Android Identity AuthorizationClient requests only `drive.appdata`. Actual scope/token are verified, tokens are never persisted, known account selection is pinned and unexpected account changes fail closed. Drive disconnect clears only Drive state; Gemini authorization remains independent.
- `DriveBackupTransport` creates immutable multipart JSON snapshots in `appDataFolder`; fixed Google origin, redirects disabled, cancellation propagates, uploads/downloads are capped at 2 MiB. Listing is restricted to supported app properties and bounded pagination; invalid IDs cannot alter the request origin/path. No cloud deletion/overwrite or automatic sync.
- `DriveBackupCoordinator` snapshots Room, validates the versioned archive and restores missing UUIDs in one transaction. Existing recipes win conflicts. Settings and onboarding use the real authorization flow; Settings offers explicit backup/list/confirmation/restore/progress/cancel/result controls.
- Snapshots contain recipe data, not credentials, settings, logs, tokens or account identifiers. Limits: 500 recipes / 2 MiB per snapshot; up to 1,000 listed snapshots, with an explicit error rather than silent truncation beyond that. Stored recipe data is not end-to-end encrypted by the application; Google account/Drive security applies.

## Local And Device Verification

- 150 JVM tests pass, including seven transport and three coordinator tests; debug/release builds and lint pass locally with JDK 17.
- Three Compose tests cover restore confirmation/cancellation, disabled concurrent actions and disconnected state. Native migration/merge tests cover distinct duplicates, local edits, identity collisions and full rollback.
- A QA run exposed stale generated `INSERT OR REPLACE` code left from the intentional D-01 mutation test. The live source was already ABORT; `--rerun-tasks` regenerated the audit variant. Generated debug/audit/release DAO code was checked for ABORT. Do not assume restoring source invalidates every cached KSP variant after mutation tests.
- Main database was stopped and backed up locally before upgrading: schema 3, three recipes, twelve ingredients, twenty steps. Only database files were copied; no preference/key/account data was inspected or copied.
- Final QA native suite: twenty-five passes and four explicit external-probe skips, twenty-nine discovered tests, 18.620 seconds. QA APK SHA-256: `fc49fedd99fb3390533317cd3038dcb74ef4de5fe6810d1a2a112cf7c2722b04`; test APK: `d0040c10d37d8e3869032dd26b085ee85cf1c002118793b00154011a5f394125`.
- Main APK SHA-256: `498bde20819dd6d2f3de6535f60b9f6185de9b8233f5a1e222c71ef0664985b7`; test APK: `d2ee1e38e64414ef674a9c8cdac9a9c2112dd0b7abe5a1bc716f5a19fe6127c6`. Main native suite: twenty-five passes/four external skips, 18.333 seconds.
- Main schema-4 migration is verified against the pre-upgrade database: all three recipe records/previous fields, twelve ingredient records and twenty step records are unchanged; three unique 36-character UUIDs were added. Gemini OAuth remained visibly connected, and no credential-unavailable warning appeared. Actual credential values were never read.
- Settings Drive connection completed without another personal warning and loaded the empty cloud list with a usable token. The SDK did not return an email for display; UI says `Google Drive connected`, not an invented account identity. Known account pinning applies when account metadata is available; no additional identity/profile/email scope is requested.
- Explicit main-app live probe passed in 5.590 seconds: one real snapshot uploaded and appeared in listing; download/validation plus two transactional restores each inserted zero recipes and skipped all three. Full serialized content (fixed archive timestamp) and local IDs matched before/after. The cloud snapshot is intentionally retained; no overwrite/delete or IAM/billing/API configuration change occurred.
- User-facing UI acceptance: `Refresh backups` displayed the live snapshot; `Backup now` created a second retained snapshot with `Backup saved: 3 recipes`; `Restore` displayed the explicit local-win confirmation, and confirming returned `Restored 0 recipes; 3 existing recipes kept`. Both snapshots were visible. A Mi 11 screenshot verified readable controls/status without overlap. No raw keys/account identifiers were exposed.
- Drive disconnect was checked on the real device: connection controls returned, cloud-retention status appeared, and after process restart all three recipe cards still showed four ingredients. Gemini OAuth remained connected and no credential-unavailable warning appeared.
- Drive was reconnected after that restart; both retained snapshots were listed again. The device was returned to its normal recipe library with Drive connected. Installation server/reverse were stopped; temporary UI dumps were removed.

## Hosted CI

[Android Checks run 38441275898](https://github.com/Zyrial96/RecipeGrabberV2/actions/runs/38441275898) targets runtime commit `575b965dbf91bbd32d04762dab40a281127363d4`: build/unit/lint/debug/release and both instrumentation jobs (API 31 and 35) completed successfully. External OAuth/Drive/generation probes remain skipped in CI. The final evidence/map update is documentation-only and does not change the verified runtime.

## Remaining Limits

- This is early-stage hardening plus manual snapshots, not a production-readiness certification or an automatic sync system.
- Actual Android system backup/device transfer, historical Room 1/2 upgrade chains, second-device/clean-install cloud recovery and minified-release device behavior remain unverified. Empty-database insertion/conflicts/rollback are native fixture tests; the real cloud acceptance deliberately preserves the existing library.
- Known Drive account pinning is conditional on returned identity metadata. Scope-only SDK results may not contain an email; UI reports only a generic connection in that case. No unverified email/account identity is invented.
- No snapshot pruning/cloud deletion is offered; snapshots are retained until the user removes app data via Google Drive controls. Listing is bounded, with an explicit failure if incomplete; there is no silent automatic retry or alternate model/provider.

## External Acceptance Probe

`DriveBackupLiveProbeTest` is explicitly gated by `driveLiveProbe=true` and refuses the QA application ID. It requires already connected Drive authorization and existing recipes, creates one actual snapshot, checks listing, restores twice with zero inserts, and compares full serialized recipe content plus local IDs. It never launches consent, deletes cloud backups or clears user data. Personal Google warnings must be confirmed by the account owner.

## Primary References

- [Google appDataFolder boundary](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Google multipart upload](https://developers.google.com/workspace/drive/api/guides/manage-uploads)
