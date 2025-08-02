package com.example.transcriptionapp.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.GeneratingTokens
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.alorma.compose.settings.ui.SettingsCheckbox
import com.alorma.compose.settings.ui.SettingsMenuLink
import com.alorma.compose.settings.ui.SettingsSwitch
import com.alorma.compose.settings.ui.base.internal.SettingsTileColors
import com.alorma.compose.settings.ui.base.internal.SettingsTileDefaults
import com.example.transcriptionapp.R
import com.example.transcriptionapp.ui.components.DeleteDialog
import com.example.transcriptionapp.ui.components.LanguageDialog
import com.example.transcriptionapp.ui.components.SummaryDialog
import com.example.transcriptionapp.ui.components.TranscriptionDialog
import com.example.transcriptionapp.util.showToast
import com.example.transcriptionapp.viewmodel.DialogType
import com.example.transcriptionapp.viewmodel.SettingsViewModel

@SuppressLint("ContextCastToActivity")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit, viewModel: SettingsViewModel, isSignedIn: Boolean) {
  val showDialog by viewModel.showDialog.collectAsStateWithLifecycle()
  val dialogType by viewModel.dialogType.collectAsStateWithLifecycle()
  val context = LocalContext.current
  val activity = LocalContext.current as? ComponentActivity


  var expertSettingsExpanded by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    viewModel.navigateToSystemAddAccount.collect {
      val intent = Intent(Settings.ACTION_ADD_ACCOUNT)
      intent.putExtra(Settings.EXTRA_ACCOUNT_TYPES, arrayOf("com.google"))
      try {
        context.startActivity(intent)
      } catch (e: Exception) {
        Log.e("SettingsScreen", "Could not launch Add Account screen", e)
        showToast(context, "Could not open account settings. Please add a Google account manually.", true)
      }
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Settings") },
        navigationIcon = {
          IconButton(onClick = onBackClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer,
          titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
      )
    },
    containerColor = MaterialTheme.colorScheme.background
  ) { paddingValues ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 8.dp),
      contentPadding = PaddingValues(vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      item {
        SettingsCardGroup(title = "Account") {
          val userDisplayNameState by viewModel.userDisplayName.collectAsStateWithLifecycle()
          val userProfilePictureUrlState by viewModel.userProfilePictureUrl.collectAsStateWithLifecycle()
          val userEmailState by viewModel.userEmail.collectAsStateWithLifecycle()
          val userFirebaseIdState by viewModel.userFirebaseId.collectAsStateWithLifecycle()

          if (isSignedIn) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                AsyncImage(
                  model = ImageRequest.Builder(LocalContext.current)
                    .data(userProfilePictureUrlState)
                    .crossfade(true)
                    .placeholder(R.drawable.ic_default_profile)
                    .error(R.drawable.ic_default_profile)
                    .build(),
                  contentDescription = "User Profile Picture",
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = userDisplayNameState ?: "User",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  userEmailState?.let { email ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = email,
                      style = MaterialTheme.typography.bodyMedium,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                  userFirebaseIdState?.let { firebaseId ->
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "Firebase ID: $firebaseId",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                  }
                }
              }
              Spacer(modifier = Modifier.height(16.dp))
              SettingsDivider()
              SettingsMenuLink(
                title = { Text(text = "Log Out") },
                subtitle = { Text(text = "Sign out from your account") },
                onClick = { viewModel.signOut() },
                icon = {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = "Log Out",
                    tint = MaterialTheme.colorScheme.error
                  )
                },
                colors = SettingsTileDefaults.colors(containerColor = Color.Transparent)
              )
            }
          } else { // Logged out state
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Icon(
                painter = painterResource(id = R.drawable.ic_default_profile),
                contentDescription = "Not logged in",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Sign in to use the app",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
              )
              Spacer(modifier = Modifier.height(20.dp))
              Button(
                onClick = {
                  if (activity != null) {
                    viewModel.signIn(activity)
                  } else {
                    Log.e("SettingsScreen", "Activity context is null, cannot start sign-in flow.")
                    showToast(context, "Cannot initiate sign-in at the moment.", true)
                  }
                },
                modifier = Modifier.fillMaxWidth(0.8f),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary,
                  contentColor = MaterialTheme.colorScheme.onPrimary
                )
              ) {
                Text(text = "Log In with Google", fontWeight = FontWeight.SemiBold)
              }
            }
          }
        }
      }

      item {
        SettingsCardGroup(title = "Transcription Service") {
          val settingsState by viewModel.settings.collectAsStateWithLifecycle()
          SettingsMenuLink(
            title = { Text(text = "Transcription Provider") },
            subtitle = { Text(text = if (settingsState.selectedTranscriptionProvider.toString() == "OPEN_AI") "Open AI" else "Google Gemini") },
            onClick = { viewModel.showDialog(DialogType.TRANSCRIPTION_PROVIDER) },
            icon = {
              Icon(
                imageVector = Icons.Default.GeneratingTokens,
                contentDescription = "Transcription Provider",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = SettingsTileDefaults.colors(containerColor = Color.Transparent)
          )
          SettingsDivider()
          SettingsMenuLink(
            title = { Text(text = "Summarization & Translation Provider") },
            subtitle = { Text(text = if (settingsState.selectedSummaryProvider.toString() == "OPEN_AI") "Open AI" else "Google Gemini") },
            onClick = { viewModel.showDialog(DialogType.SUMMARIZATION_PROVIDER) },
            icon = {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Message,
                contentDescription = "Model",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = SettingsTileDefaults.colors(containerColor = Color.Transparent)
          )
        }
      }

      item {
        SettingsCardGroup(title = "General Preferences") {
          val settingsState by viewModel.settings.collectAsStateWithLifecycle()
          SettingsMenuLink(
            title = { Text(text = "Language") },
            subtitle = { Text(text = settingsState.selectedLanguage) },
            onClick = { viewModel.showDialog(DialogType.LANGUAGE) },
            icon = {
              Icon(
                imageVector = Icons.Default.Language,
                contentDescription = "Language",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = SettingsTileDefaults.colors(containerColor = Color.Transparent),
          )
          SettingsDivider()
          SettingsCheckbox(
            state = settingsState.autoSave,
            title = { Text(text = "Autosave Transcriptions") },
            subtitle = { Text(text = "Save transcriptions automatically after generation.") },
            onCheckedChange = { viewModel.setAutoSave(it) },
            icon = {
              Icon(
                imageVector = Icons.Default.AutoMode,
                contentDescription = "Autosave",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = SettingsTileDefaults.colors(containerColor = Color.Transparent),
          )
          SettingsDivider()
          SettingsSwitch(
            state = settingsState.enableSilenceTrimming,
            title = { Text(text = "Remove Long Silences") },
            subtitle = { Text(text = "Automatically trim silence before transcription to reduce costs") },
            icon = {
              Icon(
                imageVector = Icons.Default.VolumeOff,
                contentDescription = "Silence Trimming",
                tint = MaterialTheme.colorScheme.primary
              )
            },
            colors = SettingsTileDefaults.colors(containerColor = Color.Transparent),
            onCheckedChange = { viewModel.setSilenceTrimming(it) }
          )

          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            SettingsDivider()
            SettingsSwitch(
              state = settingsState.dynamicColor,
              title = { Text(text = "Dynamic Color") },
              subtitle = { Text(text = "Use Material You theme colors (requires app restart)") },
              icon = {
                Icon(
                  imageVector = Icons.Default.ColorLens,
                  contentDescription = "Dynamic Color",
                  tint = MaterialTheme.colorScheme.primary
                )
              },
              colors = SettingsTileDefaults.colors(containerColor = Color.Transparent),
              onCheckedChange = {
                viewModel.setDynamicColor(it)
                showToast(context, "Restart app to apply changes.", true)
              },
            )
          }
        }
      }

      item {
        SettingsCardGroup(title = "Data Management") {
          SettingsMenuLink(
            title = { Text(text = "Delete All Transcriptions") },
            subtitle = { Text(text = "Permanently removes all saved transcriptions.") },
            onClick = { viewModel.showDialog(DialogType.DELETE) },
            icon = {
              Icon(
                imageVector = Icons.Default.DeleteSweep,
                contentDescription = "Delete Transcriptions",
                tint = MaterialTheme.colorScheme.error
              )
            },
            colors = SettingsTileDefaults.colors(containerColor = Color.Transparent),
          )
        }
      }

      item {
        val settingsState by viewModel.settings.collectAsStateWithLifecycle()
        val rotationAngle by animateFloatAsState(targetValue = if (expertSettingsExpanded) 180f else 0f, label = "expansion_arrow")

        SettingsCardGroup(
          modifier = Modifier.clickable { expertSettingsExpanded = !expertSettingsExpanded },
          titleComposable = {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Filled.DeveloperMode,
                  contentDescription = "Developer Options Icon",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.padding(end = 12.dp)
                )
                Text(
                  text = "Developer Options",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
              Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = if (expertSettingsExpanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotationAngle),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        ) {
          AnimatedVisibility(
            visible = expertSettingsExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
          ) {
            Column(modifier = Modifier.padding(top = 0.dp)) {
              SettingsDivider()
              SettingsSwitch(
                state = settingsState.mockApi,
                title = { Text(text = "Use Mock API") },
                subtitle = { Text(text = "For testing without real API calls") },
                icon = {
                  Icon(
                    imageVector = Icons.Default.BugReport,
                    contentDescription = "Mock API",
                    tint = MaterialTheme.colorScheme.primary
                  )
                },
                colors = SettingsTileDefaults.colors(containerColor = Color.Transparent),
                onCheckedChange = { viewModel.setMockApi(it) },
                modifier = Modifier.padding(horizontal = 0.dp)
              )
              // More developer options can be added here
            }
          }
        }
      }
    }

    val currentSettings by viewModel.settings.collectAsStateWithLifecycle()
    if (showDialog) {
      when (dialogType) {
        DialogType.LANGUAGE -> LanguageDialog(viewModel, currentSettings.selectedLanguage)
        DialogType.TRANSCRIPTION_PROVIDER -> TranscriptionDialog(viewModel, currentSettings.selectedTranscriptionProvider)
        DialogType.SUMMARIZATION_PROVIDER -> SummaryDialog(viewModel, currentSettings.selectedSummaryProvider)
        DialogType.DELETE -> DeleteDialog(viewModel)
      }
    }
  }
}

@Composable
fun SettingsCardGroup(
  title: String? = null,
  modifier: Modifier = Modifier,
  titleComposable: (@Composable ColumnScope.() -> Unit)? = null,
  content: @Composable ColumnScope.() -> Unit,
) {
  Card(
    modifier = modifier.fillMaxWidth(),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp))
  ) {
    Column(modifier = Modifier) {
      if (titleComposable != null) {
        Column { titleComposable() }
      } else if (title != null) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )

      }

      content()
    }
  }
}

@Composable
fun SettingsDivider() {
  HorizontalDivider(
    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    thickness = 0.5.dp,
    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
  )
}