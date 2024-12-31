package com.example.transcriptionapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.example.transcriptionapp.api.SettingsHolder
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
      SettingsHolder.apiKey = DataStoreUtil(applicationContext).getString(stringPreferencesKey("userApiKey")).first()!!
      SettingsHolder.language = DataStoreUtil(applicationContext).getString(stringPreferencesKey("selectedLanguage")).first()!!
      SettingsHolder.model = DataStoreUtil(applicationContext).getString(stringPreferencesKey("selectedModel")).first()!!
      SettingsHolder.format = DataStoreUtil(applicationContext).getBoolean(booleanPreferencesKey("isFormattingEnabled")).first()
    }

  }

  @Preview(showBackground = true)
  @Composable
  fun AppPreview() {
    TranscriptionAppTheme { DestinationsNavHost(navGraph = NavGraphs.root) }
  }
}
