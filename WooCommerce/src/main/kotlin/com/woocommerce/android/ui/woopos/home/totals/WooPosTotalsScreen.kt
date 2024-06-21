package com.woocommerce.android.ui.woopos.home.totals

import android.annotation.SuppressLint
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
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
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun WooPosTotalsScreen(viewModelStoreOwner: ViewModelStoreOwner) {
    val viewModel: WooPosTotalsViewModel = hiltViewModel(viewModelStoreOwner)
    val state = viewModel.state

    WooPosTotalsScreen(
        state = state,
        onCollectPaymentClick = { viewModel.onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked) },
        onSnackbarDismissed = {viewModel.onUIEvent(WooPosTotalsUIEvent.SnackbarDismissed)}
    )
}

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun WooPosTotalsScreen(
    state: StateFlow<WooPosTotalsState>,
    onCollectPaymentClick: () -> Unit,
    onSnackbarDismissed: () -> Unit
) {
    val totalsState = state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) {
        if (totalsState.value.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

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
                Card(
                    shape = RoundedCornerShape(8.dp),
                    backgroundColor = MaterialTheme.colors.surface,
                    elevation = 4.dp,
                    modifier = Modifier
                        .padding(16.dp)
                        .wrapContentSize()
                        .border(
                            width = dimensionResource(id = R.dimen.minor_10),
                            color = colorResource(id = R.color.woo_gray_5),
                            shape = RoundedCornerShape(8.dp)

                        )
                        .widthIn(min = 128.dp, max = 256.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (totalsState.value.orderId != null) {
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
                                    text = totalsState.value.orderSubtotalText,
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
                                    text = totalsState.value.orderTaxText,
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
                                    text = totalsState.value.orderTotalText,
                                    style = MaterialTheme.typography.h4,
                                    color = MaterialTheme.colors.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Divider()

                        Spacer(modifier = Modifier.height(16.dp))

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

        SnackbarHandler(totalsState, snackbarHostState, onSnackbarDismissed)
        }
}


@Composable
private fun SnackbarHandler(
    state: State<WooPosTotalsState>,
    snackbarHostState: SnackbarHostState,
    onSnacbarDismissed: ()->Unit,
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
                    onSnacbarDismissed()
                }

                SnackbarResult.ActionPerformed -> Unit
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
            orderSubtotalText = "$420.00",
            orderTotalText = "$462.00",
            orderTaxText = "$42.00",
            isCollectPaymentButtonEnabled = true,
            isLoading = false
        )
    )

    WooPosTotalsScreen(
        state = totalsState,
        onCollectPaymentClick = {},
        onSnackbarDismissed = {},
    )
}
