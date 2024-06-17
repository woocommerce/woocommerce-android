package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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

    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.padding(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (totalsState.value.orderId != null) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Subtotal: ",
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = totalsState.value.orderSubtotal.toPlainString(),
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Taxes: ",
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = totalsState.value.orderTax.toPlainString(),
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                    }
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total: ",
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                        Text(
                            text = totalsState.value.orderTotal.toPlainString(),
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { onCollectPaymentClick() },
                    enabled = totalsState.value.isCollectPaymentButtonEnabled,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
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
            isCollectPaymentButtonEnabled = true
        )
    )

    WooPosTotalsScreen(
        state = totalsState,
        onCollectPaymentClick = {}
    )
}
