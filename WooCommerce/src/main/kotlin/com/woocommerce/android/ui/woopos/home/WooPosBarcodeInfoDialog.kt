package com.woocommerce.android.ui.woopos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosOutlinedButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptivePadding
import com.woocommerce.android.util.ChromeCustomTabUtils

private const val WOO_POS_BARCODE_DOC_URL = "https://woocommerce.com/document/barcode-and-qr-code-scanner/"

@Composable
fun WooPosBarcodeInfoDialog(
    state: WooPosHomeState.BarcodeInfoDialog,
    onDismissRequest: () -> Unit,
) {
    val context = LocalContext.current
    val dialogContentDescription = getCombinedContentDescription(state = state)
    val primaryButtonContentDescription = stringResource(
        id = R.string.woopos_banner_simple_products_dialog_primary_button_content_description
    )
    val dialogBackgroundContentDescription = stringResource(
        id = R.string.woopos_dialog_barcode_info_background_content_description
    )
    WooPosDialogWrapper(
        modifier = Modifier,
        isVisible = state.isVisible,
        dialogBackgroundContentDescription = dialogBackgroundContentDescription,
        onDismissRequest = onDismissRequest
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceBright)
                .padding(WooPosSpacing.XLarge.value.toAdaptivePadding())
                .semantics(mergeDescendants = true) {
                    contentDescription = dialogContentDescription
                },
            contentAlignment = Alignment.Center
        ) {
            @Suppress("DestructuringDeclarationWithTooManyEntries")
            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (
                    header,
                    closeIcon,
                    introText,
                    primaryText,
                    secondaryText,
                    tertiaryText,
                    quaternaryText,
                    quinaryBox,
                    primaryButton
                ) = createRefs()

                WooPosText(
                    text = stringResource(id = state.header),
                    style = WooPosTypography.Heading,
                    fontWeight = FontWeight.Bold,

                    modifier = Modifier
                        .padding(
                            top = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                            bottom = WooPosSpacing.Medium.value.toAdaptivePadding()
                        )
                        .constrainAs(header) {
                            top.linkTo(closeIcon.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.preferredWrapContent
                        }
                )

                WooPosText(
                    text = stringResource(id = state.introMessage),
                    style = WooPosTypography.BodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            bottom = WooPosSpacing.Medium.value.toAdaptivePadding()
                        )
                        .constrainAs(introText) {
                            top.linkTo(header.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )

                WooPosText(
                    text = stringResource(id = state.primaryMessage),
                    style = WooPosTypography.BodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = WooPosSpacing.Medium.value.toAdaptivePadding())
                        .constrainAs(primaryText) {
                            top.linkTo(introText.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )

                val secondaryMessage = stringResource(id = state.secondaryMessage)
                val moreDetailsText = stringResource(id = R.string.woopos_dialog_barcode_info_more_details_link)

                val linkAnnotation = LinkAnnotation.Url(
                    WOO_POS_BARCODE_DOC_URL
                ) { urlAnnotation ->
                    ChromeCustomTabUtils.launchUrl(
                        context,
                        WOO_POS_BARCODE_DOC_URL,
                        enableSlideAnimation = true
                    )
                }

                val annotatedText = buildAnnotatedString {
                    append(secondaryMessage)
                    withStyle(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.primary,
                            textDecoration = TextDecoration.Underline
                        )
                    ) {
                        withLink(linkAnnotation) {
                            append(" $moreDetailsText.")
                        }
                    }
                }

                WooPosText(
                    text = annotatedText,
                    style = WooPosTypography.BodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WooPosSpacing.Medium.value.toAdaptivePadding(),
                            top = WooPosSpacing.Small.value.toAdaptivePadding()
                        )
                        .constrainAs(secondaryText) {
                            top.linkTo(primaryText.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )

                WooPosText(
                    text = stringResource(id = state.tertiaryMessage),
                    style = WooPosTypography.BodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WooPosSpacing.Medium.value.toAdaptivePadding(),
                            top = WooPosSpacing.Small.value.toAdaptivePadding(),
                        )
                        .constrainAs(tertiaryText) {
                            top.linkTo(secondaryText.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )

                WooPosText(
                    text = stringResource(id = state.quaternaryMessage),
                    style = WooPosTypography.BodyLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = WooPosSpacing.Medium.value.toAdaptivePadding(),
                            top = WooPosSpacing.Small.value.toAdaptivePadding(),
                        )
                        .constrainAs(quaternaryText) {
                            top.linkTo(tertiaryText.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )

                val bigMargin = 40.dp.toAdaptivePadding()

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                        .background(
                            color = MaterialTheme.colorScheme.surfaceDim
                        )
                        .padding(
                            vertical = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                            horizontal = WooPosSpacing.Medium.value.toAdaptivePadding()
                        )
                        .constrainAs(quinaryBox) {
                            top.linkTo(quaternaryText.bottom, margin = bigMargin)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    WooPosText(
                        text = stringResource(id = state.quinaryMessage),
                        style = WooPosTypography.BodySmall,
                        textAlign = TextAlign.Center,
                    )
                }

                val buttonMargin = WooPosSpacing.XLarge.value.toAdaptivePadding()
                WooPosOutlinedButton(
                    onClick = { onDismissRequest() },
                    text = stringResource(id = state.primaryButton.label),
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = primaryButtonContentDescription
                        }
                        .constrainAs(primaryButton) {
                            top.linkTo(quinaryBox.bottom, margin = buttonMargin)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                        }
                )
            }
        }
    }
}

@Composable
private fun getCombinedContentDescription(state: WooPosHomeState.BarcodeInfoDialog): String {
    val dialogContentDescription = stringResource(
        id = R.string.woopos_dialog_barcode_info_content_description
    )
    return "$dialogContentDescription\n${stringResource(id = state.header)}" +
        "\n${stringResource(id = state.introMessage)}\n${stringResource(id = state.primaryMessage)}" +
        "\n${stringResource(id = state.secondaryMessage)}\n${stringResource(id = state.tertiaryMessage)}" +
        "\n${stringResource(id = state.quaternaryMessage)}\n${stringResource(id = state.quinaryMessage)}"
}

@WooPosPreview
@Composable
fun BarcodeInfoDialogPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WooPosBarcodeInfoDialog(
                state = WooPosHomeState.BarcodeInfoDialog(isVisible = true),
                onDismissRequest = {},
            )
        }
    }
}
