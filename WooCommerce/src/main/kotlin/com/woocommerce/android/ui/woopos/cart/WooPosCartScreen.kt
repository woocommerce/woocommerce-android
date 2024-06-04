package com.woocommerce.android.ui.woopos.cart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.products.ProductSelectorViewModel

@Composable
@Suppress("UNUSED_PARAMETER")
fun WooPosCartScreen(
    viewModel: WooPosCartViewModel,
    productSelectorViewModel: ProductSelectorViewModel,
    onCheckoutClick: () -> Unit,
) {
    WooPosCartScreen(onCheckoutClick = onCheckoutClick)
}

@Composable
private fun WooPosCartScreen(onCheckoutClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column {
            Text(
                text = "Cart",
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.primary,
            )
            Button(onClick = onCheckoutClick) {
                Text("Checkout")
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosCartScreen(onCheckoutClick = {})
}
