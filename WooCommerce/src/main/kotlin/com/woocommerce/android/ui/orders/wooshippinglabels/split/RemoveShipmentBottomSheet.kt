package com.woocommerce.android.ui.orders.wooshippinglabels.split

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonDefaults.OutlinedBorderOpacity
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.component.WCColoredButton
import com.woocommerce.android.ui.compose.component.WCModalBottomSheet
import com.woocommerce.android.ui.compose.component.WCOutlinedButton
import com.woocommerce.android.ui.compose.component.WCRemoveButton
import com.woocommerce.android.util.StringUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoveShipmentBottomSheet(
    removeShipmentSheet: RemoveShipmentSheet,
    onDismissRemoveSheet: () -> Unit,
    onRemoveShipments: (List<Int>, Int?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedShipmentKeyState by rememberSaveable {
        mutableStateOf<Int?>(removeShipmentSheet.otherShipments.keys.firstOrNull())
    }

    val removingSingleShipment = removeShipmentSheet.removingShipments.size == 1
    val multipleOtherShipments = removeShipmentSheet.otherShipments.size > 1

    fun getDescription(context: Context) = if (!removingSingleShipment) {
        // Merging all shipments
        context.getString(R.string.woo_shipping_split_shipment_merge_unfulfilled_desc)
    } else if (multipleOtherShipments) {
        StringUtils.getQuantityString(
            context = context,
            quantity = removeShipmentSheet.removingShipments.values.first().totalItemQuantity,
            default = R.string.woo_shipping_split_shipment_bottom_sheet_desc_multiple_shipment_plural,
            one = R.string.woo_shipping_split_shipment_bottom_sheet_desc_multiple_shipment_one,
        )
    } else {
        context.getString(
            R.string.woo_shipping_split_shipment_bottom_sheet_desc,
            context.getString(
                R.string.woo_shipping_split_shipment_shipment_name,
                (removeShipmentSheet.otherShipments.keys.first() + 1).toString()
            ).toLowerCase(Locale.current)
        )
    }

    fun getRemoveButtonText(resources: Resources) = if (!removingSingleShipment) {
        resources.getString(R.string.woo_shipping_split_shipment_merge_unfulfilled_button)
    } else if (multipleOtherShipments) {
        resources.getString(
            R.string.woo_shipping_split_shipment_shipment_remove,
            resources.getString(
                R.string.woo_shipping_split_shipment_shipment_name,
                (removeShipmentSheet.removingShipments.keys.first() + 1).toString()
            )
        )
    } else {
        resources.getString(R.string.woo_shipping_split_shipment_bottom_sheet_remove_button)
    }

    WCModalBottomSheet(sheetState = sheetState, onDismissRequest = onDismissRemoveSheet) {
        Column(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 19.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(
                    if (removingSingleShipment) {
                        R.string.woo_shipping_split_shipment_bottom_sheet_title
                    } else {
                        R.string.woo_shipping_split_shipment_merge_unfulfilled_title
                    }
                ),
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                text = getDescription(LocalContext.current),
                style = MaterialTheme.typography.subtitle1,
                color = MaterialTheme.colors.onSurface
            )
            if (multipleOtherShipments) {
                removeShipmentSheet.otherShipments.forEach {
                    ShipmentCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .clickable { selectedShipmentKeyState = it.key },
                        shipment = it,
                        isSelected = it.key == selectedShipmentKeyState,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            if (removingSingleShipment) {
                WCRemoveButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = getRemoveButtonText(LocalContext.current.resources),
                    onClick = {
                        onRemoveShipments(removeShipmentSheet.removingShipments.keys.toList(), selectedShipmentKeyState)
                    }
                )
            } else {
                WCColoredButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = getRemoveButtonText(LocalContext.current.resources),
                    onClick = {
                        onRemoveShipments(removeShipmentSheet.removingShipments.keys.toList(), selectedShipmentKeyState)
                    }
                )
            }
            WCOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.cancel),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colors.onSurface),
                onClick = onDismissRemoveSheet
            )
        }
    }
}

@Composable
private fun ShipmentCard(
    modifier: Modifier,
    shipment: Map.Entry<Int, SelectableShippableItemsUI>,
    isSelected: Boolean = false
) {
    val borderWidth = if (isSelected) 2.dp else 0.5.dp
    val borderColor = if (isSelected) {
        MaterialTheme.colors.primary
    } else {
        MaterialTheme.colors.onSurface.copy(alpha = OutlinedBorderOpacity)
    }
    val containerColor = if (isSelected) R.color.color_item_selected else R.color.color_surface

    val items = StringUtils.getQuantityString(
        context = LocalContext.current,
        quantity = shipment.value.totalItemQuantity,
        default = R.string.shipping_label_package_details_items_count_many,
        one = R.string.shipping_label_package_details_items_count_one,
    )

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = colorResource(containerColor)),
        border = BorderStroke(borderWidth, borderColor),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_arrow_return),
                contentDescription = null,
                tint = if (isSelected) borderColor else Color.Unspecified
            )
            Spacer(Modifier.padding(horizontal = 16.dp))
            Column {
                Text(
                    text = stringResource(
                        R.string.woo_shipping_split_shipment_shipment_name,
                        shipment.key + 1
                    ),
                    style = MaterialTheme.typography.subtitle1,
                    color = MaterialTheme.colors.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Absolute.SpaceBetween
                ) {
                    Text(
                        text = items,
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.shipping_label_package_details_items_weight_price,
                            shipment.value.formattedTotalWeight,
                            shipment.value.formattedTotalPrice
                        ),
                        style = MaterialTheme.typography.subtitle2,
                        color = MaterialTheme.colors.onSurface
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun ShipmentCardPreview() = ShipmentCard(
    modifier = Modifier.fillMaxWidth(),
    shipment = mapOf(
        3 to SelectableShippableItemsUI(
            shippableItems = emptyList(),
            formattedTotalWeight = "550g",
            formattedTotalPrice = "$50.00",
            purchased = false
        )
    ).entries.first()
)

@Preview
@Composable
private fun ShipmentCardSelectedPreview() = ShipmentCard(
    modifier = Modifier.fillMaxWidth(),
    shipment = mapOf(
        2 to SelectableShippableItemsUI(
            shippableItems = emptyList(),
            formattedTotalWeight = "825g",
            formattedTotalPrice = "$75.00",
            purchased = false
        )
    ).entries.first(),
    isSelected = true
)
