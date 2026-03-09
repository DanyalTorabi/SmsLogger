plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.smslogger"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smslogger"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Certificate pinning disabled in debug – empty strings = no pinning
            buildConfigField("String", "CERT_HOSTNAME", "\"\"")
            buildConfigField("String", "CERT_PIN_PRIMARY", "\"\"")
            buildConfigField("String", "CERT_PIN_BACKUP", "\"\"")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // ── Certificate Pinning – SOURCE OF TRUTH ────────────────────────
            // Values MUST be derived from the server pin bundle before release:
            //   DanyalTorabi/sms-syncer-server → docs/security/android-pin-bundle.json
            //   Server handoff issue: DanyalTorabi/sms-syncer-server#130
            //   Rotation runbook:     docs/security/android-certificate-pinning-runbook.md
            //
            // Local dev / staging / production setup:
            //   See docs/security/CERTIFICATE_PINNING.md in this repo
            //
            // Mapping:
            //   CERT_HOSTNAME    ← hosts[].hostname
            //   CERT_PIN_PRIMARY ← hosts[].pins.current  (sha256/...)
            //   CERT_PIN_BACKUP  ← hosts[].pins.backup   (sha256/...)
            // ─────────────────────────────────────────────────────────────────
            buildConfigField("String", "CERT_HOSTNAME", "\"api.example.com\"")
            buildConfigField("String", "CERT_PIN_PRIMARY", "\"sha256/REPLACE_WITH_PROD_CURRENT_SPKI\"")
            buildConfigField("String", "CERT_PIN_BACKUP", "\"sha256/REPLACE_WITH_PROD_BACKUP_SPKI\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true // Required for BuildConfig.CERT_* fields (#56)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Lifecycle / ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Networking
    implementation(libs.okhttp)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Security - EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)

    // WorkManager - background token expiry checks (#48)
    implementation(libs.androidx.work.runtime.ktx)

    // Preferences - Settings screen (#54)
    implementation(libs.androidx.preference)

    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.lifecycle.runtime.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}