package com.example.transcriptionapp

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TranscriptionAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val activity = LocalContext.current
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == ComponentActivity.RESULT_OK) {
            selectedAudioUri = result.data?.data
        }
    }

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        var transcription by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(selectedAudioUri) {
            selectedAudioUri?.let { uri ->
                transcription = withContext(Dispatchers.IO) {
                    transcribeAudioFile(uri, activity)
                }
            }
        }

        // Display the transcription if available
        transcription?.let {
            Text("Transcription: $it")
        }

        Button(
            onClick = {
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    type = "audio/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                    // Ensure only audio files are displayed
                    putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/*"))
                }
                launcher.launch(intent)
            },
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text("Pick Audio")
        }


    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    TranscriptionAppTheme {
        Greeting("Android")
    }
}


suspend fun transcribeAudioFile(audioUri: Uri, context: Context): String {
    val tempFile = getFileFromUri(audioUri, context) ?: throw IllegalArgumentException("Invalid URI")
    val requestBody = tempFile.asRequestBody("audio/*".toMediaTypeOrNull())
    val audioPart = MultipartBody.Part.createFormData("file", tempFile.name, requestBody)

    val response = OpenAIClient.service.transcribeAudio(audioPart)
    return response.text
}

private fun getFileFromUri(uri: Uri, context: Context): File? {
    return try {
        // Open an input stream from the URI
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        // Create a temporary file in the app's cache directory
        val tempFile = File.createTempFile("temp_audio", ".mp3", context.cacheDir)
        Log.d("TAG", "Created temp file: ${tempFile.absolutePath}")
        // Write the content of the input stream to the temporary file
        tempFile.outputStream().use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        tempFile
    } catch (e: Exception) {
        Log.e("TAG", "Error copying file from URI: ${e.localizedMessage}")
        null
    }
}
