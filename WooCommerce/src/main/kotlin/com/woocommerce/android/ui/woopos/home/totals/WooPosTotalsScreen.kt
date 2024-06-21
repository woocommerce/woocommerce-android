package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.SnackbarResult
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview

@Composable
fun WooPosTotalsScreen(viewModelStoreOwner: ViewModelStoreOwner) {
    val viewModel: WooPosTotalsViewModel = hiltViewModel(viewModelStoreOwner)
    val state = viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { padding ->
        Card(
            shape = RoundedCornerShape(16.dp),
            backgroundColor = MaterialTheme.colors.surface,
            elevation = 4.dp,
            modifier = Modifier.padding(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Totals",
                    style = MaterialTheme.typography.h3,
                    color = MaterialTheme.colors.primary,
                )

                if (state.value.orderId != null) {
                    Text(
                        text = "Order ID: ${state.value.orderId}",
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
    SnackbarHandler(state, snackbarHostState, viewModel)
}

@Composable
private fun SnackbarHandler(
    state: State<WooPosTotalsState>,
    snackbarHostState: SnackbarHostState,
    viewModel: WooPosTotalsViewModel
) {
    val snackbarState = state.value.snackbarMessage
    if (snackbarState is SnackbarMessage.Triggered) {
        val message = stringResource(snackbarState.message)
        LaunchedEffect(state.value.snackbarMessage) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long,
            )
            when (result) {
                SnackbarResult.Dismissed -> {
                    viewModel.onUIEvent(WooPosTotalsUIEvent.SnackbarDismissed)
                }

                SnackbarResult.ActionPerformed -> Unit
            }
        }
    }
}

@Composable
@WooPosPreview
fun WooPosCartScreenPreview() {
    WooPosTotalsScreen(LocalViewModelStoreOwner.current!!)
}
