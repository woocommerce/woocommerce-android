package com.woocommerce.android.ui.woopos.cartcheckout.cart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.cartcheckout.WooPosCartCheckoutUIEvent
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
@Suppress("UNUSED_PARAMETER")
fun WooPosCartScreen(
    viewModel: WooPosCartViewModel,
    onUIEvent: (WooPosCartCheckoutUIEvent) -> Unit
) {
    WooPosTotalsScreen(onUIEvent)
}

@Composable
private fun WooPosTotalsScreen(onUIEvent: (WooPosCartCheckoutUIEvent) -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column {
                Text(
                    text = "Cart",
                    style = MaterialTheme.typography.h3,
                    color = MaterialTheme.colors.primary,
                )
                Button(onClick = { onUIEvent(WooPosCartCheckoutUIEvent.CheckoutClicked) }) {
                    Text("Checkout")
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosTotalsScreen({})
}
