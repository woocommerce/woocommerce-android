package com.woocommerce.android.ui.orders.details.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLabelSampleData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel

@Composable
fun OrderDetailWooShippingShipmentListView(
    shipments: List<ShipmentUIModel>,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Text(
            text = stringResource(id = R.string.shipping_labels).uppercase(),
            fontWeight = FontWeight.Medium,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(16.dp)
        )
        Card(
            shape = RectangleShape
        ) {
            ShipmentList(shipments = shipments)
        }
    }
}

@Composable
private fun ShipmentList(
    shipments: List<ShipmentUIModel>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        shipments.forEach { shipment ->
            ShipmentItem(
                shipment = shipment,
                shipmentsSize = shipments.size,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ShipmentItem(
    shipment: ShipmentUIModel,
    shipmentsSize: Int,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .padding(start = 16.dp)
                .defaultMinSize(minHeight = 48.dp)
        ) {
            Text(
                text = stringResource(
                    id = R.string.orderdetail_shipping_label_shipment_header,
                    "${shipment.localId}/$shipmentsSize"
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (shipment.purchased) {
                Icon(
                    modifier = Modifier.size(16.dp),
                    painter = painterResource(R.drawable.ic_progress_circle_complete),
                    contentDescription = stringResource(R.string.purchased_shipment_content_description),
                    tint = colorResource(id = R.color.woo_green_70)
                )

                Spacer(Modifier.weight(1f))

                WCOverflowMenu(
                    items = emptyList<String>(),
                    onSelected = {}
                )
            }
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))

        if (shipment.purchased && shipment.label != null) {
            WCOutlinedButton(
                text = stringResource(R.string.orderdetail_shipping_label_item_view_purchased_shipping_label),
                onClick = { TODO() },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        } else {
            WCColoredButton(
                text = stringResource(R.string.orderdetail_shipping_label_create_shipping_label),
                onClick = { TODO() },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@LightDarkThemePreviews
@Composable
private fun OrderDetailWooShippingShipmentListViewPreview() {
    val shipments = remember {
        listOf(
            ShippingLabelSampleData.getShippingLabelUIModel(purchased = true),
            ShippingLabelSampleData.getShippingLabelUIModel(purchased = false)
        )
    }

    WooThemeWithBackground {
        OrderDetailWooShippingShipmentListView(shipments = shipments)
    }
}
