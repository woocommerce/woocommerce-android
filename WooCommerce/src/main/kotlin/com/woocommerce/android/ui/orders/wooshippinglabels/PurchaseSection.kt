package com.woocommerce.android.ui.orders.wooshippinglabels

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
fun PurchaseSection(
    state: PurchaseSectionUI,
    orderCompleteToggleVisible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!state.isVisible) return
    if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
        PurchasesSectionLandscape(
            state = state,
            orderCompleteToggleVisible = orderCompleteToggleVisible,
            modifier = modifier
        )
    } else {
        PurchasesSectionPortrait(
            state = state,
            orderCompleteToggleVisible = orderCompleteToggleVisible,
            modifier = modifier
        )
    }
}

@Composable
private fun PurchasesSectionPortrait(
    state: PurchaseSectionUI,
    orderCompleteToggleVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        AnimatedVisibility(orderCompleteToggleVisible) {
            MarkComplete(
                markOrderComplete = state.markOrderComplete,
                onMarkOrderCompleteChange = state.onMarkOrderCompleteChange
            )
        }

        PurchaseButton(
            total = state.formattedPrice,
            onPurchaseShippingLabel = state.onPurchaseShippingLabel
        )
    }
}

@Composable
private fun PurchasesSectionLandscape(
    state: PurchaseSectionUI,
    orderCompleteToggleVisible: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
    ) {
        AnimatedVisibility(visible = orderCompleteToggleVisible, modifier = Modifier.weight(1f)) {
            MarkComplete(
                markOrderComplete = state.markOrderComplete,
                onMarkOrderCompleteChange = state.onMarkOrderCompleteChange,
            )
        }
        PurchaseButton(
            total = state.formattedPrice,
            onPurchaseShippingLabel = state.onPurchaseShippingLabel,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
internal fun MarkComplete(
    markOrderComplete: Boolean,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable { onMarkOrderCompleteChange(!markOrderComplete) }
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.shipping_label_shipment_details_mark_order_complete),
            Modifier.weight(1f)
        )
        WCSwitch(
            checked = markOrderComplete,
            onCheckedChange = onMarkOrderCompleteChange
        )
    }
}

@Composable
internal fun PurchaseButton(
    total: String?,
    onPurchaseShippingLabel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonText = total?.let {
        stringResource(id = R.string.shipping_label_shipment_details_purchase_label, it)
    } ?: stringResource(id = R.string.shipping_label_shipment_details_purchase_label_disabled)
    WCColoredButton(
        onClick = { onPurchaseShippingLabel() },
        enabled = total != null,
        text = buttonText,
        modifier = modifier
            .fillMaxWidth()
    )
}

@Preview
@Composable
internal fun PurchasesSectionPortraitPreview() {
    WooThemeWithBackground {
        PurchasesSectionPortrait(
            state = ShippingLabelSampleData.getPurchaseSection(),
            orderCompleteToggleVisible = true,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}

@Preview(widthDp = 750, heightDp = 120)
@Composable
fun PurchasesSectionLandscapePreview() {
    WooThemeWithBackground {
        PurchasesSectionLandscape(
            state = ShippingLabelSampleData.getPurchaseSection(),
            orderCompleteToggleVisible = true,
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}
