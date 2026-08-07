# Update Auditor v0.2 — Update Status Design

Status: design only. No v0.2 implementation is authorized by this document by itself.

## Goal

Turn the v0.1 local inventory into a source-aware answer to: **which update channels can be verified, which appear current, which have a verified newer applicable release, and which cannot be checked programmatically?**

The app must not collapse unrelated update mechanisms into one misleading "up to date" badge.

## Core rule

Every verdict carries:

- channel;
- local observation;
- upstream observation, if any;
- source;
- checked-at time;
- confidence/verdict state.

`UNKNOWN`, `UNVERIFIED`, and `MANUAL_CHECK_REQUIRED` are not `OUTDATED`.

A source that reports a build older than the local build must not cause an outdated verdict. It becomes `LOCAL_AHEAD_OF_SOURCE` or `SOURCE_MISMATCH` until reconciled.

## Verdict vocabulary

- `VERIFIED_CURRENT` — authoritative/applicable upstream source reports the installed value as the newest applicable value.
- `VERIFIED_UPDATE_AVAILABLE` — authoritative/applicable upstream source reports a newer value than the installed value.
- `LOCAL_AHEAD_OF_SOURCE` — local state is newer than the source's newest observation; source may lag or describe a different rollout channel.
- `MANUAL_CHECK_REQUIRED` — Android/platform APIs do not expose a reliable machine-readable answer to this app.
- `UNSUPPORTED` — no provider exists for this channel/device yet.
- `UNVERIFIED` — data exists but is insufficient for a defensible current/outdated claim.
- `ERROR` — provider attempted a check and failed.

## v0.2 channel matrix

| Channel | v0.2 local data | v0.2 upstream data | Verdict capability |
| --- | --- | --- | --- |
| Samsung firmware | model, build, security patch, baseband | Samsung official update-history page for supported model | verified current/update available when exact build history is applicable |
| Android security patch | installed patch date | Samsung firmware history entry, not generic Android bulletin date | verified only as part of matched Samsung firmware channel |
| Samsung OTA pending state | local build only | ordinary app cannot query pending OTA authoritatively | manual check required + secure Settings shortcut |
| Google Play system / Mainline | installed module/APEX inventory where exposed | no reliable global "latest applicable" API in v0.2 | observed/unverified + manual check |
| Google Play apps | installer/source metadata | Play in-app update API applies only to the calling app | manual check required |
| Galaxy Store apps | installer/source metadata | no general update-status provider in v0.2 | manual check required |
| Neo/F-Droid apps | installer/source metadata | deferred | unsupported in v0.2 |
| Obtainium/direct-source apps | installer/source metadata | source URL is not available cross-app through ordinary Android sandbox | unsupported in v0.2 |
| First-party apps | package/version metadata | repository/release adapter deferred | local only in v0.2 |

## Samsung provider

### Initial supported device

`SM-A166U`

Official Samsung update history:

`https://doc.samsungmobile.com/SM-A166U/031752241216/eng.html`

The provider must parse only the structured release facts needed for a verdict:

- build number;
- Android version;
- release date;
- security patch level;
- page ordering.

Do not ingest or depend on the long release-note prose.

### Comparison algorithm

1. Read the runtime model/build/security patch from the existing v0.1 local auditor.
2. Fetch the registered Samsung update-history URL only after explicit user action.
3. Parse release entries in page order.
4. Treat the first valid release entry as the source's newest observation.
5. If the local build exactly equals that build: `VERIFIED_CURRENT`.
6. If the local build exactly matches an older entry: `VERIFIED_UPDATE_AVAILABLE`, with the newest entry as the candidate update.
7. If the local build does not occur in the page:
   - never infer ordering from Samsung build strings alone;
   - if the local security patch is newer than the source's newest patch, report `LOCAL_AHEAD_OF_SOURCE`;
   - otherwise report `UNVERIFIED` or `SOURCE_MISMATCH`.
8. Never say an OTA is immediately downloadable merely because a newer history entry exists; rollout eligibility can vary by carrier/service-provider context.

The model-to-source URL mapping must live in one small registry so adding another model does not change the parser or verdict engine.

## Network posture change

v0.1 deliberately had no `INTERNET` permission. v0.2 will require it for explicit upstream checks.

Hard constraints:

- no background network activity;
- no periodic jobs;
- no telemetry or analytics;
- no account;
- no package inventory upload;
- no device identifiers sent beyond what is intrinsically present in the selected public provider URL/request;
- network code isolated under an `upstream/` package;
- local inventory remains usable offline;
- UI must visibly distinguish cached data from a fresh check;
- failures must degrade to `ERROR`/`UNVERIFIED`, not optimistic or pessimistic guesses.

## Proposed architecture

```text
local/
  DeviceAuditor
  PackageAuditor
  ModuleAuditor

status/
  UpdateChannel
  UpdateVerdict
  ChannelStatus
  VerdictEngine

upstream/
  UpdateProvider
  samsung/
    SamsungSourceRegistry
    SamsungHistoryProvider
    SamsungHistoryParser

ui/
  Status screen
  Existing package inventory
```

`UpdateProvider` returns observations. It does not decide policy. `VerdictEngine` compares local and upstream observations using deterministic rules that can be unit-tested without Android or network access.

## UI

The default screen becomes a compact channel dashboard above or separate from the package inventory.

Example:

```text
UPDATE STATUS

Samsung firmware       VERIFIED CURRENT
Installed              <runtime build>
Latest source build     <source build>
Checked                 8:42 AM
Source                  Samsung official history

Samsung OTA             MANUAL CHECK REQUIRED
[Open system update settings]

Google Play system      UNVERIFIED
Local modules observed
[Open relevant settings]

Play Store apps         MANUAL CHECK REQUIRED
Galaxy Store apps       MANUAL CHECK REQUIRED
Neo/F-Droid             UNSUPPORTED IN v0.2
Obtainium                UNSUPPORTED IN v0.2
```

No single overall green/red score in v0.2. The channels are independent and have different authority boundaries.

## Settings handoff

For system-update checks the app may launch Android's system-update settings intent, but only after resolving it to a system handler as required by current Android guidance. Failure to resolve must be handled cleanly.

This handoff is navigation, not an update verdict.

## Tests

Pure unit tests must cover at minimum:

- exact local/latest match -> `VERIFIED_CURRENT`;
- local matches older source entry -> `VERIFIED_UPDATE_AVAILABLE`;
- local build absent + newer local patch -> `LOCAL_AHEAD_OF_SOURCE`;
- local build absent + ambiguous dates -> `UNVERIFIED`;
- malformed/empty Samsung page -> `ERROR` without crash;
- provider URL missing for model -> `UNSUPPORTED`;
- parser ignores prose and extracts only release facts;
- no Samsung build-string lexicographic comparison is used as an ordering rule.

## Device gate

Before merge:

- v0.1 local inventory behavior remains intact;
- explicit Samsung check completes on the target device;
- the current runtime build is matched against Samsung's official history without false downgrade/update claims;
- disabling connectivity makes the provider fail cleanly while local data remains available;
- Rethink shows network traffic only during explicit online check;
- no background traffic appears after force-stop/relaunch/idle;
- system-update settings shortcut resolves to a system component or fails safely;
- orientation/repeated checks do not duplicate or corrupt status state.

## Deferred beyond v0.2

- F-Droid/Neo repository index adapters;
- first-party GitHub release adapters;
- user-supplied Obtainium source registry/import;
- signer continuity;
- persistent snapshots and deltas;
- generic Samsung model discovery;
- carrier/CSC-aware Samsung source selection beyond the initial registered model;
- automatic update installation;
- background monitoring or notifications.

## References

- Samsung official SM-A166U update history: https://doc.samsungmobile.com/SM-A166U/031752241216/eng.html
- Android `AppUpdateManager`: https://developer.android.com/reference/com/google/android/play/core/appupdate/AppUpdateManager
- Android `DevicePolicyManager.getPendingSystemUpdate`: https://developer.android.com/reference/android/app/admin/DevicePolicyManager#getPendingSystemUpdate(android.content.ComponentName)
- Android `PackageManager` / installed modules and APEX support: https://developer.android.com/reference/android/content/pm/PackageManager
- Android system update settings intent: https://developer.android.com/reference/android/provider/Settings#ACTION_SYSTEM_UPDATE_SETTINGS
