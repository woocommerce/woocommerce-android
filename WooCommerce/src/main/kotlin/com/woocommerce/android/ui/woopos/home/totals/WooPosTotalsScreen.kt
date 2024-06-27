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
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.snackbar.WooPosSnackbar
import com.woocommerce.android.ui.woopos.home.totals.payment.success.WooPosPaymentSuccessScreen

@Composable
fun WooPosTotalsScreen() {
    val viewModel: WooPosTotalsViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value
    WooPosTotalsScreen(state, viewModel::onUIEvent)
}

@Composable
private fun WooPosTotalsScreen(state: WooPosTotalsState, onUIEvent: (WooPosTotalsUIEvent) -> Unit) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        when (state) {
            is WooPosTotalsState.Totals -> {
                Totals(
                    modifier = Modifier.padding(padding),
                    state = state,
                    onUIEvent = onUIEvent
                )
                WooPosSnackbar(state.snackbar, snackbarHostState, onUIEvent)
            }
            is WooPosTotalsState.PaymentSuccess -> {
                WooPosPaymentSuccessScreen { onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked) }
            }

            WooPosTotalsState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun Totals(
    modifier: Modifier = Modifier,
    state: WooPosTotalsState.Totals,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp,
        modifier = Modifier.padding(16.dp),
    ) {
        Column(
            modifier = modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Subtotal: ",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.orderSubtotalText,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Taxes: ",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.orderTaxText,
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ",
                    style = MaterialTheme.typography.h6,
                    color = MaterialTheme.colors.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = state.orderTotalText,
                    style = MaterialTheme.typography.h4,
                    color = MaterialTheme.colors.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked) },
            ) {
                Text("Collect Card Payment")
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenPreview() {
    WooPosTotalsScreen(
        state = WooPosTotalsState.Totals(
            orderSubtotalText = "$420.00",
            orderTotalText = "$462.00",
            orderTaxText = "$42.00",
        ),
        onUIEvent = {}
    )
}
