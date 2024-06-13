package com.woocommerce.android.ui.woopos.home.totals

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
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosTotalsScreen() {
    val viewModel: WooPosTotalsViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState()
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

                if (state.value.orderId != null) {
                    Text(
                        text = "Subtotal: ${state.value.orderTotals.toPlainString()}",
                        style = MaterialTheme.typography.h4,
                        color = MaterialTheme.colors.primary,
                    )

                    Text(
                        text = "Taxes: " + "$0.00",
                        style = MaterialTheme.typography.h4,
                        color = MaterialTheme.colors.primary,
                    )

                    Text(
                        text = "Total: ${state.value.orderTotals.toPlainString()}",
                        style = MaterialTheme.typography.h4,
                        color = MaterialTheme.colors.primary,
                    )
                }

                Button(
                    onClick = { viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked) },
                    enabled = state.value.isCollectPaymentButtonEnabled
                ) {
                    Text("Collect Card Payment")
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosTotalsScreen()
}
