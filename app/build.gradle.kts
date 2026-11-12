import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Release signing config: loaded from keystore.properties when present (written by CI
// from repository secrets). Absent for local dev builds, which fall back to the debug
// key. keystore.properties is gitignored and never committed.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { load(it) }
    }
}

android {
    namespace = "com.translator.screen"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.translator.screen"
        minSdk = 31
        targetSdk = 35
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // Dedicated release key, populated only when keystore.properties declares a storeFile
        // (Council P1-12: gate on the property, not just the file's existence, to avoid an NPE
        // if the file is present but empty).
        create("release") {
            val storeFilePath = keystoreProperties.getProperty("storeFile")
            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // Explicit v1+v2+v3 so the disposition is unambiguous (Council P0-3).
                // v1 is enabled DELIBERATELY so `keytool -printcert -jarfile` ALSO reports the
                // signer — this prevents the v1-only false-negative "Not a signed jar file" that
                // bit users of v2/v3-only APKs (build-learnings headline #16 / Threshold
                // POST-MORTEM-shipped-not-installable). v2+v3 are what Android 7+ enforces;
                // all three together = maximum installability + inspectability.
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
        // Default debug key (generated on demand by AGP for local dev builds).
        getByName("debug")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            // Use the dedicated release key when a storeFile is declared (CI);
            // fall back to debug ONLY for local dev. CI's `apksigner verify` step is the
            // fail-closed gate that rejects any APK not signed with the release key.
            signingConfig = if (keystoreProperties.getProperty("storeFile") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.systemProperty(
                    "robolectric.repository.url",
                    "https://maven.aliyun.com/repository/public",
                )
                test.systemProperty(
                    "robolectric.dependency.cache.dir",
                    "${System.getProperty("user.home")}/.m2/repository",
                )
                test.maxHeapSize = "1024m"
                test.forkEvery = 0L
                test.minHeapSize = "256m"
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.mlkit.translate)
    implementation(libs.mlkit.language.id)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.runner)
    testImplementation(libs.androidx.test.rules)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
}
