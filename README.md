# Update Auditor

A read-only Android device-state and package-provenance auditor for a mobile-first workflow.

## v0.1 scope

This first milestone intentionally reports **local observations only**. It does not claim that the device, firmware, or any installed app is current unless an upstream source has actually been checked.

The app currently reads:

- manufacturer, model, device and product identifiers
- Android version and SDK level
- Android security patch date
- build ID and fingerprint
- baseband/radio version where Android exposes it
- installed package label and package name
- version name and version code
- first-install and last-update timestamps
- enabled, debuggable and system-app flags
- installer-of-record where Android exposes it
- local provenance classification: FIRST PARTY, SYSTEM, or USER

Known first-party packages include Clock Tools, Drafts, Field Watch, Ledger, and Update Auditor, including their debug variants.

## Security posture

- No `INTERNET` permission.
- No telemetry, analytics, account, VPN, accessibility service, device-admin capability, updater, installer, or package mutation path.
- Backups are disabled.
- Cleartext traffic is disabled.
- `QUERY_ALL_PACKAGES` is declared because installed-package inventory is the app's core local-audit function.
- Package inventory is displayed locally and is not exported by v0.1.

`UNKNOWN` and `UNVERIFIED` are not synonyms for `OUTDATED`. Upstream version comparison, Samsung OTA status, Play/Galaxy Store status, APEX/Mainline inspection, signer continuity, snapshots/deltas, and Shizuku-enhanced inspection are later milestones.

## Build

The normal workflow is phone-only:

1. Push a branch or open a PR.
2. GitHub Actions installs JDK 17 and runs `scripts/verify.sh` (Gradle 8.9 via the
   committed, checksum-pinned wrapper): JVM tests including screenshot goldens, lint,
   `assembleDebug`, the third-party inventory check and `git diff --check`.
3. Download the `update-auditor-debug` workflow artifact and sideload it on the device.

On a laptop or in an agent session the same command works: `scripts/verify.sh`
(needs a JDK 17+ and the Android SDK; `scripts/bootstrap-android-sdk.sh` installs
the SDK pieces and verifies Google's checksum). It prints PASS, FAIL or NOT_RUN per
step with the commit sha; a step that could not run is never shown as a pass.

Regression screens (Robolectric + Roborazzi, JVM only, no emulator) are compared to
the PNGs in `app/src/test/screenshots/` at 320 dp and 411 dp, font scale 1.0 and
2.0, light and dark, with Accessibility Test Framework checks on each. After an
intended UI change run `./gradlew recordRoborazziDebug` and review the PNG diff.
Lint fails on new problems; existing ones are tracked in `app/lint-baseline.xml`.

### Out of debug — signed release

The debug APK is the fresh-clone floor. The operational artifact is a **signed
release APK**, and a debug build is never published as the operational release.
The release path is gated on repository secrets so a fork or a secretless clone
stays green:

| Secret | Purpose |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | base64 of the release keystore; its presence enables the signed release job |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | signing key alias |
| `ANDROID_KEY_PASSWORD` | signing key password |

With those set, CI also runs `scripts/release.sh`, which builds `assembleRelease`,
refuses a debuggable or unsigned APK, reads the signer certificate from the APK
itself with `apksigner`, compares its SHA-256 with the owner-committed pin in
`release/signing-cert.sha256` (or prints the digest to pin), and writes
`release-manifest.json` and a changelog. The `update-auditor-release` artifact
carries those. Locally, the same signing is picked up from a gitignored
`keystore.properties` (`storeFile` / `storePassword` / `keyAlias` / `keyPassword`).
Without any of that, `scripts/release.sh` stops with `unsigned: not a release`.

## Agent execution

Automated agents: read `AGENTS.md` before editing. It carries the security
posture invariants (no `INTERNET` permission, no telemetry, no package
mutation) and the protected-data boundary. `Sovereign-Ops` is the umbrella
source of truth.

## v0.1 device gate

**Passed 2026-08-07 on the target Samsung SM-A166U.**

Validated on-device:

- APK installs and launches;
- model, Android version, patch date, build, baseband and fingerprint are coherent;
- installed-package inventory is plausible (498 observed during the gate);
- Clock Tools, Drafts, Field Watch, Ledger and Update Auditor classify FIRST PARTY;
- known system apps classify SYSTEM;
- ordinary installed apps classify USER;
- package version/install/update metadata populates where Android exposes it;
- repeated refresh does not crash or visibly hang;
- Rethink shows no network activity attributable to Update Auditor.
