package com.woocommerce.android.ui.woopos.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@Composable
fun WooPosProductInfoDialog(
    state: WooPosHomeState.ProductsInfoDialog,
    onDismissRequest: () -> Unit,
) {
    val dialogContentDescription = getCombinedContentDescription(state = state)
    val primaryButtonContentDescription = stringResource(
        id = R.string.woopos_banner_simple_products_dialog_primary_button_content_description
    )
    val dialogBackgroundContentDescription = stringResource(
        id = R.string.woopos_dialog_products_info_background_content_description
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
            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (header, closeIcon, content) = createRefs()

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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.constrainAs(content) {
                        top.linkTo(header.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                ) {
                    WooPosText(
                        text = stringResource(id = state.primaryMessage),
                        style = WooPosTypography.BodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
                    )
                    WooPosText(
                        text = stringResource(id = state.secondaryMessage),
                        style = WooPosTypography.BodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = WooPosSpacing.Medium.value.toAdaptivePadding())
                    )
                    @Suppress("WooPosDesignSystemSpacingUsageRule")
                    Spacer(Modifier.height(40.dp.toAdaptivePadding()))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(WooPosCornerRadius.Medium.value))
                            .background(
                                color = MaterialTheme.colorScheme.surfaceDim
                            )
                            .padding(
                                vertical = WooPosSpacing.XLarge.value.toAdaptivePadding(),
                                horizontal = WooPosSpacing.Medium.value.toAdaptivePadding()
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            WooPosText(
                                text = stringResource(id = state.tertiaryMessage),
                                style = WooPosTypography.BodySmall,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(WooPosSpacing.XLarge.value.toAdaptivePadding()))
                    WooPosOutlinedButton(
                        onClick = { onDismissRequest() },
                        text = stringResource(id = state.primaryButton.label),
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = primaryButtonContentDescription
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun getCombinedContentDescription(state: WooPosHomeState.ProductsInfoDialog): String {
    val dialogContentDescription = stringResource(
        id = R.string.woopos_banner_simple_products_dialog_content_description
    )
    return "$dialogContentDescription\n${stringResource(id = state.header)}" +
        "\n${stringResource(id = state.primaryMessage)}\n${stringResource(id = state.tertiaryMessage)}"
}

@WooPosPreview
@Composable
fun ProductInfoDialogPreview() {
    WooPosTheme {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            WooPosProductInfoDialog(
                state = WooPosHomeState.ProductsInfoDialog(isVisible = true),
                onDismissRequest = {},
            )
        }
    }
}
