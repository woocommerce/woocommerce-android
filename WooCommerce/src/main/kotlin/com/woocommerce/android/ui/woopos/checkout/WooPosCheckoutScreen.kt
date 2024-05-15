package com.woocommerce.android.ui.woopos.checkout

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
fun WooPosCheckoutScreen(viewModel: WooPosCheckoutViewModel, onBackClick: () -> Unit) {
    WooPosCheckoutScreen(
        onBackClick = onBackClick
    )
}

@Composable
private fun WooPosCheckoutScreen(onBackClick: () -> Unit) {
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
            Button(onClick = onBackClick) {
                Text("To Cart")
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCheckoutScreenPreview() {
    WooPosCheckoutScreen(onBackClick = {})
}
