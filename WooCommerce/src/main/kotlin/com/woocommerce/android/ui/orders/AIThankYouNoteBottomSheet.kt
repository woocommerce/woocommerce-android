package com.woocommerce.android.ui.orders

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun AIThankYouNoteBottomSheet(viewModel: AIThankYouNoteViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        ThankYouNoteGenerationForm(
            generatedThankYouNote = state.generatedThankYouNote
        )
    }
}

@Composable
fun ThankYouNoteGenerationForm(
    generatedThankYouNote: String
) {
    Text(generatedThankYouNote)
}
