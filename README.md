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
2. GitHub Actions installs JDK 17 and Gradle 8.9.
3. `gradle test` must pass.
4. `gradle assembleDebug` builds the APK.
5. Download the `update-auditor-debug` workflow artifact and sideload it on the device.

No local Android Studio or local build step is required for v0.1.

## v0.1 device gate

Before merging, confirm on the target Samsung device that:

- the APK installs and launches;
- model, Android version, patch date, build, baseband and fingerprint look correct;
- installed-package count is plausible;
- Clock Tools, Drafts, Field Watch, Ledger and Update Auditor are classified FIRST PARTY;
- known system apps are classified SYSTEM;
- ordinary installed apps are classified USER;
- package version/install/update metadata is populated where expected;
- refreshing does not crash or visibly hang;
- Rethink or another network observer shows no network activity attributable to Update Auditor.
