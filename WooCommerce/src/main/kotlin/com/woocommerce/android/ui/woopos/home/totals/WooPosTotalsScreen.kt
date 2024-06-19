package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.math.BigDecimal

@Composable
fun WooPosTotalsScreen() {
    val viewModel: WooPosTotalsViewModel = hiltViewModel()
    val state = viewModel.state

    WooPosTotalsScreen(
        state = state,
        onCollectPaymentClick = { viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked) }
    )
}

@Composable
fun WooPosTotalsScreen(
    state: StateFlow<WooPosTotalsState>,
    onCollectPaymentClick: () -> Unit
) {
    val totalsState = state.collectAsState()

    if (totalsState.value.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

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
                if (totalsState.value.orderId != null) {
                    Text(
                        text = "Subtotal: ${totalsState.value.orderSubtotal.toPlainString()}",
                        style = MaterialTheme.typography.h4,
                        color = MaterialTheme.colors.primary,
                    )

                    Text(
                        text = "Taxes: ${totalsState.value.orderTax.toPlainString()}",
                        style = MaterialTheme.typography.h4,
                        color = MaterialTheme.colors.primary,
                    )

                    Text(
                        text = "Total: ${totalsState.value.orderTotal.toPlainString()}",
                        style = MaterialTheme.typography.h4,
                        color = MaterialTheme.colors.primary,
                    )
                }

                Button(
                    onClick = { onCollectPaymentClick() },
                    enabled = totalsState.value.isCollectPaymentButtonEnabled,
                ) {
                    Text("Collect Card Payment")
                }
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenPreview() {
    val totalsState = MutableStateFlow(
        WooPosTotalsState(
            orderId = 1234L,
            orderSubtotal = BigDecimal(100.00),
            orderTotal = BigDecimal(113.00),
            orderTax = BigDecimal(13.00),
            isCollectPaymentButtonEnabled = true,
            isLoading = false
        )
    )

    WooPosTotalsScreen(
        state = totalsState,
        onCollectPaymentClick = {}
    )
}
