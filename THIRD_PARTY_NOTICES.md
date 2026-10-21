# Third-party notices

This file lists the third-party software this repository builds with, ships, or was informed by. The machine-readable source of truth is `third_party.json`; `scripts/check_third_party.py` (run by `scripts/verify.sh`) fails when the build declares a dependency that is missing from it.

License facts below were read from the upstream license file or the published POM at the pinned version, and the URL is recorded in `third_party.json`. Anything on the right of an "interpretation" label is not legal advice.

## Shipped in the APK (runtime)

Compiled into the release APK: the direct dependencies and every module they pull in (`./gradlew printRuntimeDependencies` lists them, and `scripts/verify.sh` fails if one is missing here). Apache-2.0 requires the license text to accompany redistribution; it is in [`licenses/Apache-2.0.txt`](licenses/Apache-2.0.txt). The jar listings of these artifacts contain no NOTICE file.

| Component | Version | License (SPDX) | Modules | Source |
| --- | --- | --- | --- | --- |
| AndroidX Core KTX | 1.13.1 | Apache-2.0 | core-ktx | https://developer.android.com/jetpack/androidx/releases/core |
| AndroidX AppCompat | 1.7.0 | Apache-2.0 | appcompat | https://developer.android.com/jetpack/androidx/releases/appcompat |
| AndroidX RecyclerView | 1.3.2 | Apache-2.0 | recyclerview | https://developer.android.com/jetpack/androidx/releases/recyclerview |
| AndroidX Activity KTX | 1.9.3 | Apache-2.0 | activity-ktx | https://developer.android.com/jetpack/androidx/releases/activity |
| AndroidX Lifecycle ViewModel KTX | 2.8.7 | Apache-2.0 | lifecycle-viewmodel-ktx | https://developer.android.com/jetpack/androidx/releases/lifecycle |
| AndroidX Lifecycle Runtime KTX | 2.8.7 | Apache-2.0 | lifecycle-runtime-ktx | https://developer.android.com/jetpack/androidx/releases/lifecycle |
| androidx.activity 1.9.3 (transitive) | 1.9.3 | Apache-2.0 | activity | https://developer.android.com/jetpack/androidx |
| androidx.annotation 1.4.0 (transitive) | 1.4.0 | Apache-2.0 | annotation-experimental | https://developer.android.com/jetpack/androidx |
| androidx.annotation 1.8.1 (transitive) | 1.8.1 | Apache-2.0 | annotation-jvm, annotation | https://developer.android.com/jetpack/androidx |
| androidx.appcompat 1.7.0 (transitive) | 1.7.0 | Apache-2.0 | appcompat-resources | https://developer.android.com/jetpack/androidx |
| androidx.arch.core 2.2.0 (transitive) | 2.2.0 | Apache-2.0 | core-common, core-runtime | https://developer.android.com/jetpack/androidx |
| androidx.collection 1.1.0 (transitive) | 1.1.0 | Apache-2.0 | collection | https://developer.android.com/jetpack/androidx |
| androidx.concurrent 1.1.0 (transitive) | 1.1.0 | Apache-2.0 | concurrent-futures | https://developer.android.com/jetpack/androidx |
| androidx.core 1.13.1 (transitive) | 1.13.1 | Apache-2.0 | core | https://developer.android.com/jetpack/androidx |
| androidx.cursoradapter 1.0.0 (transitive) | 1.0.0 | Apache-2.0 | cursoradapter | https://developer.android.com/jetpack/androidx |
| androidx.customview 1.0.0 (transitive) | 1.0.0 | Apache-2.0 | customview-poolingcontainer, customview | https://developer.android.com/jetpack/androidx |
| androidx.drawerlayout 1.0.0 (transitive) | 1.0.0 | Apache-2.0 | drawerlayout | https://developer.android.com/jetpack/androidx |
| androidx.emoji2 1.3.0 (transitive) | 1.3.0 | Apache-2.0 | emoji2-views-helper, emoji2 | https://developer.android.com/jetpack/androidx |
| androidx.fragment 1.5.4 (transitive) | 1.5.4 | Apache-2.0 | fragment | https://developer.android.com/jetpack/androidx |
| androidx.interpolator 1.0.0 (transitive) | 1.0.0 | Apache-2.0 | interpolator | https://developer.android.com/jetpack/androidx |
| androidx.lifecycle 2.8.7 (transitive) | 2.8.7 | Apache-2.0 | lifecycle-common-jvm, lifecycle-common, lifecycle-livedata-core-ktx, lifecycle-livedata-core, lifecycle-livedata, lifecycle-process, lifecycle-runtime-android, lifecycle-runtime-ktx-android, lifecycle-runtime, lifecycle-viewmodel-android, lifecycle-viewmodel-savedstate, lifecycle-viewmodel | https://developer.android.com/jetpack/androidx |
| androidx.loader 1.0.0 (transitive) | 1.0.0 | Apache-2.0 | loader | https://developer.android.com/jetpack/androidx |
| androidx.profileinstaller 1.3.1 (transitive) | 1.3.1 | Apache-2.0 | profileinstaller | https://developer.android.com/jetpack/androidx |
| androidx.resourceinspection 1.0.1 (transitive) | 1.0.1 | Apache-2.0 | resourceinspection-annotation | https://developer.android.com/jetpack/androidx |
| androidx.savedstate 1.2.1 (transitive) | 1.2.1 | Apache-2.0 | savedstate-ktx, savedstate | https://developer.android.com/jetpack/androidx |
| androidx.startup 1.1.1 (transitive) | 1.1.1 | Apache-2.0 | startup-runtime | https://developer.android.com/jetpack/androidx |
| androidx.tracing 1.0.0 (transitive) | 1.0.0 | Apache-2.0 | tracing | https://developer.android.com/jetpack/androidx |
| androidx.vectordrawable 1.1.0 (transitive) | 1.1.0 | Apache-2.0 | vectordrawable-animated, vectordrawable | https://developer.android.com/jetpack/androidx |
| androidx.versionedparcelable 1.1.1 (transitive) | 1.1.1 | Apache-2.0 | versionedparcelable | https://developer.android.com/jetpack/androidx |
| androidx.viewpager 1.0.0 (transitive) | 1.0.0 | Apache-2.0 | viewpager | https://developer.android.com/jetpack/androidx |
| com.google.guava 1.0 (transitive) | 1.0 | Apache-2.0 | listenablefuture | https://github.com/google/guava |
| org.jetbrains 23.0.0 (transitive) | 23.0.0 | Apache-2.0 | annotations | https://github.com/JetBrains/java-annotations |
| org.jetbrains.kotlin 1.8.20 (transitive) | 1.8.20 | Apache-2.0 | kotlin-stdlib-jdk7, kotlin-stdlib-jdk8 | https://github.com/JetBrains/kotlin |
| org.jetbrains.kotlin 2.0.21 (transitive) | 2.0.21 | Apache-2.0 | kotlin-stdlib-common, kotlin-stdlib | https://github.com/JetBrains/kotlin |
| org.jetbrains.kotlinx 1.7.3 (transitive) | 1.7.3 | Apache-2.0 | kotlinx-coroutines-android, kotlinx-coroutines-bom, kotlinx-coroutines-core-jvm, kotlinx-coroutines-core | https://github.com/Kotlin/kotlinx.coroutines |

## Test harness (never shipped)

Used only to run JVM tests on a developer machine or in CI. They are not part of the APK and are not redistributed from this repository; they are recorded because the compliance directive asks for every dependency. Only the direct test dependencies are listed; the many modules Robolectric and Espresso resolve alongside them run on the developer machine only. Robolectric is MIT ([`licenses/MIT-Robolectric.txt`](licenses/MIT-Robolectric.txt)). JUnit 4 is EPL-1.0, which is acceptable here only because it is test scope.

| Component | Version | License (SPDX) | Modules | Source |
| --- | --- | --- | --- | --- |
| JUnit 4 | 4.13.2 | EPL-1.0 | junit | https://github.com/junit-team/junit4 |
| Robolectric | 4.14.1 | MIT | robolectric | https://github.com/robolectric/robolectric |
| AndroidX Test Core | 1.6.1 | Apache-2.0 | core | https://github.com/android/android-test |
| AndroidX Test Ext JUnit | 1.2.1 | Apache-2.0 | junit | https://github.com/android/android-test |
| Espresso Core | 3.6.1 | Apache-2.0 | espresso-core | https://github.com/android/android-test |
| Roborazzi | 1.46.1 | Apache-2.0 | roborazzi, roborazzi-junit-rule, roborazzi-accessibility-check | https://github.com/takahirom/roborazzi |
| Accessibility Test Framework for Android | 4.1.1 | Apache-2.0 | accessibility-test-framework | https://github.com/google/Accessibility-Test-Framework-for-Android |

## Build tooling (never shipped)

Used to build the app. The Gradle wrapper jar is committed (`gradle/wrapper/gradle-wrapper.jar`, sha256 recorded in `third_party.json` and checked by `scripts/check_third_party.py`; it matches the checksum Gradle publishes for 8.9).

| Component | Version | License (SPDX) | Modules | Source |
| --- | --- | --- | --- | --- |
| Android Gradle Plugin | 8.7.2 | Apache-2.0 | com.android.application | https://developer.android.com/build/releases/gradle-plugin |
| Kotlin Gradle plugin | 2.0.21 | Apache-2.0 | org.jetbrains.kotlin.android | https://github.com/JetBrains/kotlin |
| Roborazzi Gradle plugin | 1.46.1 | Apache-2.0 | io.github.takahirom.roborazzi | https://github.com/takahirom/roborazzi |
| Gradle wrapper | 8.9 | Apache-2.0 |  | https://github.com/gradle/gradle |

## Informed by (no code or assets copied)

These projects shaped ideas. Their code was not copied; the implementation here is independent.

- **AppVerifier** (ISC), https://github.com/soupslurpr/AppVerifier. Informed reading signer certificates from PackageInfo.signingInfo and showing their SHA-256 digests, with multi-signer packages treated separately from a rotation lineage. Read at commit d39e32e; no code, assets or data were copied, and the implementation here (audit/SignerReader.kt, audit/CertDigest.kt) is independent. License read from https://github.com/soupslurpr/AppVerifier/blob/d39e32effd1ac2ef9d93d9669cd8b5e2a21cf6da/LICENSE.txt.
- **LibChecker** (Apache-2.0), https://github.com/LibChecker/LibChecker. Informed the idea of a per-package detail screen with a searchable, filterable package list. Only the product idea was used; no code, assets or data were copied, and the layouts and code here are independent. License read from https://github.com/LibChecker/LibChecker/blob/0aff6d8483c308abdbaae8d9fc1e6ddf3129af25/LICENSE.

## Not redistributed

Robolectric downloads Android framework jars (`android-all`, AOSP, Apache-2.0) at test time; they are cached outside the repository and are not committed or shipped.
