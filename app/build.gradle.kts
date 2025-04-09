plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compiler.ksp)
    alias(libs.plugins.dagger.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.dhbt"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.dhbt"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
}

dependencies {
    //workmanager'
    implementation (libs.androidx.work.runtime.ktx)
    implementation (libs.androidx.hilt.work)
    ksp( libs.androidx.hilt.compiler)

    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.preference.ktx)

    // Compose UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.foundation)
    implementation(libs.swipe)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Navigation
    implementation(libs.androidx.navigation.compose)
    //implementation(libs.androidx.navigation.compose.typed)

    // Lifecycle & ViewModel
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation (libs.androidx.lifecycle.viewmodel.compose)
    // Database - Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Dependency Injection - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Asynchronous - Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Data Storage
    implementation(libs.datastore.preferences)

    // Data Parsing & Serialization
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.opencsv)
    implementation(libs.poi)
    implementation(libs.poi.ooxml)


    // UI Components
    implementation(libs.compose)  // Calendar
    implementation(libs.mpandroidchart)  // Charts
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.pager.indicators)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.lottie.compose)
    implementation(libs.coil.compose)  // Image loading

    // Logging
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.30.1")
}