---
name: verify-repo
description: Verify this repository with scripts/verify.sh, fix what fails, re-run, and record the results in the branch handoff file. Use before every push, when asked whether the branch is green, or when picking up another agent's branch.
---

# Verify this repository

The logic lives in `scripts/verify.sh`. CI runs the same script, so this skill only
wraps it. Do not re-implement the checks here.

1. Read `.agent/handoffs/<branch with / as ->.json` if it exists, before anything else.
   `claimed` is what the last agent said it did; `verified` is the only evidence.
2. Run `scripts/verify.sh`. On a constrained machine add
   `VERIFY_GRADLE_ARGS="--max-workers=2"`; on a flaky network add `VERIFY_RETRIES=2`.
3. Read the PASS / FAIL / NOT_RUN table.
   - FAIL: read the log it names under `build/verify-logs/`, fix the cause, re-run.
     A changed screenshot is either a regression (fix the UI) or an intended change
     (`./gradlew recordRoborazziDebug`, then review the PNG diff before committing).
     Never edit a golden by hand and never loosen a check to get a pass.
   - NOT_RUN: say so. A missing SDK is NOT_RUN plus the bootstrap command; it is never
     a pass. Device and instrumented tests are always NOT_RUN here.
4. When it is green at the commit you are about to push, run `scripts/verify.sh --handoff`
   to write `verified` into the handoff file. Only that script writes `verified`.
5. Update `claimed`, `pending` and `next_action` in the handoff file by hand at every stop.
6. Report what actually ran. One agent owns an active branch; do not push a branch you
   do not own.

New dependency? It must be in `gradle/libs.versions.toml`, `third_party.json` and
`THIRD_PARTY_NOTICES.md` (see `OPEN_SOURCE_COMPLIANCE_PROTOCOL.md`); `verify.sh` fails
otherwise.
