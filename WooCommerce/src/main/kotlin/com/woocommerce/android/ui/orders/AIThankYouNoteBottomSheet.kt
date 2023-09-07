package com.woocommerce.android.ui.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import com.woocommerce.android.ui.orders.AIThankYouNoteViewModel.GenerationState

@Composable
fun AIThankYouNoteBottomSheet(viewModel: AIThankYouNoteViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        ThankYouNoteGenerationForm(
            generatedThankYouNote = state.generatedThankYouNote,
            generationState = state.generationState
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ThankYouNoteGenerationForm(
    generatedThankYouNote: String,
    generationState: GenerationState
) {
    Text(generatedThankYouNote)
    AnimatedContent(generationState) { generationState ->
        when (generationState) {
            is GenerationState.Start -> Text("Start")
            is GenerationState.Generating -> Text("Generating")
            is GenerationState.Generated -> Text("Generated")
            is GenerationState.Regenerating -> Text("Success")
        }
    }
}
