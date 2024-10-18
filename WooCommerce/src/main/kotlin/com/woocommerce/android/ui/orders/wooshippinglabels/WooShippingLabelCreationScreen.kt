package com.woocommerce.android.ui.orders.wooshippinglabels

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun WooShippingLabelCreationScreen(
    modifier: Modifier = Modifier
) {
    Column {
        val isExpanded = remember { mutableStateOf(false) }
        ShippingProductsCard(
            shippableItems = ShippableItems(
                shippableItems = generateItems(6),
                totalWeight = "8.5kg",
                totalPrice = "$92.78"
            ),
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            isExpanded = isExpanded.value,
            onExpand = { isExpanded.value = it }
        )
    }
}

data class ShippableItem(
    val productId: Long,
    val title: String,
    val description: String,
    val weight: String,
    val price: String,
    val quantity: Int,
    val imageUrl: String? = null
)

data class ShippableItems(
    val shippableItems: List<ShippableItem>,
    val totalWeight: String,
    val totalPrice: String
)
