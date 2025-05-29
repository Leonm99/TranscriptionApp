plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.kotlin.serialization)
  alias(libs.plugins.ksp)
  alias(libs.plugins.dagger.hilt.android)
  alias(libs.plugins.room)
  alias(libs.plugins.gservices)
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
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.graphics)
  implementation(libs.androidx.ui.tooling.preview)

  implementation(libs.androidx.material3)
  implementation(libs.materialIconsExtended)
  implementation(libs.material)
  implementation(libs.advanced.bottomsheet.material3)
  implementation(libs.modalsheet)
  implementation(libs.androidx.ui.text.google.fonts)

  implementation(libs.retrofit)
  implementation(libs.convert.gson)

  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.plugins)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.client.logging)
  implementation(libs.ktor.serialization.kotlinx.json)


  implementation(libs.androidx.lifecycle.viewmodel.compose)

  implementation(libs.composeSettings.ui)
  implementation(libs.composeSettings.ui.extended)

  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.navigation.compose)

  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)

  implementation(libs.ffmpeg.kit.full)

  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.functions)
  implementation(libs.firebase.ai)
  implementation(libs.firebase.appcheck.playintegrity)
  implementation(libs.firebase.appcheck.debug)
  implementation(libs.firebase.appcheck.ktx)

  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)
}
