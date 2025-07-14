package com.woocommerce.android.ui.products.ai.description

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R.color
import com.woocommerce.android.R.dimen
import com.woocommerce.android.R.drawable
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Celebration
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Generated
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Regenerating
import com.woocommerce.android.ui.products.ai.description.AIProductDescriptionViewModel.ViewState.GenerationState.Start

@Composable
fun AIProductDescriptionBottomSheet(
    viewModel: AIProductDescriptionViewModel
) {
    viewModel.viewState.observeAsState().value?.let { state ->
        DescriptionGenerationForm(
            viewState = state,
            onTitleChanged = viewModel::onTitleChanged,
            onFeaturesChanged = viewModel::onFeaturesChanged,
            onGenerateButtonClicked = viewModel::onGenerateButtonClicked,
            onRegenerateButtonClicked = viewModel::onRegenerateButtonClicked,
            onCopyButtonClicked = viewModel::onCopyButtonClicked,
            onApplyButtonClicked = viewModel::onApplyButtonClicked,
            onDescriptionFeedbackReceived = viewModel::onDescriptionFeedbackReceived,
            onCelebrationButtonClicked = viewModel::onCelebrationButtonClicked
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
@Suppress("LongParameterList")
fun DescriptionGenerationForm(
    viewState: ViewState,
    onTitleChanged: (String) -> Unit,
    onFeaturesChanged: (String) -> Unit,
    onGenerateButtonClicked: () -> Unit,
    onRegenerateButtonClicked: () -> Unit,
    onCopyButtonClicked: () -> Unit,
    onApplyButtonClicked: () -> Unit,
    onDescriptionFeedbackReceived: (Boolean) -> Unit,
    onCelebrationButtonClicked: () -> Unit,
) {
    AnimatedContent(viewState.generationState) { generationState ->
        when (generationState) {
            is Generated -> {
                if (generationState.showError) {
                    Error()
                }
                GenerationFlow(viewState, onTitleChanged, onFeaturesChanged) {
                    GeneratedDescription(
                        description = viewState.description,
                        onRegenerateButtonClicked = onRegenerateButtonClicked,
                        onApplyButtonClicked = onApplyButtonClicked,
                        onCopyButtonClicked = onCopyButtonClicked,
                        onDescriptionFeedbackReceived = onDescriptionFeedbackReceived
                    )
                }
            }
            GenerationState.Generating -> {
                GenerationFlow(viewState, onTitleChanged, onFeaturesChanged, enableTextFields = false) {
                    ProductDescriptionSkeletonView()
                }
            }
            is Start -> {
                GenerationFlow(viewState, onTitleChanged, onFeaturesChanged) {
                    if (generationState.showError) {
                        Error()
                    }

                    WCColoredButton(
                        onClick = onGenerateButtonClicked,
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(id = string.product_sharing_write_with_ai),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = drawable.ic_ai),
                                contentDescription = null,
                                tint = colorResource(id = color.woo_white)
                            )
                        }
                    )
                }
            }
            Regenerating -> {
                GenerationFlow(viewState, onTitleChanged, onFeaturesChanged, enableTextFields = true) {
                    RegenerationInProgress(onApplyButtonClicked)
                }
            }
            Celebration -> CelebrationDialog(onCelebrationButtonClicked)
        }
    }
}

@Composable
private fun Error() {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.error,
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_50))
            )
            .fillMaxWidth()
            .padding(dimensionResource(id = dimen.major_100))
    ) {
        Text(
            text = stringResource(id = string.ai_product_description_error),
            style = MaterialTheme.typography.body1,
            color = MaterialTheme.colors.onError,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun GenerationFlow(
    state: ViewState,
    onTitleChanged: (String) -> Unit,
    onFeaturesChanged: (String) -> Unit,
    enableTextFields: Boolean = true,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100)),
        ) {
            Text(
                text = stringResource(id = string.ai_product_description_title),
                style = MaterialTheme.typography.h6
            )

            if (state.isProductTitleInitiallyPresent) {
                Text(
                    text = state.productTitle,
                    style = MaterialTheme.typography.caption,
                    color = colorResource(id = color.color_on_surface_medium)
                )
            } else {
                OutlinedTextField(
                    value = state.productTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensionResource(id = dimen.minor_100)),
                    maxLines = 1,
                    enabled = enableTextFields,
                    isError = state.shouldShowErrorOutlineIfEmpty && state.productTitle.isEmpty(),
                    onValueChange = onTitleChanged,
                    placeholder = {
                        Text(stringResource(id = string.ai_product_description_title_hint))
                    }
                )
            }
        }
        Divider(
            color = colorResource(id = color.divider_color),
            thickness = dimensionResource(id = dimen.minor_10)
        )
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = dimen.major_100)),
            verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100))
        ) {
            OutlinedTextField(
                value = state.features,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = onFeaturesChanged,
                enabled = enableTextFields,
                isError = state.shouldShowErrorOutlineIfEmpty && state.features.isEmpty(),
                placeholder = {
                    Text(stringResource(id = string.ai_product_description_hint))
                }
            )

            Text(
                text = stringResource(id = string.ai_product_description_example),
                style = MaterialTheme.typography.caption,
                color = colorResource(id = color.color_on_surface_medium)
            )

            Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_75)))

            content()
        }
    }
}

@Composable
private fun RegenerationInProgress(onApplyButtonClicked: () -> Unit) {
    ProductDescriptionSkeletonView()

    Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_75)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        CircularProgressIndicator(
            modifier = Modifier.align(Alignment.CenterStart)
        )

        WCColoredButton(
            onClick = onApplyButtonClicked,
            modifier = Modifier
                .align(Alignment.CenterEnd),
            enabled = false
        ) {
            Text(
                text = stringResource(id = string.apply),
                modifier = Modifier.padding(horizontal = dimensionResource(id = dimen.major_100))
            )
        }
    }
}

@Composable
@Suppress("LongParameterList")
private fun GeneratedDescription(
    description: String,
    onRegenerateButtonClicked: () -> Unit,
    onApplyButtonClicked: () -> Unit,
    onCopyButtonClicked: () -> Unit,
    onDescriptionFeedbackReceived: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_50))
            )
            .fillMaxWidth()
            .padding(dimensionResource(id = dimen.major_100)),
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = dimen.minor_100))
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = MaterialTheme.typography.body1
        )

        WCTextButton(
            modifier = Modifier.align(Alignment.End),
            onClick = onCopyButtonClicked,
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(id = color.color_on_surface_medium)
            )
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = dimen.major_150))
            )
            Text(
                modifier = Modifier.padding(start = dimensionResource(id = dimen.minor_100)),
                text = stringResource(id = string.copy)
            )
        }

        val isFeedbackVisible = remember { mutableStateOf(true) }
        if (isFeedbackVisible.value) {
            ConstraintLayout(
                modifier = Modifier
                    .background(
                        color = colorResource(id = color.woo_black_alpha_008),
                        shape = RoundedCornerShape(dimensionResource(id = dimen.minor_50))
                    )
                    .fillMaxWidth()
                    .padding(
                        start = dimensionResource(id = dimen.major_100),
                        top = dimensionResource(id = dimen.minor_100),
                        bottom = dimensionResource(id = dimen.minor_100)
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
                    text = stringResource(id = string.ai_product_description_feedback),
                    color = colorResource(id = color.color_on_surface_medium),
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
                        modifier = Modifier.size(dimensionResource(id = dimen.major_150)),
                        tint = colorResource(id = color.color_on_surface_medium)
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
                        modifier = Modifier.size(dimensionResource(id = dimen.major_150)),
                        tint = colorResource(id = color.color_on_surface_medium)
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_75)))

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        WCTextButton(
            onClick = onRegenerateButtonClicked,
            modifier = Modifier.align(Alignment.CenterStart),
            colors = ButtonDefaults.textButtonColors(
                contentColor = colorResource(id = color.color_on_surface)
            )
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(dimensionResource(id = dimen.major_150))
            )
            Text(
                modifier = Modifier.padding(start = dimensionResource(id = dimen.minor_100)),
                text = stringResource(id = string.ai_product_description_regenerate_button)
            )
        }
        WCColoredButton(
            onClick = onApplyButtonClicked,
            modifier = Modifier
                .align(Alignment.CenterEnd),
        ) {
            Text(
                text = stringResource(id = string.apply),
                modifier = Modifier.padding(horizontal = dimensionResource(id = dimen.major_100))
            )
        }
    }
}

@Composable
fun ProductDescriptionSkeletonView() {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(dimensionResource(id = dimen.minor_50))
            )
            .padding(dimensionResource(id = dimen.major_110))
            .fillMaxWidth()
    ) {
        SkeletonView(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = dimen.skeleton_text_large_width))
                .height(dimensionResource(id = dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = dimen.skeleton_text_extra_large_width))
                .height(dimensionResource(id = dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = dimen.skeleton_text_large_width))
                .height(dimensionResource(id = dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = dimen.skeleton_text_extra_large_width))
                .height(dimensionResource(id = dimen.major_100))
        )
    }
}

@Composable
fun CelebrationDialog(
    onConfirmClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
            .padding(all = dimensionResource(id = dimen.major_100))
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = drawable.img_ai_generated_content),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.major_100)))
        Text(
            text = stringResource(id = string.ai_product_description_note_dialog_heading),
            style = MaterialTheme.typography.h6,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.major_100)))
        Text(
            text = stringResource(id = string.ai_product_description_note_dialog_message),
            style = MaterialTheme.typography.body2,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = dimen.major_100)))

        WCColoredButton(onClick = onConfirmClick, modifier = Modifier.fillMaxWidth()) {
            Text(text = stringResource(id = string.ai_product_description_note_dialog_confirmation))
        }
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewAIDescriptionGenerationForm() {
    DescriptionGenerationForm(
        ViewState(
            generationState = Start(true),
            description = "This stylish and comfortable set is designed to enhance your performance and " +
                "keep you looking and feeling great during your workouts. Upgrade your fitness game and " +
                "make a statement with the \"Fit Fashionista\" activewear set.",
            isProductTitleInitiallyPresent = true
        ),
        onTitleChanged = {},
        onFeaturesChanged = {},
        onGenerateButtonClicked = {},
        onRegenerateButtonClicked = {},
        onCopyButtonClicked = {},
        onApplyButtonClicked = {},
        onDescriptionFeedbackReceived = {},
        onCelebrationButtonClicked = {}
    )
}
