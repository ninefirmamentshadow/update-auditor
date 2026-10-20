---
name: android-release
description: Build and inspect a signed release APK with scripts/release.sh. Use when asked to cut, check or publish a release build of this app.
---

# Android release

The logic lives in `scripts/release.sh`; this skill only wraps it.

- Agents never create, change or print signing keys or passwords. Signing material is
  supplied by the owner (`keystore.properties`, or `KEYSTORE_FILE` / `KEYSTORE_PASSWORD` /
  `KEY_ALIAS` / `KEY_PASSWORD` in the environment).
- Run `scripts/release.sh`. With no keystore it stops with `unsigned: not a release`; that
  is the correct outcome, not something to work around.
- The script runs `scripts/verify.sh`, builds `assembleRelease`, then reads the APK
  itself: pinned application id, not debuggable, no `INTERNET` permission, signature
  verified with `apksigner`, signer SHA-256 compared with `release/signing-cert.sha256`.
  If no pin exists it prints the digest for the owner to pin; do not create the pin file.
- Output: `release/update-auditor-<version>.apk`, `release/release-manifest.json`,
  `release/changelog.md` (all git-ignored).
- A debug build is never published as a release. Installing the APK on a device is the
  owner's gate and is NOT_RUN here; say so.
