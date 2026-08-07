# Jules task — add Samsung SM-X218U support to Update Auditor v0.2

## Repository state

Work only on branch `agent/update-auditor-v0.2-implementation` / draft PR #3 unless the owner explicitly directs otherwise.

Do **not** merge PR #3. The owner will perform the physical-device gate before merge.

Start by running:

```bash
bash scripts/setup-dev.sh
```

If the host already has all required tools and you only need environment validation:

```bash
bash scripts/setup-dev.sh --no-build
```

The bootstrap contract is JDK 17, Gradle 8.9, Android compile platform 35, and build-tools 35.0.0.

## Objective

Extend the existing v0.2 Samsung official-history provider to support the owner's Galaxy Tab A9+ 5G without weakening the source/verdict architecture or introducing background networking.

### Device observation from physical SM-X218U

Observed in Update Auditor v0.2 on 2026-08-07:

- manufacturer: `samsung`
- model: `SM-X218U`
- device: `gta9p`
- product: `gta9psqxnc`
- Android: `16` / SDK 36
- security patch: `2026-06-05`
- Android build ID: `BP2A.250605.031.A3`
- resolved Samsung firmware build: `X218USQSCEZF3`
- baseband: `X218USQSCEZF3`
- fingerprint includes `X218USQSCEZF3`

The current v0.2 result is intentionally `UNSUPPORTED` because only SM-A166U is registered.

## Verified authoritative Samsung source

Register this exact official Samsung history page for `SM-X218U`:

`https://doc.samsungmobile.com/SM-X218U/028554240115/eng.html`

At task creation, its newest relevant entry is:

- build: `X218USQSCEZF3`
- Android version: `B(Android 16)`
- release date: `2026-06-25`
- security patch: `2026-06-05`

Those values match the physical device observation above. The resulting v0.2 Samsung firmware verdict on this device should therefore be `VERIFIED_CURRENT` when the page is successfully fetched and parsed.

Do not hard-code the expected newest build into verdict logic. The Samsung page is the runtime authority; the build above is a regression fixture/acceptance observation only.

## Required changes

1. Add `SM-X218U` to the existing Samsung source registry using the verified official URL above.
2. Add/extend pure unit tests so registry lookup is case-insensitive and covers both supported models:
   - `SM-A166U`
   - `SM-X218U`
3. Add a parser/verdict regression fixture representing at least the newest two SM-X218U releases and prove:
   - local `X218USQSCEZF3` => `VERIFIED_CURRENT`;
   - local `X218USQSCEZE8` => `VERIFIED_UPDATE_AVAILABLE` with `X218USQSCEZF3` as upstream newest.
4. Preserve all existing v0.2 verdict semantics. In particular:
   - never order Samsung builds lexicographically;
   - unknown build is not automatically outdated;
   - rollout/history availability is not equivalent to an immediately downloadable OTA;
   - source failure degrades to `ERROR`/`UNVERIFIED`, not a guessed verdict.
5. Preserve the explicit-network-only posture:
   - no background jobs;
   - no telemetry;
   - no inventory upload;
   - no automatic updates;
   - local audit remains offline-capable.
6. Do not broaden scope to Neo/F-Droid, Obtainium, Play Store, Galaxy Store, signer continuity, notifications, or update installation.
7. Run the full bootstrap/build and leave PR #3 draft.

## Acceptance gates before handing back

Automated:

- `bash scripts/setup-dev.sh` succeeds;
- all existing tests remain green;
- new SM-X218U tests pass;
- debug APK builds;
- no unrelated dependency churn.

Owner device gate after Jules finishes:

- SM-X218U displays firmware `X218USQSCEZF3` separately from Android build ID `BP2A.250605.031.A3`;
- explicit Samsung check returns `VERIFIED_CURRENT` against Samsung official history;
- offline Samsung check fails cleanly while local inventory still works;
- Rethink shows Samsung traffic only during explicit check and no background traffic afterward;
- system updater handoff remains system-owned/fail-safe;
- repeated checks/orientation do not duplicate or corrupt status.

## Deliverable

Commit the minimal changes to the existing implementation branch and report:

- files changed;
- tests added/changed;
- exact commands run;
- CI/build result;
- any source-format assumptions or unresolved risks.

Do not merge.
