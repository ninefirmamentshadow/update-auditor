# Third-party notices

This file lists the third-party software this repository builds with, ships, or was informed by. The machine-readable source of truth is `third_party.json`; `scripts/check_third_party.py` (run by `scripts/verify.sh`) fails when the build declares a dependency that is missing from it.

License facts below were read from the upstream license file or the published POM at the pinned version, and the URL is recorded in `third_party.json`. Anything on the right of an "interpretation" label is not legal advice.

## Shipped in the APK (runtime)

Compiled into the release APK. Apache-2.0 requires the license text to accompany redistribution; it is in [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt). None of these artifacts carries a NOTICE file.

| Component | Version | License (SPDX) | Source |
| --- | --- | --- | --- |
| AndroidX Core KTX | 1.13.1 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/core |
| AndroidX AppCompat | 1.7.0 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/appcompat |
| AndroidX RecyclerView | 1.3.2 | Apache-2.0 | https://developer.android.com/jetpack/androidx/releases/recyclerview |

## Test harness (never shipped)

Used only to run JVM tests on a developer machine or in CI. They are not part of the APK and are not redistributed from this repository; they are recorded because the compliance directive asks for every dependency. Robolectric is MIT ([`licenses/MIT-Robolectric.txt`](licenses/MIT-Robolectric.txt)). JUnit 4 is EPL-1.0, which is acceptable here only because it is test scope.

| Component | Version | License (SPDX) | Source |
| --- | --- | --- | --- |
| JUnit 4 | 4.13.2 | EPL-1.0 | https://github.com/junit-team/junit4 |
| Robolectric | 4.14.1 | MIT | https://github.com/robolectric/robolectric |
| AndroidX Test Core | 1.6.1 | Apache-2.0 | https://github.com/android/android-test |
| AndroidX Test Ext JUnit | 1.2.1 | Apache-2.0 | https://github.com/android/android-test |
| Espresso Core | 3.6.1 | Apache-2.0 | https://github.com/android/android-test |
| Roborazzi | 1.46.1 | Apache-2.0 | https://github.com/takahirom/roborazzi |
| Accessibility Test Framework for Android | 4.1.1 | Apache-2.0 | https://github.com/google/Accessibility-Test-Framework-for-Android |

## Build tooling (never shipped)

Used to build the app. The Gradle wrapper jar is committed (`gradle/wrapper/gradle-wrapper.jar`, sha256 recorded in `third_party.json` and checked by `scripts/check_third_party.py`; it matches the checksum Gradle publishes for 8.9).

| Component | Version | License (SPDX) | Source |
| --- | --- | --- | --- |
| Android Gradle Plugin | 8.7.2 | Apache-2.0 | https://developer.android.com/build/releases/gradle-plugin |
| Kotlin Gradle plugin | 2.0.21 | Apache-2.0 | https://github.com/JetBrains/kotlin |
| Roborazzi Gradle plugin | 1.46.1 | Apache-2.0 | https://github.com/takahirom/roborazzi |
| Gradle wrapper | 8.9 | Apache-2.0 | https://github.com/gradle/gradle |

## Not redistributed

Robolectric downloads Android framework jars (`android-all`, AOSP, Apache-2.0) at test time; they are cached outside the repository and are not committed or shipped.
