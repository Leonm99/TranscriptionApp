package com.example.transcriptionapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.transcriptionapp.ui.components.ScrollableWithFixedPartsModalSheet
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.util.FileUtils
import com.example.transcriptionapp.util.matchUrlFromSharedText
import com.example.transcriptionapp.viewmodel.BottomSheetViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.system.exitProcess

private const val TAG = "ShareActivity"

@AndroidEntryPoint
class ShareActivity : ComponentActivity() {

  private var sharedUrlCached: String = ""
  private val bottomSheetViewModel: BottomSheetViewModel by viewModels()

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    configureWindow()

    setContent {
      TranscriptionAppTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = ShareScreen) {
          composable<ShareScreen> {
            //            val isBottomSheetVisible =
            //
            // bottomSheetViewModel.isBottomSheetVisible.collectAsStateWithLifecycle().value
            //            AnimatedVisibility(isBottomSheetVisible, exit = fadeOut(), enter =
            // fadeIn()) {
            //              Box(
            //                modifier =
            //
            // Modifier.fillMaxSize().alpha(0.5f).animateEnterExit().background(Color.Black)
            //              )
            //            }
            ScrollableWithFixedPartsModalSheet(bottomSheetViewModel)
          }
        }
      }
    }

    if (savedInstanceState == null) {
      CoroutineScope(Dispatchers.IO).launch {
        delay(1000)
        handleIntent(intent)
      }
    }

    lifecycleScope.launch {
      bottomSheetViewModel.closeApp.collect { shouldClose ->
        if (shouldClose) {
          Log.d(TAG, "closeApp: WE GET HERE FAM, WELL AT LEAST I HOPE SO")
          finishAffinity() // Close the app
          exitProcess(0)
          Log.d(TAG, "JUST A LIL TEST")
        }
      }
    }
  }

  private fun configureWindow() {
    window.apply {
      setBackgroundDrawable(0.toDrawable())
      setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
      // On Android 12 and higher, TYPE_APPLICATION_OVERLAY is deprecated, and we use
      // TYPE_APPLICATION_ATTACHED_DIALOG instead
      setType(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG
        } else {
          WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        }
      )
    }
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND) {
      when {
        intent.type?.startsWith("audio/") == true || intent.type?.startsWith("video/") == true -> {
          handleAudioOrVideoIntent(intent)
          bottomSheetViewModel.transcribeAudios()
          bottomSheetViewModel.toggleBottomSheet(true, true)
        }

        intent.type?.startsWith("text/") == true -> {
          handleTextIntent(intent)
        }
      }
    }
    if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
      if (intent.type?.startsWith("audio/") == true || intent.type?.startsWith("video/") == true) {
        handleAudioOrVideoIntent(intent)
        bottomSheetViewModel.transcribeAudios()
        bottomSheetViewModel.toggleBottomSheet(true, true)
      }
    }
  }

  private fun handleAudioOrVideoIntent(intent: Intent) {
    val clipData = intent.clipData

    // Check if there are multiple URIs
    if (clipData != null) {
      for (i in 0 until clipData.itemCount) {
        val item = clipData.getItemAt(i)
        val uri = item.uri
        val audioFile = FileUtils.saveFileToCache(this, uri)
        audioFile?.let { file ->
          Log.d(TAG, "Audio/Video file path: ${file.absolutePath}")
          // ServiceUtil.startFloatingService(this,"TRANSCRIBE", file.absolutePath)

          bottomSheetViewModel.onAudioSelected(Uri.fromFile(file), this)
        }
      }
    } else {
      val uri = getUriFromIntent(intent)
      val audioFile = FileUtils.saveFileToCache(this, uri)
      audioFile?.let { file ->
        Log.d(TAG, "Audio/Video file path: ${file.absolutePath}")
        // ServiceUtil.startFloatingService(this,"TRANSCRIBE", file.absolutePath)

        bottomSheetViewModel.onAudioSelected(Uri.fromFile(file), this)
      }
    }
  }

  private fun handleTextIntent(intent: Intent) {
    val link = matchUrlFromSharedText(intent.extras?.getString(Intent.EXTRA_TEXT))
    link.let {
      Log.d(TAG, "Shared text link: $it")
      sharedUrlCached = it
    }
  }

  private fun getUriFromIntent(intent: Intent): Uri {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    } ?: throw IllegalArgumentException("No URI found in intent")
  }
}

@Serializable object ShareScreen
