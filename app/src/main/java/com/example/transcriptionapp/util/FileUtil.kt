package com.example.transcriptionapp.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object FileUtils {


fun saveFileToCache(context: Context, uri: Uri): File? {
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

fun getFileFromUri(uri: Uri, context: Context): File? {
  return try {
    // Open an input stream from the URI
    val inputStream = context.contentResolver.openInputStream(uri) ?: return null
    // Create a temporary file in the app's cache directory
    val tempFile = File.createTempFile("temp_audio", ".mp3", context.cacheDir)
    Log.d("TAG", "Created temp file: ${tempFile.absolutePath}")
    // Write the content of the input stream to the temporary file
    tempFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
    tempFile
  } catch (e: Exception) {
    Log.e("TAG", "Error copying file from URI: ${e.localizedMessage}")
    null
  }
}
//  fun convertAudio(inputUri: Uri, outputFormat: String, context: Context): Uri? {
//    return try {
//      val context = null
//      val outputPath = File(context.cacheDir, "output.$outputFormat").absolutePath
//      val result = FFmpeg.execute(arrayOf("-y", "-i", inputUri.path, "-c:a", outputFormat, outputPath))
//      if (result == 0) Uri.fromFile(File(outputPath)) else null
//    } catch (e: Exception) {
//      e.printStackTrace()
//      null
//    }
//  }
}
