import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.roborazzi)
}

val appVersionName = "0.1.0"
val appVersionCode = 1

// Single source for scripts and CI: `./gradlew -q printVersion` prints
// `versionName=...` and `versionCode=...`, so nothing parses this file with sed.
tasks.register("printVersion") {
    val versionNameValue = appVersionName
    val versionCodeValue = appVersionCode
    doLast {
        println("versionName=$versionNameValue")
        println("versionCode=$versionCodeValue")
    }
}

/**
 * Release signing is driven by CI secrets (see .github/workflows/build.yml) or a
 * local keystore.properties. If no keystore is present the release variant still
 * builds — unsigned — so a fresh clone or a fork without secrets does not go red.
 * "Out of debug" does not require signing to succeed; it requires that a signed
 * release path exists and is taken whenever the keystore material is present.
 */
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(FileInputStream(keystorePropertiesFile))
}
val releaseStoreFile: String? =
    (keystoreProperties.getProperty("storeFile") ?: System.getenv("KEYSTORE_FILE"))
        ?.takeIf { it.isNotBlank() && rootProject.file(it).exists() }

android {
    namespace = "com.sovereignops.updateauditor"
    compileSdk = 35
    // Pinned so a fresh `scripts/bootstrap-android-sdk.sh` install is sufficient
    // (AGP would otherwise try to download its default build-tools on first use).
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.sovereignops.updateauditor"
        minSdk = 26
        targetSdk = 35
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: System.getenv("KEYSTORE_PASSWORD")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: System.getenv("KEY_ALIAS")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (releaseStoreFile != null) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // New lint problems fail the build; existing ones are tracked in the committed
    // baseline so they are visible and can be burned down deliberately.
    lint {
        abortOnError = true
        baseline = file("lint-baseline.xml")
        // Version-nag checks depend on what upstream published today, so they would
        // make the baseline drift without anything changing here. Dependency updates
        // are deliberate and recorded in third_party.json.
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                // JVM tests, including the screenshot goldens recorded from the
                // debug variant, run against debug only; the release variant adds
                // minification, not behaviour these tests exercise.
                it.enabled = it.name.contains("Debug")
                it.maxHeapSize = "1g"
                it.systemProperties["robolectric.pixelCopyRenderMode"] = "hardware"
            }
        }
    }
}

// Golden screenshots are committed here. gradle.properties sets
// roborazzi.test.verify=true, so a normal `test` run compares against them;
// `./gradlew recordRoborazziDebug` re-records after an intentional UI change.
roborazzi {
    outputDir.set(file("src/test/screenshots"))
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.androidx.test.espresso.core)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.roborazzi.accessibility.check)
    testImplementation(libs.accessibility.test.framework)
}
