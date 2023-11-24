package com.woocommerce.android.ui.orders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.orders.AIThankYouNoteViewModel.GenerationState

@Composable
fun AIThankYouNoteBottomSheet(viewModel: AIThankYouNoteViewModel) {
    viewModel.viewState.observeAsState().value?.let { state ->
        ThankYouNoteGenerationForm(
            generatedThankYouNote = state.generatedThankYouNote,
            generationState = state.generationState,
            onRegenerateButtonClicked = viewModel::onRegenerateButtonClicked,
            onCopyButtonClicked = viewModel::onCopyButtonClicked,
            onDescriptionFeedbackReceived = viewModel::onDescriptionFeedbackReceived,
            onShareButtonClicked = viewModel::onShareButtonClicked
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Suppress("LongParameterList")
@Composable
fun ThankYouNoteGenerationForm(
    generatedThankYouNote: String,
    generationState: GenerationState,
    onRegenerateButtonClicked: () -> Unit,
    onCopyButtonClicked: () -> Unit,
    onDescriptionFeedbackReceived: (Boolean) -> Unit,
    onShareButtonClicked: () -> Unit
) {
    AnimatedContent(generationState) { state ->
        Column(
            modifier = Modifier
                .background(MaterialTheme.colors.surface)
        ) {
            ThankYouNoteHeader()

            when (state) {
                is GenerationState.Generating -> GeneratingState()
                is GenerationState.Generated -> GeneratedState(
                    generatedThankYouNote,
                    onRegenerateButtonClicked,
                    onCopyButtonClicked,
                    onDescriptionFeedbackReceived,
                    onShareButtonClicked
                )
                is GenerationState.Regenerating -> GeneratingState(isRegenerating = true)
                is GenerationState.Failed -> FailedState(onRegenerateButtonClicked)
            }
        }
    }
}

@Composable
fun ThankYouNoteHeader() {
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
fun GeneratingState(isRegenerating: Boolean = false) {
    Column(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .let { if (isRegenerating) it.fillMaxHeight() else it },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
fun GeneratedState(
    note: String,
    onRegenerateButtonClicked: () -> Unit,
    onCopyButtonClicked: () -> Unit,
    onDescriptionFeedbackReceived: (Boolean) -> Unit,
    onShareButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100))
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
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
                onClick = onCopyButtonClicked,
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

            Survey(onDescriptionFeedbackReceived)

            ActionButtons(onRegenerateButtonClicked, onShareButtonClicked)
        }
    }
}

@Suppress("LongMethod")
@Composable
fun Survey(
    onDescriptionFeedbackReceived: (Boolean) -> Unit
) {
    Column {
        val isFeedbackVisible = remember { mutableStateOf(true) }
        if (isFeedbackVisible.value) {
            ConstraintLayout(
                modifier = Modifier
                    .background(
                        color = colorResource(id = R.color.woo_black_alpha_008),
                        shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
                    )
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = R.dimen.major_100),
                        top = dimensionResource(id = R.dimen.minor_100),
                        bottom = dimensionResource(id = R.dimen.minor_100)
                    )
            ) {
                val (text, like, dislike) = createRefs()

                Text(
                    modifier = Modifier
                        .constrainAs(text) {
                            top.linkTo(parent.top)
                            bottom.linkTo(parent.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(like.start)
                            width = Dimension.fillToConstraints
                        },
                    text = stringResource(id = R.string.ai_product_description_feedback),
                    color = colorResource(id = R.color.color_on_surface_medium),
                    style = MaterialTheme.typography.caption
                )

                IconButton(
                    modifier = Modifier
                        .constrainAs(like) {
                            top.linkTo(parent.top)
                            end.linkTo(dislike.start)
                            bottom.linkTo(parent.bottom)
                        },
                    onClick = {
                        onDescriptionFeedbackReceived(true)
                        isFeedbackVisible.value = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.major_150)),
                        tint = colorResource(id = R.color.color_on_surface_medium)
                    )
                }

                IconButton(
                    modifier = Modifier
                        .constrainAs(dislike) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom)
                        },
                    onClick = {
                        onDescriptionFeedbackReceived(false)
                        isFeedbackVisible.value = false
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbDown,
                        contentDescription = null,
                        modifier = Modifier.size(dimensionResource(id = R.dimen.major_150)),
                        tint = colorResource(id = R.color.color_on_surface_medium)
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtons(
    onRegenerateButtonClicked: () -> Unit,
    onShareButtonClicked: () -> Unit
) {
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_75)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        WCTextButton(
            onClick = onRegenerateButtonClicked,
            modifier = Modifier.align(Alignment.CenterStart),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(id = R.color.color_on_surface)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_150))
            )
            Text(
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                text = stringResource(id = R.string.ai_order_thank_you_note_dialog_regenerate_button)
            )
        }
        WCColoredButton(
            onClick = onShareButtonClicked,
            modifier = Modifier
                .align(Alignment.CenterEnd),
        ) {
            Text(
                text = stringResource(id = R.string.ai_order_thank_you_note_dialog_share_button),
                modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.major_100))
            )
        }
    }
}

@Composable
fun FailedState(
    onRegenerateButtonClicked: () -> Unit
) {
    Column(
        modifier = Modifier
            .padding(dimensionResource(id = R.dimen.major_100)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.ai_order_thank_you_note_dialog_failed_title),
            style = MaterialTheme.typography.body1,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        WCColoredButton(
            onClick = onRegenerateButtonClicked,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = dimensionResource(id = R.dimen.major_100))
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = R.dimen.major_150))
            )
            Text(
                modifier = Modifier.padding(start = dimensionResource(id = R.dimen.minor_100)),
                text = stringResource(id = R.string.ai_order_thank_you_note_dialog_regenerate_button)
            )
        }
    }
}
