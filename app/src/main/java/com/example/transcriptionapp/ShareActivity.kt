package com.example.transcriptionapp

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import java.io.File
import java.io.FileOutputStream

private const val TAG = "ShareActivity"

class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
    }

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
                    val link = intent.extras?.getString(Intent.EXTRA_TEXT)
                    link?.let {
                        Log.d("ReceiveIntentActivity", "Text link: $it")
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

}

