plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.dagger.hilt)
  alias(libs.plugins.room)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example.transcriptionapp"
  compileSdk = 35

  defaultConfig {
    applicationId = "com.example.transcriptionapp"
    minSdk = 29
    targetSdk = 35
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    buildConfig = true
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true

      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {

    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions { jvmTarget = "11" }
  buildFeatures { compose = true }
}

room { schemaDirectory("$projectDir/schemas") }

dependencies {
  // AndroidX Core & Lifecycle
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.text.google.fonts)
  implementation(libs.androidx.compose.ui.tooling.preview)

  // DataStore
  implementation(libs.androidx.datastore.preferences)

  // Navigation
  implementation(libs.androidx.navigation.compose)

  // Room
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  // Hilt (Dependency Injection)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  // Firebase
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.firebase.appcheck.ktx)
  implementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.firebase.auth.ktx)
  implementation(libs.firebase.functions)

  // Ktor (Networking)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.logging)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.plugins)
  implementation(libs.ktor.serialization.kotlinx.json)

  // Kotlin Serialization
  implementation(libs.kotlinx.serialization.json)

  // Other Libraries
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services.auth) // For Google Login
  implementation(libs.coil.compose) // For Image Loading
  implementation(libs.compose.settings.ui) // For Settings
  implementation(libs.compose.settings.ui.extended)
  implementation(libs.ffmpeg.kit.full) // For FFMPEG
  implementation(libs.google.id) // For Google ID
  implementation(libs.modalsheet) // For Modal Sheet

  // Testing
  testImplementation(libs.test.junit)
  androidTestImplementation(libs.test.junit.androidx)
  androidTestImplementation(libs.test.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.test.compose.ui.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.test.compose.ui.manifest)
}