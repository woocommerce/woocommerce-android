package com.woocommerce.android.ui.woopos.cart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.util.WooPosPreview

@Composable
@Suppress("UNUSED_PARAMETER")
fun WooPosCartScreen(
    viewModel: WooPosCartViewModel,
    onCheckoutClick: () -> Unit,
    onConnectToCardReaderClicked: () -> Unit
) {
    WooPosCartScreen(
        onCheckoutClick = onCheckoutClick,
        onConnectToCardReaderClicked = onConnectToCardReaderClicked,
    )
}

@Composable
private fun WooPosCartScreen(
    onCheckoutClick: () -> Unit,
    onConnectToCardReaderClicked: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
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

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = onConnectToCardReaderClicked) {
                Text("Connect to Card Reader")
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosCartScreen(onCheckoutClick = {}, onConnectToCardReaderClicked = {})
}
