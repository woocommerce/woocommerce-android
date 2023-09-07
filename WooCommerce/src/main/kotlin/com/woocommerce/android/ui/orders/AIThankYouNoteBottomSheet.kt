package com.woocommerce.android.ui.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCTextButton
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
fun ThankYouNoteGenerationForm(generatedThankYouNote: String, generationState: GenerationState) {
    Text(generatedThankYouNote)
    AnimatedContent(generationState) { state ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
                .verticalScroll(rememberScrollState())
        ) {
            Header()

            when (state) {
                is GenerationState.Generating -> StartState()
                is GenerationState.Generated -> GeneratedState(generatedThankYouNote)
                is GenerationState.Regenerating -> Text("Success")
            }
        }
    }
}

@Composable
fun Header() {
    Column {
        Text(
            text = stringResource(id = R.string.ai_order_thank_you_note_dialog_title),
            style = MaterialTheme.typography.h6,
            modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))
        )

        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
    }
}

@Composable
fun StartState() {
    Column(modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100))) {
        Text(
            text = stringResource(R.string.ai_order_thank_you_note_dialog_loading_message),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        CircularProgressIndicator(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
                .size(dimensionResource(id = R.dimen.major_150))
                .align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
fun GeneratedState(note: String) {
    Column(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
    ) {
        Column(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
                )
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.minor_100))
        ) {
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = note,
                style = MaterialTheme.typography.body1
            )

            WCTextButton(
                modifier = Modifier.align(Alignment.End),
                onClick = { },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = colorResource(id = R.color.color_on_surface_medium)
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(dimensionResource(id = R.dimen.major_150))
                )
                Text(
                    modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                    text = stringResource(id = R.string.copy)
                )
            }
        }
    }
}
