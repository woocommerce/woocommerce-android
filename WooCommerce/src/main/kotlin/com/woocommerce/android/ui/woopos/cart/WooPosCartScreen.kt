package com.woocommerce.android.ui.woopos.cart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.woocommerce.android.ui.woopos.util.WooPosPreview

@Composable
@Suppress("UNUSED_PARAMETER")
fun WooPosCartScreen(
    viewModel: WooPosCartViewModel,
    onCheckoutClick: () -> Unit,
) {
    WooPosCartScreen(
        onButtonClicked = onCheckoutClick
    )
}

@Composable
private fun WooPosCartScreen(onButtonClicked: () -> Unit) {
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
            Button(onClick = onButtonClicked) {
                Text("Checkout")
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosCartScreen(onButtonClicked = {})
}
