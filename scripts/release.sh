#!/usr/bin/env bash
# Build and inspect the signed release APK. This script never creates, changes or
# prints signing keys; it builds with whatever keystore material the owner supplies
# and then checks the APK itself.
#
#   scripts/release.sh [--skip-verify]
#   scripts/release.sh --inspect-only APK     run only the APK checks (4 and 5 below) on
#                                             an existing file; writes nothing, builds
#                                             nothing, and is never a release
#
# Signing material (owner-supplied, never committed): keystore.properties
# (storeFile/storePassword/keyAlias/keyPassword) or the environment variables
# KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
#
# Order:
#   1. refuse a dirty tree and a missing keystore ("unsigned: not a release")
#   2. scripts/verify.sh must pass (--skip-verify only when CI just ran it at this commit)
#   3. ./gradlew assembleRelease
#   4. aapt2 dump badging: applicationId pinned, not debuggable, no forbidden permission
#   5. apksigner verify --print-certs on the APK itself; compare the signer's SHA-256
#      with release/signing-cert.sha256, or print the digest for the owner to pin
#   6. write release/release-manifest.json, release/changelog.md and a versioned APK copy
#
# A debug build is never a release; this script has no path that publishes one.
set -euo pipefail

cd "$(dirname "$0")/.."

# Pinned per repository.
APP_ID="com.sovereignops.updateauditor"
FORBIDDEN_PERMISSIONS=("android.permission.INTERNET")
BUILD_TOOLS_VERSION="${ANDROID_BUILD_TOOLS:-35.0.0}"
PIN_FILE="release/signing-cert.sha256"
APK="app/build/outputs/apk/release/app-release.apk"

SKIP_VERIFY=0
INSPECT_ONLY=""
while [ $# -gt 0 ]; do
    case "$1" in
        --skip-verify) SKIP_VERIFY=1 ;;
        --inspect-only) shift; INSPECT_ONLY="${1:-}"; [ -n "$INSPECT_ONLY" ] || { echo "--inspect-only needs an APK path" >&2; exit 64; } ;;
        -h|--help) sed -n '2,26p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "unknown argument: $1" >&2; exit 64 ;;
    esac
    shift
done

die() { echo "release: $*" >&2; exit 1; }

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
TOOLS="$SDK/build-tools/$BUILD_TOOLS_VERSION"
AAPT2="$TOOLS/aapt2"
APKSIGNER="$TOOLS/apksigner"

[ -x "$AAPT2" ] && [ -x "$APKSIGNER" ] || die "build-tools $BUILD_TOOLS_VERSION not found under $SDK; run scripts/bootstrap-android-sdk.sh and export ANDROID_HOME"
command -v python3 >/dev/null || die "python3 is required to write the manifest"
export ANDROID_HOME="$SDK"

if [ -z "$INSPECT_ONLY" ]; then
    # 1. Preconditions that must hold before spending a build on this.
    have_keystore=0
    if [ -f keystore.properties ]; then
        store_file="$(sed -n 's/^storeFile *= *//p' keystore.properties | head -1)"
        if [ -n "$store_file" ] && [ -f "$store_file" ]; then have_keystore=1; fi
    fi
    if [ -n "${KEYSTORE_FILE:-}" ] && [ -f "${KEYSTORE_FILE}" ]; then
        have_keystore=1
    fi
    if [ "$have_keystore" != 1 ]; then
        echo "unsigned: not a release" >&2
        echo "  no keystore material found. Provide keystore.properties or set KEYSTORE_FILE," >&2
        echo "  KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD. Agents do not create keys." >&2
        exit 3
    fi
    if [ -n "$(git status --porcelain)" ]; then
        die "working tree is not clean; a release must be built from a committed state"
    fi
fi

GRADLE_ARGS=(--no-daemon)
if [ -n "${VERIFY_GRADLE_ARGS:-}" ]; then
    # shellcheck disable=SC2206
    GRADLE_ARGS+=(${VERIFY_GRADLE_ARGS})
fi

COMMIT="$(git rev-parse HEAD)"

if [ -n "$INSPECT_ONLY" ]; then
    APK="$INSPECT_ONLY"
    [ -f "$APK" ] || die "no such file: $APK"
else
    # 2. The same verification everyone else runs.
    if [ "$SKIP_VERIFY" = 1 ]; then
        echo "release: --skip-verify given; assuming scripts/verify.sh already passed at $COMMIT"
    else
        scripts/verify.sh || die "scripts/verify.sh did not pass; not building a release"
    fi

    # 3. Build.
    rm -f "$APK"
    ./gradlew "${GRADLE_ARGS[@]}" assembleRelease
    [ -f "$APK" ] || die "expected $APK was not produced (an unsigned build lands in app-release-unsigned.apk: unsigned: not a release)"
fi

# 4. Inspect what was actually built.
BADGING="$("$AAPT2" dump badging "$APK")"
pkg_line="$(printf '%s\n' "$BADGING" | sed -n "s/^package: //p" | head -1)"
badge_id="$(printf '%s' "$pkg_line" | sed -n "s/^name='\([^']*\)'.*/\1/p")"
badge_code="$(printf '%s' "$pkg_line" | sed -n "s/.* versionCode='\([^']*\)'.*/\1/p")"
badge_name="$(printf '%s' "$pkg_line" | sed -n "s/.* versionName='\([^']*\)'.*/\1/p")"
[ "$badge_id" = "$APP_ID" ] || die "applicationId is '$badge_id', expected pinned '$APP_ID'"
if printf '%s\n' "$BADGING" | grep -q '^application-debuggable'; then
    die "APK is debuggable: a debug build is not a release"
fi
for permission in "${FORBIDDEN_PERMISSIONS[@]}"; do
    if printf '%s\n' "$BADGING" | grep -q "^uses-permission: name='$permission'"; then
        die "APK requests $permission, which this app's security posture forbids"
    fi
done
if [ -z "$INSPECT_ONLY" ]; then
    printed="$(./gradlew -q "${GRADLE_ARGS[@]}" printVersion 2>/dev/null)"
    gradle_name="$(printf '%s\n' "$printed" | sed -n 's/^versionName=//p')"
    gradle_code="$(printf '%s\n' "$printed" | sed -n 's/^versionCode=//p')"
    [ "$gradle_name" = "$badge_name" ] && [ "$gradle_code" = "$badge_code" ] \
        || die "version in the APK ($badge_name/$badge_code) differs from Gradle ($gradle_name/$gradle_code)"
fi

# 5. Signature, read from the APK itself.
SIGN_OUT="$("$APKSIGNER" verify --verbose --print-certs "$APK")" || die "apksigner verify failed: unsigned: not a release"
schemes="$(printf '%s\n' "$SIGN_OUT" | sed -n 's/^Verified using \(v[0-9.]*\) scheme.*: true$/\1/p' | tr '\n' ' ')"
[ -n "$schemes" ] || die "APK is not verified by any v1-v4 signature scheme"
signer_count="$(printf '%s\n' "$SIGN_OUT" | sed -n 's/^Number of signers: //p')"
[ "$signer_count" = 1 ] || die "expected exactly 1 signer, apksigner reports '$signer_count'"
CERT_SHA256="$(printf '%s\n' "$SIGN_OUT" | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | tr 'A-F' 'a-f' | tr -d ':[:space:]')"
printf '%s' "$CERT_SHA256" | grep -Eq '^[0-9a-f]{64}$' || die "could not read the signer certificate digest from apksigner output"

pin_status="absent"
if [ -f "$PIN_FILE" ]; then
    pinned="$(grep -v '^#' "$PIN_FILE" | tr 'A-F' 'a-f' | tr -d ':[:space:]' | sed 's/^sha256//')"
    printf '%s' "$pinned" | grep -Eq '^[0-9a-f]{64}$' || die "$PIN_FILE does not contain a SHA-256 digest"
    [ "$pinned" = "$CERT_SHA256" ] || die "signing certificate $CERT_SHA256 does not match the pin in $PIN_FILE ($pinned)"
    pin_status="matched"
else
    echo "release: NOT PINNED. This certificate has no pin yet. To pin it, the owner runs:"
    echo "  echo $CERT_SHA256 > $PIN_FILE"
    echo "and commits that file after checking the digest is the owner's own key."
fi

if [ -n "$INSPECT_ONLY" ]; then
    echo "inspect-only: $APK passed the APK checks (id $badge_id, signer $CERT_SHA256, pin: $pin_status)."
    echo "This is not a release: nothing was built or written."
    exit 0
fi

# 6. Manifest, changelog, versioned copy.
mkdir -p release
APK_SHA256="$(sha256sum "$APK" | cut -d' ' -f1)"
OUT_APK="release/update-auditor-$badge_name.apk"
cp "$APK" "$OUT_APK"

export COMMIT APP_ID badge_name badge_code APK_SHA256 CERT_SHA256 pin_status schemes OUT_APK
export JDK_VERSION="$(java -version 2>&1 | sed -n '1s/.*"\(.*\)".*/\1/p')"
export GRADLE_VERSION="$(sed -n 's#.*gradle-\([0-9.]*\)-bin.zip.*#\1#p' gradle/wrapper/gradle-wrapper.properties)"
export BUILD_TOOLS_VERSION
export AGP_VERSION="$(sed -n 's/^agp = "\(.*\)"/\1/p' gradle/libs.versions.toml)"
python3 - <<'PY'
import datetime, json, os
e = os.environ
manifest = {
    "schema": 1,
    "application_id": e["APP_ID"],
    "version_name": e["badge_name"],
    "version_code": int(e["badge_code"]),
    "commit": e["COMMIT"],
    "built_at_utc": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    "apk": {"path": e["OUT_APK"], "sha256": e["APK_SHA256"]},
    "signing": {
        "certificate_sha256": e["CERT_SHA256"],
        "pin": e["pin_status"],
        "schemes": e["schemes"].split(),
    },
    "tools": {
        "gradle": e["GRADLE_VERSION"],
        "android_gradle_plugin": e["AGP_VERSION"],
        "jdk": e["JDK_VERSION"],
        "build_tools": e["BUILD_TOOLS_VERSION"],
    },
}
with open("release/release-manifest.json", "w", encoding="utf-8") as handle:
    json.dump(manifest, handle, indent=2)
    handle.write("\n")
PY

last_tag="$(git describe --tags --abbrev=0 2>/dev/null || true)"
{
    echo "# Changes in $badge_name"
    echo
    if [ -n "$last_tag" ]; then
        echo "Commits since $last_tag, up to $COMMIT."
        echo
        git log --no-merges --format='- %h %s' "$last_tag..HEAD"
    else
        echo "No earlier tag; every commit up to $COMMIT."
        echo
        git log --no-merges --format='- %h %s'
    fi
} > release/changelog.md

echo
echo "release: OK"
echo "  apk       $OUT_APK"
echo "  sha256    $APK_SHA256"
echo "  signer    $CERT_SHA256 (pin: $pin_status)"
echo "  manifest  release/release-manifest.json"
echo "  changelog release/changelog.md"
echo "Installing it on a device and confirming it launches is NOT_RUN here; that is the owner's gate."
