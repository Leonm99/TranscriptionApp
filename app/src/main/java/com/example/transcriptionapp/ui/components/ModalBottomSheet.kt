package com.example.transcriptionapp.ui.components

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val (isLoading, transcription) = viewModel.transcriptionState.collectAsState(
        initial = TranscriptionState()
    ).value

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
                        Log.d("ModalBottomSheet", "FINISH")
                        activity.finish()
                    }
                }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(100.dp))
                    } else {
                        transcription?.let {
                            Text(
                                modifier = Modifier.padding(16.dp),
                                text = "Transcription: $it"
                            )
                            Box(modifier = Modifier.fillMaxHeight(),
                                contentAlignment = Alignment.BottomEnd){
                            Button(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = 0,
                                            y = -sheetState.requireOffset()
                                                .toInt()
// https://stackoverflow.com/questions/76454800/how-to-place-a-sticky-bottom-row-bar-at-modalbottomsheet-using-jetpack-compose
                                        )
                                    }
                                    .wrapContentWidth()
                                    .shadow(
                                        elevation = 4.dp,
                                        shape = RoundedCornerShape(
                                            25.dp
                                        )
                                    ),

                                onClick = { viewModel.summarize() }

                            ) {
                                Text(text = "Summarize")
                            }
                            }
                        }
                    }
                }
            }
        }



    }
}
