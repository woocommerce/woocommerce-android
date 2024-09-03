package com.woocommerce.android.ui.woopos.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding

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
        modifier = Modifier
            .fillMaxWidth()
            .padding(102.dp.toAdaptivePadding())
            .testTag("woo_pos_product_info_dialog_background"),
        isVisible = state.isVisible,
        dialogBackgroundContentDescription = dialogBackgroundContentDescription,
        onDismissRequest = onDismissRequest
    ) {
        Box(
            modifier = Modifier
                .padding(40.dp.toAdaptivePadding())
                .semantics(mergeDescendants = true) {
                    contentDescription = dialogContentDescription
                }
                .testTag("woo_pos_product_info_dialog"),
            contentAlignment = Alignment.Center
        ) {
            ConstraintLayout(
                modifier = Modifier.fillMaxWidth()
            ) {
                val (header, closeIcon, content) = createRefs()

                Text(
                    text = stringResource(id = state.header),
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f),
                    modifier = Modifier
                        .padding(bottom = 16.dp.toAdaptivePadding())
                        .constrainAs(header) {
                            top.linkTo(closeIcon.bottom)
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            width = Dimension.preferredWrapContent
                        }
                )

                IconButton(
                    onClick = { onDismissRequest() },
                    modifier = Modifier
                        .constrainAs(closeIcon) {
                            top.linkTo(parent.top)
                            end.linkTo(parent.end)
                        }
                        .focusable(enabled = false)
                ) {
                    Icon(
                        modifier = Modifier
                            .size(40.dp)
                            .focusable(enabled = false),
                        imageVector = Icons.Default.Close,
                        tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                        contentDescription = stringResource(
                            id = R.string.woopos_banner_simple_products_close_content_description
                        ),
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.constrainAs(content) {
                        top.linkTo(header.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                    }
                ) {
                    Text(
                        text = stringResource(id = state.primaryMessage),
                        style = MaterialTheme.typography.h5,
                        color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp.toAdaptivePadding())
                    )
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                color = WooPosTheme.colors.dialogSubtitleHighlightBackground
                            )
                            .padding(16.dp.toAdaptivePadding()),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = stringResource(id = state.secondaryMessage),
                                style = MaterialTheme.typography.subtitle1,
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colors.onBackground.copy(alpha = 0.87f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp.toAdaptivePadding()))
                    OutlinedButton(
                        onClick = {
                            onDismissRequest()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = primaryButtonContentDescription
                            },
                        border = BorderStroke(2.dp, MaterialTheme.colors.primary),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            modifier = Modifier
                                .padding(vertical = 20.dp.toAdaptivePadding()),
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.h5,
                            text = stringResource(id = state.primaryButton.label)
                        )
                    }
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
        "\n${stringResource(id = state.primaryMessage)}\n${stringResource(id = state.secondaryMessage)}"
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
