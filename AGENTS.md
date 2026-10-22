# AGENTS.md — Update Auditor execution rules

These rules apply to automated coding agents working anywhere in this
repository. They are the single-app adaptation of the Sovereign-Ops execution
doctrine; Sovereign-Ops remains the umbrella source of truth for doctrine; this file is canonical for agents in this repository.

## Canonical source

This file is the **canonical** instruction source for every coding agent (Claude Code, Codex/ChatGPT, Gemini/Jules, Cursor, Copilot, Grok, others). `CLAUDE.md`, `GEMINI.md`, `.cursor/rules/agents.mdc` and `.github/copilot-instructions.md` are thin pointers to it; edit rules here, never there. `sh scripts/check-agent-pointers.sh` enforces this.

## Owner principles (highest priority after an explicit owner instruction)

1. **Source-of-truth order.** Live GitHub for code, CI and deploys; the owner's file storage for source files and masters; any notes hub is only a sanitized index. When they disagree, trust them in that order and report the conflict.
2. **Fact, inference, speculation.** Label which is which. Never invent test results, build status, or device behaviour. Report the exact commit, tests run, artifact status and blockers (see "Verification report").
3. **Authorization.** No merging, releasing, or publishing without explicit owner authorization. This repository is public: work on a branch and open a PR; do not push directly to the default branch. Never force-push or rewrite pushed history without owner OK.
4. **Reversible, high return.** Prefer small reversible changes with high net return on time. Bug fixes are test-first (failing test, then fix). Never retune heuristics or classifiers against known fixtures to raise scores; document held-out misses honestly.
5. **Data invariants.** No user data is silently discarded. Every transformation is compared against its input, confirmed before it is destructive, and recoverable.
6. **Privacy.** Never search for, reconstruct, or commit legal identity, residence, contact handles, client info, medical, benefits, or financial amounts. Fixtures are synthetic only (e.g. `MERIDIAN`, `Ashford`, `919-555-0142`, `example.invalid`). Persona or voice material is imported by the user at runtime, never bundled. Identity layers stay distinct.
7. **No invented biography.** Use `[TK: …]` placeholders for any biographical detail not supplied by the owner.
8. **Credentials.** Never print or commit secrets, tokens, keystores, or signing passwords. If access is missing, name exactly which access (service, scope, secret name) rather than asking for pasted credentials.
9. **Phase through 2026-12-31: consolidation and conversion.** At most three active bets; no new lanes, features outside the request, or new projects without the owner's say.
10. **Advisory tools never block** the user's own writing, saving, or copying. Warn, don't gate.
11. **Escalate only real, consequential decisions** (irreversible, public, costly, privacy-affecting, posture-changing, or architectural). Proceed on ordinary reversible ones.

Where older text conflicts with these principles, these principles win.

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

## Standing operating directive

`SOVEREIGN_EXECUTION_PROTOCOL.md` is the owner's default working style for every agent and workflow in this repository: execution first, verification alongside the work, minimal friction, risk-proportionate intervention, deliverables over commentary. Read it in full before starting. It applies unless the owner overrides it for a specific task; the security posture and protected data boundary below are its §4 boundaries made concrete.

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

## Verification report and definition of done

Every handoff or completion report states, as facts with evidence:

- **Commit:** full SHA and branch; whether pushed; PR link.
- **Tests:** exact commands and results. Never paraphrase a run that did not happen; say NOT_RUN and why (e.g. no Android SDK).
- **Build:** status per artifact; CI result or "CI not run (reason)". GitHub Actions may be unavailable (billing); then give local results.
- **Blockers / missing access:** precise, no credentials.

Status labels, each claimed only with its own evidence: `implemented` · `compiled` · `tested` · `device-tested` · `signed` · `delivered` · `merged` · `published`. A higher label never implies a lower one was checked.

**Done** means: requested change implemented; relevant tests added/updated and run; `sh scripts/check-agent-pointers.sh` passes; docs touched by the change updated; nothing private or secret added; report written with `docs/agents/HANDOFF_TEMPLATE.md`. Merge/release only if authorized.
