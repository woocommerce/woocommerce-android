package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButton
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.home.totals.payment.success.WooPosPaymentSuccessScreen

@Composable
fun WooPosTotalsScreen(modifier: Modifier = Modifier) {
    val viewModel: WooPosTotalsViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value
    WooPosTotalsScreen(modifier, state, viewModel::onUIEvent)
}

@Composable
private fun WooPosTotalsScreen(
    modifier: Modifier = Modifier,
    state: WooPosTotalsState,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        backgroundColor = MaterialTheme.colors.surface,
        elevation = 4.dp
    ) {
        when (state) {
            is WooPosTotalsState.Totals -> {
                TotalsLoaded(
                    state = state,
                    onUIEvent = onUIEvent
                )
            }

            is WooPosTotalsState.PaymentSuccess -> {
                WooPosPaymentSuccessScreen(state) { onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked) }
            }

            is WooPosTotalsState.Loading -> {
                TotalsLoading()
            }
        }
    }
}

@Composable
private fun TotalsLoaded(
    state: WooPosTotalsState.Totals,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 0.dp)
                .weight(1f)
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colors.background,
                    shape = RoundedCornerShape(16.dp),
                )
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            TotalsGrid(state)

            Spacer(modifier = Modifier.weight(1f))
        }

        WooPosButton(
            text = stringResource(R.string.woopos_payment_collect_payment_label),
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(24.dp),
            onClick = { onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked) },
        )
    }
}

@Composable
private fun TotalsGrid(state: WooPosTotalsState.Totals) {
    Column(
        modifier = Modifier
            .border(
                width = (0.5).dp,
                color = WooPosTheme.colors.border,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(24.dp)
            .width(380.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.woopos_payment_subtotal_label),
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = state.orderSubtotalText,
                style = MaterialTheme.typography.h6,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Divider(color = WooPosTheme.colors.border, thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.woopos_payment_tax_label),
                style = MaterialTheme.typography.h6,
            )
            Text(
                text = state.orderTaxText,
                style = MaterialTheme.typography.h6,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        Divider(color = WooPosTheme.colors.border, thickness = 0.5.dp)

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(R.string.woopos_payment_total_label),
                style = MaterialTheme.typography.h6,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = state.orderTotalText,
                style = MaterialTheme.typography.h2,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TotalsLoading() {
    Column(
        modifier = Modifier
            .background(
                color = MaterialTheme.colors.background,
                shape = RoundedCornerShape(16.dp),
            )
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .wrapContentSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            WooPosShimmerBox(
                modifier = Modifier
                    .height(24.dp)
                    .width(332.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(24.dp)
                    .width(332.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(24.dp))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(40.dp)
                    .width(332.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
        }
    }
}

@Composable
fun WooPosTotalsErrorScreen(
    errorMessage: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = errorMessage,
            style = MaterialTheme.typography.body2
        )

        Spacer(modifier = Modifier.height(16.dp))

        WooPosButton(
            text = stringResource(R.string.retry),
            onClick = onRetryClick,
            modifier = Modifier.padding(top = 16.dp)
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsErrorScreenPreview() {
    WooPosTheme {
        WooPosTotalsErrorScreen(
            errorMessage = "An error occurred. Please try again.",
            onRetryClick = {}
        )
    }
}


@Composable
@WooPosPreview
fun WooPosTotalsScreenPreview() {
    WooPosTheme {
        WooPosTotalsScreen(
            state = WooPosTotalsState.Totals(
                orderSubtotalText = "$420.00",
                orderTotalText = "$462.00",
                orderTaxText = "$42.00",
            ),
            onUIEvent = {}
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenLoadingPreview() {
    WooPosTheme {
        WooPosTotalsScreen(
            state = WooPosTotalsState.Loading,
            onUIEvent = {}
        )
    }
}
