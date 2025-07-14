package com.woocommerce.android.ui.products

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.AppConstants.FADE_ANIMATION_DELAY_MS
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.animations.SkeletonView
import com.woocommerce.android.ui.compose.component.FeedbackRequest
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOutlinedTextField
import com.woocommerce.android.ui.compose.component.WCTextButton
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.ProductSharingViewModel.AIButtonState
import com.woocommerce.android.ui.products.ProductSharingViewModel.AIButtonState.Generating
import com.woocommerce.android.ui.products.ProductSharingViewModel.AIButtonState.Regenerate
import com.woocommerce.android.ui.products.ProductSharingViewModel.AIButtonState.WriteWithAI
import com.woocommerce.android.ui.products.ProductSharingViewModel.ProductSharingViewState

@Composable
fun ProductSharingBottomSheet(viewModel: ProductSharingViewModel) {
    viewModel.viewState.observeAsState().value?.let {
        ProductShareWithAI(
            viewState = it,
            onGenerateButtonClick = viewModel::onGenerateButtonClicked,
            onShareMessageEdit = viewModel::onShareMessageEdited,
            onSharingButtonClick = viewModel::onShareButtonClicked,
            onInfoButtonClick = viewModel::onInfoButtonClicked,
            onDescriptionFeedbackReceived = viewModel::onDescriptionFeedbackReceived
        )
    }
}

@Composable
fun ProductShareWithAI(
    viewState: ProductSharingViewState,
    onGenerateButtonClick: () -> Unit = {},
    onShareMessageEdit: (String) -> Unit = {},
    onSharingButtonClick: () -> Unit = {},
    onInfoButtonClick: () -> Unit = {},
    onDescriptionFeedbackReceived: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colors.surface)
    ) {
        Text(
            text = stringResource(id = R.string.share) + " ${viewState.productTitle}",
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
        )
        Divider(
            color = colorResource(id = R.color.divider_color),
            thickness = dimensionResource(id = R.dimen.minor_10)
        )
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.major_100))
        ) {
            if (viewState.isGenerating) {
                SharingMessageSkeletonView()
            } else {
                val isError = viewState.errorMessage.isNotEmpty()

                WCOutlinedTextField(
                    value = viewState.shareMessage,
                    onValueChange = { onShareMessageEdit(it) },
                    label = stringResource(id = R.string.product_sharing_optional_message_label),
                    isError = isError,
                    helperText = if (isError) viewState.errorMessage else null,
                    textFieldModifier = Modifier.height(dimensionResource(id = R.dimen.multiline_textfield_height))
                )

                Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.major_100)))

                AnimatedVisibility(
                    visible = viewState.shouldShowFeedbackForm,
                    enter = fadeIn(animationSpec = tween(FADE_ANIMATION_DELAY_MS)),
                    exit = fadeOut(animationSpec = tween(FADE_ANIMATION_DELAY_MS))
                ) {
                    FeedbackRequest(
                        feedbackRequestText = R.string.ai_feedback_form_message,
                        onFeedbackReceived = onDescriptionFeedbackReceived
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .padding(
                        vertical = dimensionResource(id = R.dimen.minor_100)
                    )
                    .fillMaxWidth()
            ) {
                WCOutlinedButton(
                    onClick = onGenerateButtonClick,
                    enabled = !viewState.isGenerating
                ) {
                    AIButtonContent(buttonState = viewState.buttonState)
                }

                val learnMoreButtonColor = if (viewState.isGenerating) {
                    colorResource(id = R.color.color_on_surface_disabled)
                } else {
                    colorResource(id = R.color.color_primary)
                }
                WCTextButton(
                    onClick = onInfoButtonClick,
                    enabled = !viewState.isGenerating
                ) {
                    Text(
                        text = stringResource(id = R.string.learn_more),
                        style = MaterialTheme.typography.body2,
                        color = learnMoreButtonColor,
                        textAlign = TextAlign.End
                    )
                }
            }

            WCColoredButton(
                onClick = onSharingButtonClick,
                modifier = Modifier.fillMaxWidth(),
                enabled = !viewState.isGenerating
            ) {
                Text(text = stringResource(id = R.string.share))
            }
        }
    }
}

@Composable
fun SharingMessageSkeletonView() {
    Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_75)))
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(dimensionResource(id = R.dimen.minor_50))
            )
            .padding(dimensionResource(id = R.dimen.major_110))
            .fillMaxWidth()
    ) {
        SkeletonView(
            modifier = Modifier
                .fillMaxWidth()
                .height(dimensionResource(id = R.dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.skeleton_text_large_width))
                .height(dimensionResource(id = R.dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.skeleton_text_extra_large_width))
                .height(dimensionResource(id = R.dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.skeleton_text_large_width))
                .height(dimensionResource(id = R.dimen.major_100))
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.minor_100)))
        SkeletonView(
            modifier = Modifier
                .width(dimensionResource(id = R.dimen.skeleton_text_extra_large_width))
                .height(dimensionResource(id = R.dimen.major_100))
        )
    }
}

@Composable
fun AIButtonContent(buttonState: AIButtonState) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (buttonState) {
            is WriteWithAI -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_ai_share_button),
                    contentDescription = null
                )
            }

            is Regenerate -> {
                Icon(
                    painter = painterResource(id = R.drawable.ic_regenerate),
                    contentDescription = null
                )
            }

            is Generating -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(dimensionResource(id = R.dimen.major_100)),
                    strokeWidth = dimensionResource(id = R.dimen.minor_25),
                    color = colorResource(id = R.color.color_on_surface_disabled)
                )
            }
        }
        Text(
            text = buttonState.label,
            modifier = Modifier
                .padding(
                    horizontal = dimensionResource(id = R.dimen.major_75)
                )
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(name = "Smaller screen", device = Devices.NEXUS_5)
@Preview(name = "RTL mode", locale = "ar")
@Composable
fun DefaultUIWithSharingContent() {
    val shareMessage =
        "Hey! 🎵 I just listened to the new album \"Album Title\" by Artist Name, and it's fantastic! Check it out " +
            "now on your favorite music platform and join the conversation using #AlbumTitleByArtistName. Let's " +
            "spread the love for this amazing music! 🎧💕 #NewMusicAlert"
    WooThemeWithBackground {
        ProductShareWithAI(
            viewState = ProductSharingViewState(
                productTitle = "Music Album",
                shareMessage = shareMessage,
                buttonState = WriteWithAI(stringResource(id = R.string.product_sharing_write_with_ai))
            )
        )
    }
}

@Preview
@Composable
fun DefaultUIWithRegenerateButton() {
    val shareMessage =
        "Hey! 🎵 I just listened to the new album \"Album Title\" by Artist Name, and it's fantastic! Check it out " +
            "now on your favorite music platform and join the conversation using #AlbumTitleByArtistName. Let's " +
            "spread the love for this amazing music! 🎧💕 #NewMusicAlert"
    WooThemeWithBackground {
        ProductShareWithAI(
            viewState = ProductSharingViewState(
                productTitle = "Music Album",
                shareMessage = shareMessage,
                buttonState = Regenerate(stringResource(id = R.string.product_sharing_regenerate))
            )
        )
    }
}

@Preview
@Composable
fun DefaultUIWithRegenerateButtonAndFeedbackForm() {
    val shareMessage =
        "Hey! 🎵 I just listened to the new album \"Album Title\" by Artist Name, and it's fantastic! Check it out " +
            "now on your favorite music platform and join the conversation using #AlbumTitleByArtistName. Let's " +
            "spread the love for this amazing music! 🎧💕 #NewMusicAlert"
    WooThemeWithBackground {
        ProductShareWithAI(
            viewState = ProductSharingViewState(
                productTitle = "Music Album",
                shareMessage = shareMessage,
                buttonState = Regenerate(stringResource(id = R.string.product_sharing_regenerate)),
                shouldShowFeedbackForm = true
            )
        )
    }
}

@Preview
@Composable
fun Generating() {
    WooThemeWithBackground {
        ProductShareWithAI(
            viewState = ProductSharingViewState(
                productTitle = "Music Album",
                shareMessage = "",
                buttonState = Generating(stringResource(id = R.string.product_sharing_generating)),
                isGenerating = true
            )
        )
    }
}
