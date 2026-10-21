# AGENTS.md — Update Auditor execution rules

These rules apply to automated coding agents working anywhere in this
repository. They are the single-app adaptation of the Sovereign-Ops execution
doctrine; Sovereign-Ops remains the umbrella source of truth.

## What this app is

Update Auditor is a **read-only** Android device-state and package-provenance
auditor for a mobile-first workflow. It reports local observations only and
never claims that firmware or an app is current unless an upstream source has
actually been checked. See `README.md` and `docs/V0_2_DESIGN.md`.

## Repository map

- `app/` — the Android application (Kotlin).
- `docs/` — design notes. A design document does not by itself authorize its
  own implementation; the owner's current instruction does.
- `scripts/` — the portable pipeline: `verify.sh` (tests, screenshot goldens,
  lint, debug build, third-party inventory, whitespace; PASS/FAIL/NOT_RUN with the
  HEAD sha), `release.sh`, `bootstrap-android-sdk.sh`, `check_third_party.py`,
  `handoff.py`. CI and agents run the same scripts.
- `.agent/` — the branch handoff schema and one handoff file per active branch.
- `third_party.json`, `THIRD_PARTY_NOTICES.md`, `licenses/` — the component inventory.
- `.github/workflows/build.yml` — the only CI: runs `scripts/verify.sh`, and a
  signed release APK via `scripts/release.sh` when keystore secrets are present.

## Standing operating directive

`SOVEREIGN_EXECUTION_PROTOCOL.md` is the owner's default working style for every agent and workflow in this repository: execution first, verification alongside the work, minimal friction, risk-proportionate intervention, deliverables over commentary. Read it in full before starting. It applies unless the owner overrides it for a specific task; the security posture and protected data boundary below are its §4 boundaries made concrete.

## Open-source reuse directive

`OPEN_SOURCE_COMPLIANCE_PROTOCOL.md` is the owner's standing directive for researching and reusing open-source code: verify the license at a pinned upstream ref, prefer MIT / Apache-2.0 / BSD / ISC, treat GPL / AGPL / LGPL / MPL and unlicensed projects as ideas only, keep notices, and record every dependency and copied snippet in `third_party.json` and `THIRD_PARTY_NOTICES.md` (`scripts/check_third_party.py` enforces this in `scripts/verify.sh`). It supplements the execution protocol above; the security posture below still limits what may be adopted. Read it before adding a dependency, copying a snippet, or porting an idea from another project.

## Scope and authority

1. Follow the owner's current explicit instruction.
2. Read this file and `README.md` before editing.
3. Prefer the lowest-risk reversible implementation. Mark unresolved values
   `OWNER DECISION`, `LIVE VERIFICATION REQUIRED`, or `TODO` and continue.
4. Green CI is mechanical evidence, not semantic proof. Fix failures
   attributable to the current change without waiting for another prompt.

Clarification is reserved for genuine owner-level boundaries: a change to the
security/network posture (see below), irreversible deletion, credential or
signing changes, or an externally published owner statement.

## Security posture — do not weaken without an owner call

This app's value is its restraint. The following are load-bearing invariants,
not defaults to relax for convenience:

- **No `INTERNET` permission** and no network code in v0.1. v0.2's upstream
  checks are a deliberate posture change (`docs/V0_2_DESIGN.md`) and require an
  explicit owner decision before the permission is added.
- No telemetry, analytics, account, VPN, accessibility service, device-admin
  capability, updater, installer, or package-mutation path.
- `allowBackup=false`, cleartext traffic disabled.
- Package inventory is displayed locally and is not exported.
- `UNKNOWN` / `UNVERIFIED` / `MANUAL_CHECK_REQUIRED` are never rendered or
  reasoned about as `OUTDATED`.

## Protected data boundary

Never add, infer, reconstruct, print, or commit the owner's legal identity,
physical location detail, sleeping/real-time location, operational contact
numbers, credentials, or signing/recovery secrets. Keep them out of code,
fixtures, logs, filenames, commit messages, and build artifacts unless the
owner explicitly provides a sanitized value for that specific output.

## Code and data rules

- Prefer simple, auditable implementations and explicit failure handling.
- Verdict logic must be deterministic and unit-testable without Android or a
  network. Providers return observations; a verdict engine decides policy.
- A first-party package **name** is only a claim: FIRST_PARTY needs the platform
  signature or a pinned signer digest. Pins are in
  `app/src/main/assets/first_party_pins.txt` and are the owner's to add; never invent
  or guess a digest, and never promote a name-only match to FIRST_PARTY.
- Package inventory and signer digests are display-only: no copy, share or export action.
- Never report success after a failed write, skipped validation, or a build
  that did not actually pass.
- **Never publish a debug build as an operational release.** The signed release
  path exists for exactly this reason.
- Setup and CI scripts must not print secrets.

## Validation

```bash
scripts/verify.sh              # test (incl. screenshot goldens), lintDebug, assembleDebug,
                               # third-party inventory, git diff --check
scripts/verify.sh --handoff    # same, and record results in .agent/handoffs/<branch>.json
scripts/release.sh             # signed release only; refuses unsigned/debuggable output
./gradlew recordRoborazziDebug # re-record screenshot goldens after an intended UI change
```

`verify.sh` prints PASS, FAIL or NOT_RUN per step with the HEAD sha. A missing SDK
or JDK is NOT_RUN plus the bootstrap command and is never reported as a pass.
Device and instrumented tests are not part of it and are always NOT_RUN there.
A successful build is not device validation. The README records the on-device
gate; distinguish build evidence from real-device evidence.

Only `verify.sh` writes `verified` in a handoff file; agents write `claimed`,
`pending` and `next_action`. One agent owns an active branch; others read or review.

## Git discipline

- Use terse commit messages that describe the actual diff without protected
  literals.
- Stage only files within the intended task scope unless a dependent file must
  change to keep tests or docs coherent.
- Do not force-push or rewrite published history unless the owner's current
  instruction explicitly requires it.

## Completion checklist

- The requested behavior is implemented, not merely described.
- `scripts/verify.sh` passes at the commit being pushed, or the exact FAIL / NOT_RUN
  steps are named.
- No protected identity, location, or contact data was exposed.
- The security posture above was not silently weakened.
- Docs and tests were updated when the implementation changed their contract.
