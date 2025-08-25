package com.woocommerce.android.ui.orders.details.views

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.ProductThumbnail
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCOverflowMenu
import com.woocommerce.android.ui.compose.preview.LightDarkThemePreviews
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.OrderShipmentTrackingHelper
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippingLabelSampleData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.util.StringUtils.getQuantityString

@Composable
fun OrderDetailWooShippingShipmentListView(
    shipments: List<ShipmentUIModel>,
    onCreateShippingLabelClicked: (shipmentId: Int?) -> Unit,
    onViewShippingLabelClicked: (ShippingLabelModel) -> Unit,
    onRequestRefundClicked: (labelId: Long) -> Unit,
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
            shape = RectangleShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
            ShipmentList(
                shipments = shipments,
                onCreateShippingLabelClicked = onCreateShippingLabelClicked,
                onViewShippingLabelClicked = onViewShippingLabelClicked,
                onRequestRefundClicked = onRequestRefundClicked,
            )
        }
    }
}

@Composable
private fun ShipmentList(
    shipments: List<ShipmentUIModel>,
    onCreateShippingLabelClicked: (shipmentId: Int?) -> Unit,
    onViewShippingLabelClicked: (ShippingLabelModel) -> Unit,
    onRequestRefundClicked: (labelId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        shipments.forEachIndexed { index, shipment ->
            ShipmentItem(
                shipment = shipment,
                index = index,
                shipmentsSize = shipments.size,
                onCreateShippingLabelClicked = onCreateShippingLabelClicked,
                onViewShippingLabelClicked = onViewShippingLabelClicked,
                onRequestRefundClicked = onRequestRefundClicked,
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun ShipmentItem(
    shipment: ShipmentUIModel,
    index: Int,
    shipmentsSize: Int,
    onCreateShippingLabelClicked: (shipmentId: Int?) -> Unit,
    onViewShippingLabelClicked: (ShippingLabelModel) -> Unit,
    onRequestRefundClicked: (labelId: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 64.dp)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = stringResource(
                        id = R.string.orderdetail_shipping_label_shipment_header,
                        "${index + 1}/$shipmentsSize"
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                if (shipment.label?.isPurchased == true) {
                    Icon(
                        modifier = Modifier.size(16.dp),
                        painter = painterResource(R.drawable.ic_progress_circle_complete),
                        contentDescription = stringResource(R.string.purchased_shipment_content_description),
                        tint = colorResource(id = R.color.woo_green_70)
                    )

                    Spacer(Modifier.weight(1f))

                    WCOverflowMenu(
                        items = listOf(stringResource(R.string.orderdetail_shipping_label_request_refund)),
                        onSelected = { onRequestRefundClicked(shipment.label.labelId) },
                    )
                }
            }

            if (shipment.label?.refund != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.orderdetail_shipping_label_refunded),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(horizontal = 16.dp)
                        .background(
                            color = colorResource(R.color.woo_blue_5).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(16.dp)
                )
                Spacer(Modifier.height(16.dp))
            }
        }
        HorizontalDivider(Modifier.padding(start = 16.dp))

        shipment.items.takeIf { it.isNotEmpty() }?.let {
            ShipmentItemsRow(
                items = it,
                modifier = Modifier.fillMaxWidth()
            )
            HorizontalDivider(Modifier.padding(start = 16.dp))
        }

        if (shipment.label?.isPurchased == true) {
            LabelTrackingRow(shipment.label)

            HorizontalDivider(Modifier.padding(start = 16.dp))

            ViewLabelRow(
                onClick = { onViewShippingLabelClicked(shipment.label) },
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            WCColoredButton(
                text = stringResource(R.string.orderdetail_shipping_label_create_shipping_label),
                onClick = { onCreateShippingLabelClicked(shipment.localId.toIntOrNull()) },
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ViewLabelRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = stringResource(R.string.orderdetail_shipping_label_item_view_purchased_shipping_label))

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null
        )
    }
}

@Composable
private fun ShipmentItemsRow(
    items: List<ShippableItemModel>,
    modifier: Modifier = Modifier
) {
    var isDialogShown by rememberSaveable { mutableStateOf(false) }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clickable(onClick = { isDialogShown = true })
            .padding(16.dp)
    ) {
        Text(
            text = getQuantityString(
                quantity = items.size,
                default = R.string.orderdetail_shipping_label_shipment_items_multiple,
                one = R.string.orderdetail_shipping_label_shipment_items_one,
            )
        )

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null
        )
    }

    if (isDialogShown) {
        Dialog(onDismissRequest = { isDialogShown = false }) {
            Card {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = getQuantityString(
                            quantity = items.size,
                            default = R.string.orderdetail_shipping_label_shipment_items_multiple,
                            one = R.string.orderdetail_shipping_label_shipment_items_one,
                        ),
                        style = MaterialTheme.typography.titleLarge,
                    )

                    Spacer(Modifier.height(24.dp))

                    items.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .padding(vertical = 8.dp)
                                .fillMaxWidth()
                        ) {
                            ProductThumbnail(
                                imageUrl = item.imageUrl.orEmpty(),
                                contentDescription = stringResource(R.string.product_image_content_description)
                            )
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Spacer(Modifier.weight(1f))

                            Text(
                                text = item.quantity.toInt().toString(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    WCOutlinedButton(
                        text = stringResource(R.string.done),
                        onClick = { isDialogShown = false },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun LabelTrackingRow(
    label: ShippingLabelModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(vertical = 8.dp)
            .padding(start = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.order_shipment_tracking_number),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = label.tracking,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Light
            )
        }
        Spacer(Modifier.weight(1f))
        WCOverflowMenu(
            items = if (label.trackingLink.isNotEmpty()) TrackingMenuItem.entries else listOf(TrackingMenuItem.COPY),
            onSelected = {
                when (it) {
                    TrackingMenuItem.COPY -> OrderShipmentTrackingHelper.copyTrackingNumber(context, label.tracking)
                    TrackingMenuItem.TRACK -> OrderShipmentTrackingHelper.trackShipment(context, label.trackingLink)
                }
            },
            mapper = { stringResource(it.title) }
        )
    }
}

private enum class TrackingMenuItem(@StringRes val title: Int) {
    COPY(R.string.orderdetail_copy_tracking_number),
    TRACK(R.string.orderdetail_track_shipment)
}

@LightDarkThemePreviews
@Composable
private fun OrderDetailWooShippingShipmentListViewPreview() {
    val shipments = remember {
        listOf(
            ShippingLabelSampleData.getShippingLabelUIModel(purchased = true),
            ShippingLabelSampleData.getShippingLabelUIModel(purchased = false),
            ShippingLabelSampleData.getShippingLabelUIModel(purchased = true).let {
                it.copy(label = it.label!!.copy(refund = ShippingLabelModel.Refund(null, null)))
            }
        )
    }

    WooThemeWithBackground {
        OrderDetailWooShippingShipmentListView(
            shipments = shipments,
            onCreateShippingLabelClicked = {},
            onViewShippingLabelClicked = {},
            onRequestRefundClicked = {}
        )
    }
}
