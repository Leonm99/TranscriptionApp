package com.example.transcriptionapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
import kotlinx.coroutines.withContext
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
            ScrollableWithFixedPartsModalSheet(bottomSheetViewModel)
            val duplicateFileWarningList by bottomSheetViewModel.showDuplicateFileWarning.collectAsStateWithLifecycle()

            if (duplicateFileWarningList.isNotEmpty()) {
              AlertDialog(
                onDismissRequest = { bottomSheetViewModel.dismissDuplicateWarning() },
                title = { Text("Already Transcribed?") },
                text = {
                  val fileNames = duplicateFileWarningList.joinToString(separator = "\n") { audioFileWithHash ->
                    audioFileWithHash.uri.lastPathSegment ?: audioFileWithHash.uri.toString()

                  }
                  Text("The following file(s) appear in your transcription history:\n\n$fileNames\n\nDo you want to transcribe them again?")
                },
                confirmButton = {
                  TextButton(onClick = {
                    bottomSheetViewModel.proceedWithTranscription(transcribeDuplicates = true)

                  }) { Text("Transcribe Anyway") }
                },
                dismissButton = {
                  TextButton(onClick = {
                    bottomSheetViewModel.proceedWithTranscription(transcribeDuplicates = false)
                  }) { Text("Skip Duplicates") }
                }
              )
            }
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
          //finishAffinity() // Close the app
          finish()
          exitProcess(0)
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
        }

        intent.type?.startsWith("text/") == true -> {
          handleTextIntent(intent)
        }
      }
    }
    if (intent?.action == Intent.ACTION_SEND_MULTIPLE) {
      if (intent.type?.startsWith("audio/") == true || intent.type?.startsWith("video/") == true) {
        handleAudioOrVideoIntent(intent)

      }
    }
  }

  private fun handleAudioOrVideoIntent(intent: Intent) {
    lifecycleScope.launch {
      val urisToProcess = mutableListOf<Uri>()
      intent.clipData?.let { clipData ->
        for (i in 0 until clipData.itemCount) {
          urisToProcess.add(clipData.getItemAt(i).uri)
        }
      } ?: intent.data?.let {
        urisToProcess.add(it)
      }

      if (urisToProcess.isEmpty()) {
        Log.w(TAG, "No URIs found to process in the intent.")
        Toast.makeText(this@ShareActivity, "No files selected.", Toast.LENGTH_SHORT).show()
        return@launch
      }


      val processedFileUris = mutableListOf<Uri>()

      for (contentUri in urisToProcess) {
        val fileUri = processAndCacheUri(contentUri)
        fileUri?.let {
          processedFileUris.add(it)
        }
      }

      if (processedFileUris.isNotEmpty()) {
        
        bottomSheetViewModel.onAudioSelected(processedFileUris)
        bottomSheetViewModel.endAfterSave = true



      } else {
        Log.w(TAG, "No files could be processed and cached.")
        Toast.makeText(this@ShareActivity, "Could not process selected files.", Toast.LENGTH_SHORT).show()
      }
    }
  }


  private suspend fun processAndCacheUri(contentUri: Uri): Uri? {
    return withContext(Dispatchers.IO) {
      try {
        val audioFile = FileUtils.saveFileToCache(this@ShareActivity, contentUri)
        audioFile?.let { file ->
          Log.d(TAG, "Audio/Video file cached: ${file.absolutePath}")
          // ServiceUtil.startFloatingService(this@ShareActivity,"TRANSCRIBE", file.absolutePath)
          Uri.fromFile(file)
        }
      } catch (e: Exception) {
        Log.e(TAG, "Error caching file from URI: $contentUri", e)
        null
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

}

@Serializable object ShareScreen
