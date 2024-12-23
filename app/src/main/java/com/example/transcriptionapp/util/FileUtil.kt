package com.example.transcriptionapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

fun saveToCache(context: Context, uri: Uri): File? {
  return try {
    context.contentResolver.openInputStream(uri)?.use { inputStream ->
      val file =
          File(context.cacheDir, "shared_audio_file.${getFileExtensionFromUri(context, uri)}")
      FileOutputStream(file).use { outputStream -> inputStream.copyTo(outputStream) }
      Log.d("ReceiveIntentActivity", "File saved to cache: ${file.absolutePath}")
      file
    }
  } catch (e: Exception) {
    Log.e("ReceiveIntentActivity", "Failed to save file to cache", e)
    null
  }
}

private fun getFileExtensionFromUri(context: Context, uri: Uri): String {
  val mimeType = context.contentResolver.getType(uri)
  return android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
}
