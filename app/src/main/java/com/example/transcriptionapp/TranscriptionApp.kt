package com.example.transcriptionapp

import android.app.Application
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.appcheck.appCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import com.google.firebase.initialize
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TranscriptionApp : Application() {
  override fun onCreate() {
    super.onCreate()

    Firebase.initialize(context = this) // Initialize Firebase

    // Conditionally install App Check provider
    if (BuildConfig.DEBUG) {
      // In debug builds, install the DebugAppCheckProviderFactory
      Log.d("BuildTypeCheck", "This is a DEBUG build.")

      Firebase.appCheck.installAppCheckProviderFactory(
          DebugAppCheckProviderFactory.getInstance(),
      )
    } else {
      // In release builds, install the PlayIntegrityAppCheckProviderFactory
      Firebase.appCheck.installAppCheckProviderFactory(
          PlayIntegrityAppCheckProviderFactory.getInstance(),
      )
    }
  }
}
