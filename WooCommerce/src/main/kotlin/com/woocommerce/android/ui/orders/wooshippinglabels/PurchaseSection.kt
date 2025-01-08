package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCSwitch
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

@Composable
internal fun PurchasesSection(
    total: String?,
    markOrderComplete: Boolean,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    onPurchaseShippingLabel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        MarkComplete(
            markOrderComplete = markOrderComplete,
            onMarkOrderCompleteChange = onMarkOrderCompleteChange
        )
        PurchaseButton(total, onPurchaseShippingLabel)
    }
}

@Composable
internal fun PurchasesSectionLandscape(
    total: String?,
    markOrderComplete: Boolean,
    onMarkOrderCompleteChange: (Boolean) -> Unit,
    onPurchaseShippingLabel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MarkComplete(
                markOrderComplete = markOrderComplete,
                onMarkOrderCompleteChange = onMarkOrderCompleteChange,
                modifier = Modifier.weight(1f)
            )
            PurchaseButton(
                total = total,
                onPurchaseShippingLabel = onPurchaseShippingLabel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Preview(widthDp = 750, heightDp = 200)
@Composable
fun PurchasesSectionLandscapePreview() {
    WooThemeWithBackground {
        PurchasesSectionLandscape(
            total = "$12.00",
            markOrderComplete = true,
            onMarkOrderCompleteChange = {},
            onPurchaseShippingLabel = {},
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
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
        modifier = modifier
            .clickable { onMarkOrderCompleteChange(!markOrderComplete) }
            .fillMaxWidth()
            .padding(dimensionResource(R.dimen.major_100)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.shipping_label_shipment_details_mark_order_complete),
            Modifier.weight(1f)
        )
        WCSwitch(
            checked = markOrderComplete,
            onCheckedChange = onMarkOrderCompleteChange,
            modifier = Modifier
                .padding(end = dimensionResource(R.dimen.minor_100))
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
            .padding(
                top = dimensionResource(R.dimen.minor_100),
                bottom = dimensionResource(R.dimen.major_100),
                start = dimensionResource(R.dimen.major_100),
                end = dimensionResource(R.dimen.major_100)
            )
    )
}

@Preview
@Composable
internal fun PurchasesSectionPreview() {
    WooThemeWithBackground {
        PurchasesSection(
            total = null,
            markOrderComplete = true,
            onMarkOrderCompleteChange = {},
            onPurchaseShippingLabel = {},
            modifier = Modifier.padding(dimensionResource(R.dimen.major_100))
        )
    }
}
