package com.woocommerce.android.ui.orders.wooshippinglabels.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ScrollableTabRow
import androidx.compose.material.Tab
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.woocommerce.android.R
import kotlinx.coroutines.launch

@Composable
fun ShipmentsTabRow(
    shipmentTabs: List<ShipmentTabData>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    ScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        backgroundColor = MaterialTheme.colors.surface,
        contentColor = MaterialTheme.colors.primary,
        divider = {},
        edgePadding = 0.dp,
        modifier = modifier
    ) {
        shipmentTabs.forEachIndexed { index, tabData ->
            val textColor = if (index == selectedTabIndex) {
                MaterialTheme.colors.primary
            } else {
                colorResource(id = R.color.color_on_surface_medium)
            }
            Tab(
                text = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(
                                R.string.woo_shipping_split_shipment_shipment_name,
                                tabData.shipmentIndex // Use 1-based indexing for the shipments
                            ),
                            color = textColor,
                            style = MaterialTheme.typography.subtitle1,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (tabData.isPurchased) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                painter = painterResource(R.drawable.ic_progress_circle_complete),
                                contentDescription = stringResource(R.string.purchased_shipment_content_description),
                                tint = colorResource(id = R.color.woo_green_70)
                            )
                        }
                    }
                },
                selected = selectedTabIndex == index,
                onClick = { scope.launch { onTabSelected(index) } }
            )
        }
    }
}

data class ShipmentTabData(val shipmentIndex: Int, val isPurchased: Boolean)
