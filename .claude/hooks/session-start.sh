#!/bin/bash
# SessionStart orientation.
#
# Adapted from Sovereign-Ops. This repository installs nothing at session start;
# the hook only reports repository state so a session opens knowing where it is,
# what is dirty, and how to build and test. Every command is read-only and its
# failure is reported rather than raised — a session that opens and says what is
# wrong beats one that refuses to open.
set -uo pipefail

cd "${CLAUDE_PROJECT_DIR:-.}" || exit 0

printf '%s\n' '=== Update Auditor session state ==='

printf '\n-- git --\n'
branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
printf 'branch: %s\n' "$branch"
dirty="$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
printf 'uncommitted paths: %s\n' "$dirty"
git log -1 --format='last commit: %h %s (%cr)' 2>/dev/null || printf 'last commit: (none)\n'

printf '\n-- build & test --\n'
cat <<'NOTE'
Verify:       scripts/verify.sh [--handoff]   (tests, screenshots, lint, debug build, inventory)
Release:      scripts/release.sh              (refuses unsigned/debuggable; owner supplies keys)
SDK:          scripts/bootstrap-android-sdk.sh, then export ANDROID_HOME
CI:           .github/workflows/build.yml runs scripts/verify.sh, then release.sh when keystore secrets exist.
NOTE

printf '\n-- reminders --\n'
cat <<'NOTE'
Read AGENTS.md, SOVEREIGN_EXECUTION_PROTOCOL.md (standing directive: execute, verify
alongside, minimal friction) and OPEN_SOURCE_COMPLIANCE_PROTOCOL.md (verify licenses,
keep notices, record every dependency in third_party.json) before editing. Read your
branch's handoff file in .agent/handoffs/ first if it exists. This app is read-only by
design: no INTERNET permission, no telemetry, no package mutation. Do not add a network
posture or publish a debug build as an operational release without an explicit owner call.
NOTE
