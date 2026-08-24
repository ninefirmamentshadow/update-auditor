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
Unit tests:   gradle test        (or ./gradlew test if a wrapper is present)
Debug APK:    gradle assembleDebug
Release APK:  gradle assembleRelease   (signed only when keystore material is set)
CI:           .github/workflows/build.yml runs tests + both APK paths.
NOTE

printf '\n-- reminders --\n'
cat <<'NOTE'
Read AGENTS.md before editing. This app is read-only by design: no INTERNET
permission, no telemetry, no package mutation. Do not add a network posture or
publish a debug build as an operational release without an explicit owner call.
NOTE
