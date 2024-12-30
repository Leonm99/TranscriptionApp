package com.example.transcriptionapp

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.transcriptionapp.ui.screens.ShareScreen
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.util.matchUrlFromSharedText
import com.example.transcriptionapp.util.FileUtils.saveFileToCache as saveToCache
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel

private const val TAG = "ShareActivity"

class ShareActivity : ComponentActivity() {
  private val viewModel by viewModels<TranscriptionViewModel>()
  private var sharedUrlCached: String = ""

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()

    window.run {
      setBackgroundDrawable(ColorDrawable(0))
      setLayout(
          WindowManager.LayoutParams.MATCH_PARENT,
          WindowManager.LayoutParams.MATCH_PARENT,
      )
      setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
    }

    setContent { TranscriptionAppTheme { ShareScreen(viewModel, this) } }

    handleIntent(intent)
  }

  private fun handleIntent(intent: Intent?) {
    if (intent?.action == Intent.ACTION_SEND) {
      when {
        intent.type?.startsWith("audio/") == true || intent.type?.startsWith("video/") == true -> {
          val uri = getUriFromIntent(intent)

            val audioFile = saveToCache(this, uri)
            audioFile?.let { file ->
              Log.d("ReceiveIntentActivity", "Audio file path: ${file.absolutePath}")
              // ServiceUtil.startFloatingService(this,"TRANSCRIBE", file.absolutePath)
              viewModel.onAudioSelected(Uri.fromFile(file), this)

            }
        }
        intent.type?.startsWith("text/") == true -> {
          val link = matchUrlFromSharedText(intent.extras?.getString(Intent.EXTRA_TEXT))

          link.let {
            Log.d("ReceiveIntentActivity", "Text link: $it")
            sharedUrlCached = it
            // ServiceUtil.startFloatingService(this,"DOWNLOAD", it)
          }
        }
      }
    }
    // Log.d("ReceiveIntentActivity", "FINISH")
    // finish()
  }

  private fun getUriFromIntent(intent: Intent): Uri {
    return (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
      @Suppress("DEPRECATION")
      intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
    })!!
  }
}
