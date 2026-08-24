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
- `.github/workflows/build.yml` — the only CI: unit tests, debug APK, and a
  signed release APK when keystore secrets are present.

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
- Never report success after a failed write, skipped validation, or a build
  that did not actually pass.
- **Never publish a debug build as an operational release.** The signed release
  path exists for exactly this reason.
- Setup and CI scripts must not print secrets.

## Validation

Run the smallest relevant checks that materially validate the change:

```bash
gradle test            # pure JVM unit tests — the verdict/classifier logic
gradle assembleDebug   # the app compiles
gradle assembleRelease # the shipping artifact compiles (signed if keys present)
```

A successful build is not device validation. The README records the on-device
gate; distinguish build evidence from real-device evidence.

## Git discipline

- Use terse commit messages that describe the actual diff without protected
  literals.
- Stage only files within the intended task scope unless a dependent file must
  change to keep tests or docs coherent.
- Do not force-push or rewrite published history unless the owner's current
  instruction explicitly requires it.

## Completion checklist

- The requested behavior is implemented, not merely described.
- `gradle test` and the relevant assemble task pass, or exact failures are named.
- No protected identity, location, or contact data was exposed.
- The security posture above was not silently weakened.
- Docs and tests were updated when the implementation changed their contract.
