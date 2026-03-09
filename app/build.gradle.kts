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
            // Replace these values with real sha256/ pins extracted via:
            // openssl s_client -connect yourdomain.com:443 | openssl x509 -pubkey -noout \
            //   | openssl pkey -pubin -outform der | openssl dgst -sha256 -binary \
            //   | openssl enc -base64
            buildConfigField("String", "CERT_HOSTNAME", "\"yourdomain.com\"")
            buildConfigField("String", "CERT_PIN_PRIMARY", "\"sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=\"")
            buildConfigField("String", "CERT_PIN_BACKUP", "\"sha256/BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB=\"")
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