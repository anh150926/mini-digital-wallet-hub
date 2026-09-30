plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.walletapp.android"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.walletapp.android"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)

    // Navigation Component
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Lifecycle & ViewModel & LiveData
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)

    // Retrofit & OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Security & Biometric
    implementation(libs.security.crypto)
    implementation(libs.biometric)

    // CameraX & QR Scanning
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode)

    // QR Generation & UI
    implementation(libs.zxing.core)
    implementation(libs.swiperefreshlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}