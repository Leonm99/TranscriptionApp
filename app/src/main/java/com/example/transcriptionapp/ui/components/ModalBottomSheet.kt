package com.example.transcriptionapp.ui.components

import android.util.Log
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val (isLoading, transcription, summary, translation, timestamp, _) = viewModel.transcriptionState.collectAsState(
        initial = TranscriptionState()
    ).value
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val context = LocalContext.current

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
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {


                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(100.dp))
                    } else {
                            Log.d("BottomSheet", "THIS $summary  $translation")
                            TranscriptionCard(transcription, summary = summary, translation = translation , timestamp) { text ->
                                viewModel.copyToClipboard(
                                    context,
                                    text
                                )
                            }
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

