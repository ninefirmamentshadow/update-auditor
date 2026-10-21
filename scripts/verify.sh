#!/usr/bin/env bash
# Portable verification for this repository. CI, a laptop and an agent all run this
# same script, so "green" means the same thing everywhere.
#
#   scripts/verify.sh [--handoff]
#
# Steps: unit + screenshot tests, lint, debug build, third-party inventory,
# whitespace check. Each prints PASS, FAIL or NOT_RUN with the HEAD sha. A step that
# could not run (no Android SDK, no JDK, no Python) is NOT_RUN and is never shown as
# a pass. Exit code: 0 = every step PASS, 1 = at least one FAIL, 2 = nothing failed
# but at least one step was NOT_RUN.
#
# --handoff   also record the results in .agent/handoffs/<branch>.json (`verified`).
#             This script is the only writer of that field.
#
# Environment:
#   VERIFY_GRADLE_ARGS   extra Gradle arguments (default empty; e.g. "--max-workers=2")
#   VERIFY_RETRIES       times to retry a Gradle step whose log shows a dependency
#                        download problem (default 0). Test or lint failures are
#                        never retried.
#   ANDROID_HOME         Android SDK location (also ANDROID_SDK_ROOT, ~/android-sdk)
set -uo pipefail

cd "$(dirname "$0")/.."
ROOT="$PWD"

WRITE_HANDOFF=0
for arg in "$@"; do
    case "$arg" in
        --handoff) WRITE_HANDOFF=1 ;;
        -h|--help) sed -n '2,24p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "unknown argument: $arg" >&2; exit 64 ;;
    esac
done

FULL_SHA="$(git rev-parse HEAD 2>/dev/null || echo unknown)"
SHORT_SHA="$(git rev-parse --short=12 HEAD 2>/dev/null || echo unknown)"
BRANCH="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
DIRTY_COUNT="$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
if [ "$DIRTY_COUNT" != "0" ]; then
    TREE_NOTE="uncommitted changes: $DIRTY_COUNT path(s); results describe the working tree, not only the commit"
else
    TREE_NOTE="clean"
fi

LOG_DIR="$ROOT/build/verify-logs"
mkdir -p "$LOG_DIR"
RESULTS_TSV="$LOG_DIR/results.tsv"
: > "$RESULTS_TSV"

GRADLE_ARGS=(--no-daemon)
if [ -n "${VERIFY_GRADLE_ARGS:-}" ]; then
    # shellcheck disable=SC2206
    GRADLE_ARGS+=(${VERIFY_GRADLE_ARGS})
fi
RETRIES="${VERIFY_RETRIES:-0}"

# --- environment probes -------------------------------------------------------
SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
BOOTSTRAP_HINT="run scripts/bootstrap-android-sdk.sh, then export ANDROID_HOME=$SDK"
NO_GRADLE_REASON=""
if ! command -v java >/dev/null 2>&1; then
    NO_GRADLE_REASON="no JDK on PATH (install JDK 17+)"
elif [ ! -d "$SDK/platforms/android-35" ] || [ ! -d "$SDK/build-tools/35.0.0" ]; then
    NO_GRADLE_REASON="Android SDK (platforms;android-35, build-tools;35.0.0) not found at $SDK; $BOOTSTRAP_HINT"
else
    export ANDROID_HOME="$SDK"
fi
NO_PYTHON_REASON=""
command -v python3 >/dev/null 2>&1 || NO_PYTHON_REASON="python3 not found"

# --- step runner --------------------------------------------------------------
CHECKS=()
RESULTS=()
COMMANDS=()
DETAILS=()

record() { # name command result detail
    CHECKS+=("$1"); COMMANDS+=("$2"); RESULTS+=("$3"); DETAILS+=("$4")
    local clean_detail="${4//$'\t'/ }"
    clean_detail="${clean_detail//$'\n'/ }"
    printf '%s\t%s\t%s\t%s\n' "$1" "$2" "$3" "$clean_detail" >> "$RESULTS_TSV"
}

resolution_problem() { # logfile
    grep -qE 'status code 429|Could not resolve|Could not download|Could not GET|Read timed out|Connection reset|MavenArtifactFetcher|was not found in any of the following' "$1"
}

run_step() { # name blocked-reason display command...
    local name="$1" blocked="$2" display="$3"; shift 3
    if [ -n "$blocked" ]; then
        record "$name" "$display" NOT_RUN "$blocked"
        return
    fi
    local log="$LOG_DIR/$name.log" attempt=0
    while :; do
        if "$@" > "$log" 2>&1; then
            record "$name" "$display" PASS ""
            return
        fi
        if [ "$attempt" -lt "$RETRIES" ] && resolution_problem "$log"; then
            attempt=$((attempt + 1))
            echo "  $name: dependency download problem, retry $attempt/$RETRIES" >&2
            sleep $((attempt * 15))
            continue
        fi
        local note="see $log"
        resolution_problem "$log" && note="possible dependency download/network problem; $note"
        record "$name" "$display" FAIL "$note"
        echo "---- last lines of $name ----" >&2
        tail -n 25 "$log" >&2
        return
    done
}

gw() { ./gradlew "${GRADLE_ARGS[@]}" "$@"; }
GW_SHOWN="./gradlew ${GRADLE_ARGS[*]}"

# Every module that ships in the APK, transitive ones included, must be inventoried.
runtime_inventory() {
    gw -q printRuntimeDependencies > /dev/null &&
        python3 scripts/check_third_party.py --resolved app/build/runtime-dependencies.txt
}

echo "verify: HEAD $SHORT_SHA on $BRANCH, tree $TREE_NOTE"

run_step test          "$NO_GRADLE_REASON" "$GW_SHOWN test"          gw test
run_step lint          "$NO_GRADLE_REASON" "$GW_SHOWN lintDebug"     gw lintDebug
run_step assembleDebug "$NO_GRADLE_REASON" "$GW_SHOWN assembleDebug" gw assembleDebug
run_step third-party   "$NO_PYTHON_REASON" "python3 scripts/check_third_party.py" python3 scripts/check_third_party.py
run_step runtime-deps  "${NO_GRADLE_REASON:-$NO_PYTHON_REASON}" "$GW_SHOWN printRuntimeDependencies; check_third_party.py --resolved" runtime_inventory
if ls .agent/handoffs/*.json >/dev/null 2>&1; then
    run_step handoff-schema "$NO_PYTHON_REASON" "python3 scripts/handoff.py validate" python3 scripts/handoff.py validate
fi
run_step diff-check    "" "git diff --check; git show --check HEAD" \
    bash -c 'git diff --check && git diff --cached --check && git show --check --format= HEAD'

# --- report -------------------------------------------------------------------
echo
printf '%-16s %-8s %s\n' CHECK RESULT COMMAND
FAILS=0; NOTRUNS=0
for i in "${!CHECKS[@]}"; do
    printf '%-16s %-8s %s\n' "${CHECKS[$i]}" "${RESULTS[$i]}" "${COMMANDS[$i]}"
    case "${RESULTS[$i]}" in
        FAIL) FAILS=$((FAILS + 1)) ;;
        NOT_RUN) NOTRUNS=$((NOTRUNS + 1)) ;;
    esac
done
for i in "${!CHECKS[@]}"; do
    if [ -n "${DETAILS[$i]}" ]; then
        printf '  %s %s: %s\n' "${RESULTS[$i]}" "${CHECKS[$i]}" "${DETAILS[$i]}"
    fi
done
echo
echo "HEAD $FULL_SHA  tree: $TREE_NOTE"
echo "Device and instrumented tests are not part of this script; they are NOT_RUN here."

if [ "$WRITE_HANDOFF" = 1 ]; then
    if [ -n "$NO_PYTHON_REASON" ]; then
        echo "handoff NOT written: $NO_PYTHON_REASON" >&2
    else
        if [ "$DIRTY_COUNT" != "0" ]; then
            # Keep the caveat attached to every recorded result, not just the console.
            sed -i "s/\$/ [$TREE_NOTE]/" "$RESULTS_TSV"
        fi
        python3 scripts/handoff.py record --tsv "$RESULTS_TSV" --commit "$FULL_SHA" || FAILS=$((FAILS + 1))
    fi
fi

if [ "$FAILS" -gt 0 ]; then
    echo "RESULT: FAIL ($FAILS failing step(s))"
    exit 1
fi
if [ "$NOTRUNS" -gt 0 ]; then
    echo "RESULT: NOT_RUN ($NOTRUNS step(s) could not run; this is not a pass)"
    exit 2
fi
echo "RESULT: PASS"
