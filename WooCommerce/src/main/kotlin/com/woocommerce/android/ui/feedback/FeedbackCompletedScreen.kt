package com.woocommerce.android.ui.feedback

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.designsystem.compose.WooTheme
import com.woocommerce.android.ui.designsystem.compose.component.WooLinkedBodyText
import com.woocommerce.android.ui.designsystem.compose.component.WooPrimaryButton
import com.woocommerce.android.ui.designsystem.compose.component.WooTopAppBar
import com.woocommerce.android.ui.designsystem.compose.foundation.WooDesignSystemThemeWithBackground
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun FeedbackCompletedScreen(
    onCloseClick: () -> Unit,
    onBackToStoreClick: () -> Unit,
    onContactUsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contactUsText = stringResource(R.string.feedback_completed_contact_us)
    val descriptionText = stringResource(R.string.feedback_completed_description, contactUsText)
    val description = remember(descriptionText, contactUsText) {
        buildFeedbackCompletedDescription(
            descriptionText = descriptionText,
            contactUsText = contactUsText,
        )
    }

    FeedbackCompletedScreenContent(
        toolbarTitle = stringResource(R.string.feedback_completed_title),
        title = stringResource(R.string.feedback_completed_title_message),
        description = description,
        backToStoreButtonText = stringResource(R.string.feedback_completed_back_to_store_button_text),
        onCloseClick = onCloseClick,
        onBackToStoreClick = onBackToStoreClick,
        onContactUsClick = onContactUsClick,
        modifier = modifier,
    )
}

@Composable
private fun FeedbackCompletedScreenContent(
    toolbarTitle: String,
    title: String,
    description: AnnotatedString,
    backToStoreButtonText: String,
    onCloseClick: () -> Unit,
    onBackToStoreClick: () -> Unit,
    onContactUsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WooTopAppBar(
                title = toolbarTitle,
                navigationIcon = ImageVector.vectorResource(R.drawable.ic_gridicons_cross_24dp),
                navigationIconContentDescription = stringResource(R.string.close),
                onNavigationClick = onCloseClick,
                windowInsets = WindowInsets(0),
            )
        },
        containerColor = WooTheme.colors.background.section,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = MAX_CONTENT_WIDTH)
                    .fillMaxWidth()
                    .padding(WooTheme.padding.padding8),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FeedbackCompletedTitle(text = title)
                Image(
                    painter = painterResource(R.drawable.img_success_tablet),
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(top = WooTheme.spacing.space9)
                        .height(dimensionResource(R.dimen.image_major_150)),
                )
                WooLinkedBodyText(
                    text = description,
                    onLinkClick = { linkTag ->
                        if (linkTag == CONTACT_US_LINK_TAG) {
                            onContactUsClick()
                        }
                    },
                    modifier = Modifier
                        .padding(
                            start = WooTheme.spacing.space7,
                            top = WooTheme.spacing.space9,
                            end = WooTheme.spacing.space7,
                        )
                        .fillMaxWidth(),
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = WooTheme.spacing.space8),
                    contentAlignment = Alignment.Center,
                ) {
                    WooPrimaryButton(
                        text = backToStoreButtonText,
                        onClick = onBackToStoreClick,
                    )
                }
                Image(
                    painter = painterResource(R.drawable.img_crowdsignal_attribution),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .padding(top = WooTheme.spacing.space7)
                        .width(dimensionResource(R.dimen.image_major_150)),
                )
            }
        }
    }
}

@Composable
private fun FeedbackCompletedTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .semantics { heading() },
        color = WooTheme.colors.background.onSection,
        style = WooTheme.text.headlineSmall.strong,
        textAlign = TextAlign.Center,
    )
}

private fun buildFeedbackCompletedDescription(
    descriptionText: String,
    contactUsText: String,
): AnnotatedString {
    val contactUsStartIndex = descriptionText.indexOf(contactUsText)

    return buildAnnotatedString {
        withStyle(ParagraphStyle(textAlign = TextAlign.Center)) {
            if (contactUsStartIndex < 0) {
                append(descriptionText)
                return@withStyle
            }

            append(descriptionText.substring(startIndex = 0, endIndex = contactUsStartIndex))
            withLink(LinkAnnotation.Clickable(tag = CONTACT_US_LINK_TAG, linkInteractionListener = {})) {
                append(contactUsText)
            }
            append(descriptionText.substring(startIndex = contactUsStartIndex + contactUsText.length))
        }
    }
}

@PreviewLightDark
@Composable
private fun FeedbackCompletedScreenLegacyPreview() {
    WooThemeWithBackground {
        FeedbackCompletedScreenPreviewContent()
    }
}

@PreviewLightDark
@Composable
private fun FeedbackCompletedScreenDesignSystemPreview() {
    WooDesignSystemThemeWithBackground {
        FeedbackCompletedScreenPreviewContent()
    }
}

@Preview(name = "Long text", widthDp = 320)
@Composable
private fun FeedbackCompletedScreenLongTextPreview() {
    WooDesignSystemThemeWithBackground {
        FeedbackCompletedScreenPreviewContent(
            title = "Thank you for sharing detailed thoughts about your store experience with us",
            descriptionText = "Keep in mind that this is not a support ticket and we won't be able to address " +
                "individual feedback. If you need help with store setup, payments, products, or orders, " +
                "Contact us here",
        )
    }
}

@Preview(name = "Large font", fontScale = 1.5f)
@Composable
private fun FeedbackCompletedScreenLargeFontPreview() {
    WooDesignSystemThemeWithBackground {
        FeedbackCompletedScreenPreviewContent()
    }
}

@Preview(name = "RTL Arabic", locale = "ar")
@Composable
private fun FeedbackCompletedScreenRtlPreview() {
    WooDesignSystemThemeWithBackground {
        FeedbackCompletedScreenPreviewContent()
    }
}

@Composable
private fun FeedbackCompletedScreenPreviewContent(
    title: String = "Thank you for sharing your\n thoughts with us",
    descriptionText: String = "Keep in mind that this is not a support ticket and we won't be able to address " +
        "individual feedback.\n\nNeed some help? Contact us here",
) {
    val contactUsText = "Contact us here"
    FeedbackCompletedScreenContent(
        toolbarTitle = "Feedback sent",
        title = title,
        description = buildFeedbackCompletedDescription(
            descriptionText = descriptionText,
            contactUsText = contactUsText,
        ),
        backToStoreButtonText = "Back to store",
        onCloseClick = {},
        onBackToStoreClick = {},
        onContactUsClick = {},
    )
}

private val MAX_CONTENT_WIDTH = 560.dp
private const val CONTACT_US_LINK_TAG = "contact_us"
