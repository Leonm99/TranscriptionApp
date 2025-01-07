package com.example.transcriptionapp.ui.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.transcriptionapp.viewmodel.TranscriptionState
import com.example.transcriptionapp.viewmodel.TranscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheet(
    viewModel: TranscriptionViewModel,
    activity: ComponentActivity? = null,
    finishAfter: Boolean? = false,
) {
    val showBottomSheet by viewModel.isBottomSheetVisible.collectAsState()
    val (isLoading, transcription) = viewModel.transcriptionState.collectAsState(
        initial = TranscriptionState()
    ).value
    var isFullyExpanded by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = isFullyExpanded)

    LaunchedEffect(transcription) {
        isFullyExpanded = (transcription?.length ?: 0) > 500 // Example threshold
    }
    Box(modifier = Modifier.fillMaxSize()) {

        if (showBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(top = 50.dp),
                sheetState = sheetState,
                onDismissRequest = {
                    viewModel.hideBottomSheet()
                    if (activity != null && finishAfter == true) {
                        activity.finish()
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(100.dp))
                    } else {
                        transcription?.let {
                            TranscriptionCard(it)
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight(),

                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                        .offset {
                                    IntOffset(
                                        x = 0,
                                        y = -sheetState.requireOffset()
                                            .toInt()
                                    )
                                },
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    StickyBottomSheetButton(
                                        onClick = { viewModel.summarize() },
                                        text = "Summarize",
                                    )
                                    StickyBottomSheetButton(
                                        onClick = { viewModel.translate() },
                                        text = "Translate"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
