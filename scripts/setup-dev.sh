#!/usr/bin/env bash
set -euo pipefail

# Update Auditor development bootstrap.
# Intended for ephemeral Linux coding-agent environments (including Jules)
# and ordinary developer shells. It does not require Android Studio.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TOOLS_DIR="${ROOT_DIR}/.tools"
GRADLE_VERSION="8.9"
ANDROID_PLATFORM="35"
ANDROID_BUILD_TOOLS="35.0.0"
GRADLE_HOME_LOCAL="${TOOLS_DIR}/gradle-${GRADLE_VERSION}"

log() { printf '[setup] %s\n' "$*"; }
die() { printf '[setup] ERROR: %s\n' "$*" >&2; exit 1; }

command -v curl >/dev/null 2>&1 || die "curl is required"
command -v unzip >/dev/null 2>&1 || die "unzip is required"

mkdir -p "${TOOLS_DIR}"

# JDK 17 is the project contract. Do not silently build with a different major.
command -v java >/dev/null 2>&1 || die "JDK 17 is required but java is not on PATH"
JAVA_MAJOR="$(java -version 2>&1 | awk -F'[\".]' '/version/ { print $2; exit }')"
[[ "${JAVA_MAJOR}" == "17" ]] || die "JDK 17 is required; found Java ${JAVA_MAJOR:-unknown}"
log "JDK 17 detected"

# Resolve Android SDK from the conventional environment variables/locations.
ANDROID_SDK_ROOT="${ANDROID_SDK_ROOT:-${ANDROID_HOME:-}}"
if [[ -z "${ANDROID_SDK_ROOT}" ]]; then
  for candidate in "${HOME}/Android/Sdk" "/opt/android-sdk" "/usr/local/lib/android/sdk"; do
    if [[ -d "${candidate}" ]]; then
      ANDROID_SDK_ROOT="${candidate}"
      break
    fi
  done
fi
[[ -n "${ANDROID_SDK_ROOT}" && -d "${ANDROID_SDK_ROOT}" ]] || \
  die "Android SDK not found. Set ANDROID_SDK_ROOT or ANDROID_HOME."
export ANDROID_SDK_ROOT
export ANDROID_HOME="${ANDROID_SDK_ROOT}"
log "Android SDK: ${ANDROID_SDK_ROOT}"

SDKMANAGER=""
for candidate in \
  "${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager" \
  "${ANDROID_SDK_ROOT}/cmdline-tools/bin/sdkmanager" \
  "${ANDROID_SDK_ROOT}/tools/bin/sdkmanager"; do
  if [[ -x "${candidate}" ]]; then
    SDKMANAGER="${candidate}"
    break
  fi
done

if [[ -n "${SDKMANAGER}" ]]; then
  log "Ensuring Android platform ${ANDROID_PLATFORM}, build-tools ${ANDROID_BUILD_TOOLS}, and platform-tools"
  yes | "${SDKMANAGER}" --licenses >/dev/null 2>&1 || true
  "${SDKMANAGER}" \
    "platforms;android-${ANDROID_PLATFORM}" \
    "build-tools;${ANDROID_BUILD_TOOLS}" \
    "platform-tools"
else
  [[ -d "${ANDROID_SDK_ROOT}/platforms/android-${ANDROID_PLATFORM}" ]] || \
    die "sdkmanager is unavailable and Android platform ${ANDROID_PLATFORM} is not installed"
  log "sdkmanager unavailable; existing Android platform ${ANDROID_PLATFORM} will be used"
fi

# Pin Gradle locally so agents do not depend on an arbitrary host Gradle.
if [[ ! -x "${GRADLE_HOME_LOCAL}/bin/gradle" ]]; then
  ZIP_PATH="${TOOLS_DIR}/gradle-${GRADLE_VERSION}-bin.zip"
  log "Downloading Gradle ${GRADLE_VERSION}"
  curl --fail --location --retry 3 \
    "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    --output "${ZIP_PATH}"
  rm -rf "${GRADLE_HOME_LOCAL}"
  unzip -q "${ZIP_PATH}" -d "${TOOLS_DIR}"
  rm -f "${ZIP_PATH}"
fi

export PATH="${GRADLE_HOME_LOCAL}/bin:${ANDROID_SDK_ROOT}/platform-tools:${PATH}"
log "Gradle: $(gradle --version | awk '/^Gradle / { print $2; exit }')"

cd "${ROOT_DIR}"

if [[ "${1:-}" == "--no-build" ]]; then
  log "Environment ready; build skipped by request"
  exit 0
fi

log "Running unit tests"
gradle test --stacktrace

log "Building debug APK"
gradle assembleDebug --stacktrace

APK_PATH="${ROOT_DIR}/app/build/outputs/apk/debug/app-debug.apk"
[[ -f "${APK_PATH}" ]] || die "Build completed but APK was not found at ${APK_PATH}"
log "Ready: ${APK_PATH}"
