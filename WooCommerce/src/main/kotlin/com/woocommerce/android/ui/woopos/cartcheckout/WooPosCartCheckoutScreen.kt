package com.woocommerce.android.ui.woopos.cartcheckout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosCartCheckoutScreen(viewModel: WooPosCartCheckoutViewModel) {
    WooPosCartCheckoutScreen(viewModel::onUIEvent)
}

@Composable
private fun WooPosCartCheckoutScreen(onUIEvent: (WooPosCartCheckoutUIEvent) -> Unit) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.h3,
                color = MaterialTheme.colors.primary,
            )
            Button(onClick = { onUIEvent(WooPosCartCheckoutUIEvent.CheckoutClicked) }) {
                Text("To Cart")
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartCheckoutScreenPreview() {
    WooPosCartCheckoutScreen({})
}
