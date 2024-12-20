package com.example.transcriptionapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.util.Log
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.util.matchUrlFromSharedText

import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ShareActivity"

class ShareActivity : ComponentActivity() {

    private var sharedUrlCached: String = ""


    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            when {
                intent.type?.startsWith("audio/") == true || intent.type?.startsWith("video/") == true -> {
                    val uri = getUriFromIntent(intent)
                    uri?.let {
                        val audioFile = saveToCache(it)
                        audioFile?.let { file ->
                            Log.d("ReceiveIntentActivity", "Audio file path: ${file.absolutePath}")
                            //ServiceUtil.startFloatingService(this,"TRANSCRIBE", file.absolutePath)
                        }
                    }
                }
                intent.type?.startsWith("text/") == true -> {
                    val link =  matchUrlFromSharedText(intent.extras?.getString(Intent.EXTRA_TEXT))

                    link.let {
                        Log.d("ReceiveIntentActivity", "Text link: $it")
                        sharedUrlCached = it
                        //ServiceUtil.startFloatingService(this,"DOWNLOAD", it)
                    }
                }
            }
        }
        finish()
    }

    private fun getUriFromIntent(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun saveToCache(uri: Uri): File? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val file = File(cacheDir, "shared_audio_file.${getFileExtension(uri)}")
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
                Log.d("ReceiveIntentActivity", "File saved to cache: ${file.absolutePath}")
                file
            }
        } catch (e: Exception) {
            Log.e("ReceiveIntentActivity", "Failed to save file to cache", e)
            null
        }
    }

    private fun getFileExtension(uri: Uri): String {
        val mimeType = contentResolver.getType(uri)
        return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        handleIntent(intent)

        if (sharedUrlCached.isEmpty()) {
            finish()
        }



        enableEdgeToEdge()

        window.run {
            setBackgroundDrawable(ColorDrawable(0))
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY)
            } else {
                setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT)
            }
        }




        setContent {

            TranscriptionAppTheme {
                BottomSheet()

            }

        }


    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun BottomSheet() {
        val scope = rememberCoroutineScope()
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)


        ModalBottomSheet(
            modifier = Modifier.fillMaxHeight(),
            sheetState = sheetState,
            onDismissRequest = {
                scope.launch {sheetState.hide()}.invokeOnCompletion {
                finish()
            } },
        ) {
            // Display the transcription if available
            Text(modifier = Modifier.padding(16.dp), text = "Transcription: $sharedUrlCached")


        }
    }
}


