package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosDialogWrapper
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosItemImage
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosLazyColumn
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosIconSize
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.toAdaptiveIconSize
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState
import com.woocommerce.android.ui.woopos.orders.details.WooPosOrderDetailsState

@Composable
fun WooPosRefundDetailsDialog(
    dialogState: WooPosOrderDetailsState.DialogState.RefundDetails,
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WooPosDialogWrapper(
        modifier = modifier,
        isVisible = isVisible,
        dialogBackgroundContentDescription = stringResource(
            R.string.woopos_orders_details_refund_details_background
        ),
        onCloseClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            WooPosText(
                text = dialogState.label,
                style = WooPosTypography.Heading,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = WooPosSpacing.XLarge.value),
            )

            WooPosLazyColumn(
                modifier = Modifier.weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(WooPosSpacing.None.value),
                contentPadding = PaddingValues(bottom = WooPosSpacing.Medium.value),
                withBottomShadow = true,
            ) {
                itemsIndexed(dialogState.items) { index, item ->
                    RefundDetailItem(item)
                    if (index < dialogState.items.size - 1) {
                        DividerWithSpacing()
                    }
                }
            }

            Divider()
            Spacer(Modifier.height(WooPosSpacing.Medium.value))

            TotalRowLine(
                label = dialogState.itemsSubtotalLabel,
                value = dialogState.itemsSubtotalAmount,
                boldLabel = false,
            )

            Spacer(Modifier.height(WooPosSpacing.Small.value))

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_refund_tax),
                value = dialogState.tax,
                boldLabel = false,
            )

            DividerWithSpacing()

            TotalRowLine(
                label = stringResource(R.string.woopos_orders_details_refund_total),
                value = dialogState.refundTotal,
                boldLabel = true,
            )

            dialogState.paymentMethodTitle?.let { paymentMethod ->
                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = paymentMethod,
                    style = WooPosTypography.BodyMedium,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }
        }
    }
}

@Composable
private fun RefundDetailItem(
    item: WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = WooPosSpacing.Small.value),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooPosItemImage(
            imageUrl = item.imageUrl,
            modifier = Modifier
                .size(56.dp.toAdaptiveIconSize())
                .clip(RoundedCornerShape(WooPosCornerRadius.Small.value)),
            placeholderIcon = ImageVector.vectorResource(R.drawable.ic_inventory_2_24dp),
            placeholderIconSize = WooPosIconSize.Small.value,
        )

        Spacer(Modifier.width(WooPosSpacing.Medium.value))

        Column(modifier = Modifier.weight(1f)) {
            WooPosText(
                text = item.name,
                style = WooPosTypography.BodyLarge,
                fontWeight = FontWeight.Bold,
            )
            item.attributesDescription?.let { attrs ->
                Spacer(Modifier.height(WooPosSpacing.XSmall.value))
                WooPosText(
                    text = attrs,
                    style = WooPosTypography.BodyMedium,
                    color = WooPosTheme.colors.onSurfaceVariantHighest,
                )
            }
            Spacer(Modifier.height(WooPosSpacing.XSmall.value))
            WooPosText(
                text = item.qtyAndUnitPrice,
                style = WooPosTypography.BodyMedium,
                color = WooPosTheme.colors.onSurfaceVariantHighest,
            )
        }

        WooPosText(
            text = item.lineTotal,
            style = WooPosTypography.BodyMedium,
        )
    }
}

@Composable
private fun TotalRowLine(
    label: String,
    value: String,
    boldLabel: Boolean = true,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WooPosText(
            text = label,
            style = if (boldLabel) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = if (boldLabel) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
        WooPosText(
            text = value,
            style = if (boldLabel) WooPosTypography.BodyLarge else WooPosTypography.BodyMedium,
            fontWeight = if (boldLabel) FontWeight.Bold else FontWeight.Normal,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun DividerWithSpacing() {
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
    HorizontalDivider(
        color = WooPosTheme.colors.outlineVariant,
        thickness = 0.25.dp,
    )
    Spacer(Modifier.height(WooPosSpacing.Medium.value))
}

@Composable
private fun Divider() {
    HorizontalDivider(
        color = WooPosTheme.colors.outlineVariant,
        thickness = 0.25.dp,
    )
}
