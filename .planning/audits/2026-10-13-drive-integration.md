# Drive Backup Integration

## Implemented Boundary

- Separate Android Identity AuthorizationClient requests only `drive.appdata`. Actual scope/token are verified, tokens are never persisted, known account selection is pinned and unexpected account changes fail closed. Drive disconnect clears only Drive state; Gemini authorization remains independent.
- `DriveBackupTransport` creates immutable multipart JSON snapshots in `appDataFolder`; fixed Google origin, redirects disabled, cancellation propagates, uploads/downloads are capped at 2 MiB. Listing is restricted to supported app properties and bounded pagination; invalid IDs cannot alter the request origin/path. No cloud deletion/overwrite or automatic sync.
- `DriveBackupCoordinator` snapshots Room, validates the versioned archive and restores missing UUIDs in one transaction. Existing recipes win conflicts. Settings and onboarding use the real authorization flow; Settings offers explicit backup/list/confirmation/restore/progress/cancel/result controls.
- Snapshots contain recipe data, not credentials, settings, logs, tokens or account identifiers. Limits: 500 recipes / 2 MiB per snapshot; up to 1,000 listed snapshots, with an explicit error rather than silent truncation beyond that. Stored recipe data is not end-to-end encrypted by the application; Google account/Drive security applies.

## Verification In Progress

- 150 JVM tests pass, including seven transport and three coordinator tests; debug/release builds and lint pass locally with JDK 17.
- Three Compose tests cover restore confirmation/cancellation, disabled concurrent actions and disconnected state. Native migration/merge tests cover distinct duplicates, local edits, identity collisions and full rollback.
- A QA run exposed stale generated `INSERT OR REPLACE` code left from the intentional D-01 mutation test. The live source was already ABORT; `--rerun-tasks` regenerated the audit variant. Generated debug/audit/release DAO code was checked for ABORT. Do not assume restoring source invalidates every cached KSP variant after mutation tests.
- Main database was stopped and backed up locally before upgrading: schema 3, three recipes, twelve ingredients, twenty steps. Only database files were copied; no preference/key/account data was inspected or copied.
- Final QA native suite: twenty-five passes and four explicit external-probe skips, twenty-nine discovered tests, 18.620 seconds. QA APK SHA-256: `fc49fedd99fb3390533317cd3038dcb74ef4de5fe6810d1a2a112cf7c2722b04`; test APK: `d0040c10d37d8e3869032dd26b085ee85cf1c002118793b00154011a5f394125`.
- Main schema-4 migration preservation, real upload/list/download/repeated restore and hosted CI remain pending. Do not infer these from isolated tests or existing OAuth consent.

## External Acceptance Probe

`DriveBackupLiveProbeTest` is explicitly gated by `driveLiveProbe=true` and refuses the QA application ID. It requires already connected Drive authorization and existing recipes, creates one actual snapshot, checks listing, restores twice with zero inserts, and compares full serialized recipe content plus local IDs. It never launches consent, deletes cloud backups or clears user data. Personal Google warnings must be confirmed by the account owner.

## Primary References

- [Google appDataFolder boundary](https://developers.google.com/workspace/drive/api/guides/appdata)
- [Google multipart upload](https://developers.google.com/workspace/drive/api/guides/manage-uploads)
