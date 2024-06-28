package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
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
                TotalsLoaded(
                    modifier = Modifier.padding(padding),
                    state = state,
                    onUIEvent = onUIEvent
                )
                WooPosSnackbar(state.snackbar, snackbarHostState, onUIEvent)
            }

            is WooPosTotalsState.PaymentSuccess -> {
                WooPosPaymentSuccessScreen(state) { onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked) }
            }

            is WooPosTotalsState.Loading -> {
                TotalsLoading(
                    modifier = Modifier.padding(padding),
                )
            }
        }
    }
}

@Composable
private fun TotalsLoaded(
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
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .background(Color.Transparent),
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, Color(0xFFBDBDBD))
            ) {
                Column(
                    modifier = modifier
                        .widthIn(max = 420.dp)
                        .wrapContentHeight()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.woopos_payment_subtotal_label),
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
                            text = stringResource(R.string.woopos_payment_tax_label),
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

                    Column(
                        modifier = modifier
                            .wrapContentWidth()
                            .wrapContentHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = stringResource(R.string.woopos_payment_total_label),
                            style = MaterialTheme.typography.h6,
                            color = MaterialTheme.colors.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = state.orderTotalText,
                            style = MaterialTheme.typography.h4,
                            color = MaterialTheme.colors.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            WooPosButton(
                text = stringResource(R.string.woopos_payment_collect_payment_label),
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(16.dp),
                onClick = { onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked) },
            )
        }
    }
}

@Composable
private fun TotalsLoading(
    modifier: Modifier = Modifier,
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
            Spacer(modifier = Modifier.weight(1f))

            Surface(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .wrapContentWidth()
                    .wrapContentHeight()
                    .background(Color.Transparent),
            ) {
                Column(
                    modifier = modifier
                        .widthIn(max = 420.dp)
                        .wrapContentHeight()
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    WooPosShimmerBox(
                        modifier = Modifier
                            .height(30.dp)
                            .width(300.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    WooPosShimmerBox(
                        modifier = Modifier
                            .height(30.dp)
                            .width(300.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    WooPosShimmerBox(
                        modifier = Modifier
                            .height(60.dp)
                            .width(300.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
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

@Composable
@WooPosPreview
fun WooPosTotalsScreenLoadingPreview() {
    WooPosTotalsScreen(
        state = WooPosTotalsState.Loading,
        onUIEvent = {}
    )
}
