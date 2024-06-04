package com.woocommerce.android.ui.woopos.cartcheckout.totals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun WooPosTotalsScreen(
    viewModel: WooPosTotalsViewModel,
    onUIEvent: (WooPosCartCheckoutUIEvent) -> Unit
) {
    WooPosTotalsScreen(onUIEvent)
}

@Composable
@Suppress("UNUSED_PARAMETER")
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
                    text = "Totals",
                    style = MaterialTheme.typography.h3,
                    color = MaterialTheme.colors.primary,
                )
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosTotalsScreen {}
}
