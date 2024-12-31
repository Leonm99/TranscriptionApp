package com.example.transcriptionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.example.transcriptionapp.api.ApiKeyHolder
import com.example.transcriptionapp.ui.theme.TranscriptionAppTheme
import com.example.transcriptionapp.util.DataStoreUtil
import com.ramcosta.composedestinations.DestinationsNavHost
import com.ramcosta.composedestinations.generated.NavGraphs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { TranscriptionAppTheme { DestinationsNavHost(navGraph = NavGraphs.root) } }
    lifecycleScope.launch(Dispatchers.IO){
      ApiKeyHolder.apiKey = DataStoreUtil(applicationContext).getString(stringPreferencesKey("userApiKey")).first()
    }

  }

  @Preview(showBackground = true)
  @Composable
  fun AppPreview() {
    TranscriptionAppTheme { DestinationsNavHost(navGraph = NavGraphs.root) }
  }
}
