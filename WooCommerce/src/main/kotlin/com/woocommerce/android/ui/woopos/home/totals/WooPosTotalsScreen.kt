package com.woocommerce.android.ui.woopos.home.totals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.cardreader.IppPaymentStateObserver
import com.woocommerce.android.ui.woopos.common.composeui.WooPosPreview
import com.woocommerce.android.ui.woopos.common.composeui.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.component.Button
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosButtonLarge
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosErrorScreen
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosShimmerBox
import com.woocommerce.android.ui.woopos.common.composeui.toAdaptivePadding
import com.woocommerce.android.ui.woopos.home.totals.payment.success.WooPosPaymentSuccessScreen
import kotlinx.coroutines.delay

@Composable
fun WooPosTotalsScreen(modifier: Modifier = Modifier) {
    val viewModel: WooPosTotalsViewModel = hiltViewModel()
    val state = viewModel.state.collectAsState().value
    val paymentFlowState: IppPaymentStateObserver.PaymentFlowState = viewModel.paymentState.collectAsState().value
    WooPosTotalsScreen(modifier, state, paymentFlowState, viewModel::onUIEvent)
}

@Composable
private fun WooPosTotalsScreen(
    modifier: Modifier = Modifier,
    state: WooPosTotalsViewState,
    paymentFlowState: IppPaymentStateObserver.PaymentFlowState,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    Box(modifier = modifier) {
        StateChangeAnimated(visible = state is WooPosTotalsViewState.Totals) {
            if (state is WooPosTotalsViewState.Totals) {
                TotalsLoaded(state = state, paymentFlowState = paymentFlowState, onUIEvent = onUIEvent)
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.PaymentSuccess) {
            if (state is WooPosTotalsViewState.PaymentSuccess) {
                WooPosPaymentSuccessScreen(state) { onUIEvent(WooPosTotalsUIEvent.OnNewTransactionClicked) }
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.Loading) {
            if (state is WooPosTotalsViewState.Loading) {
                TotalsLoading()
            }
        }

        StateChangeAnimated(visible = state is WooPosTotalsViewState.Error) {
            if (state is WooPosTotalsViewState.Error) {
                TotalsErrorScreen(
                    errorMessage = state.message,
                    onUIEvent = onUIEvent
                )
            }
        }
    }
}

@Composable
private fun StateChangeAnimated(
    visible: Boolean,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        content = content
    )
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun TotalsLoaded(
    state: WooPosTotalsViewState.Totals,
    paymentFlowState: IppPaymentStateObserver.PaymentFlowState,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    var isButtonVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(300)
        isButtonVisible = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp.toAdaptivePadding()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Spacer(modifier = Modifier.weight(1f))

            TotalsGrid(state)

            Spacer(modifier = Modifier.weight(1f))
        }
        Text(text = paymentFlowState.toString())
        AnimatedVisibility(visible = isButtonVisible) {
            WooPosButtonLarge(
                text = stringResource(R.string.woopos_payment_collect_payment_label),
                onClick = { onUIEvent(WooPosTotalsUIEvent.CollectPaymentClicked) },
                modifier = Modifier
                    .animateEnterExit(
                        enter = slideInVertically { it },
                    )
            )
        }
    }
}

@Composable
private fun TotalsGrid(state: WooPosTotalsViewState.Totals) {
    Column(
        modifier = Modifier
            .padding(24.dp.toAdaptivePadding())
            .width(382.dp)
    ) {
        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_subtotal_label),
            textTwo = state.orderSubtotalText,
        )

        Spacer(modifier = Modifier.height(8.dp.toAdaptivePadding()))

        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_tax_label),
            textTwo = state.orderTaxText,
        )

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        Divider(color = WooPosTheme.colors.border, thickness = 1.dp)

        Spacer(modifier = Modifier.height(16.dp.toAdaptivePadding()))

        TotalsGridRow(
            textOne = stringResource(R.string.woopos_payment_total_label),
            textTwo = state.orderTotalText,
            styleOne = MaterialTheme.typography.h4,
            styleTwo = MaterialTheme.typography.h4,
            fontWeightOne = FontWeight.Medium,
            fontWeightTwo = FontWeight.Bold,
        )
    }
}

@Composable
private fun TotalsGridRow(
    textOne: String,
    textTwo: String,
    styleOne: TextStyle = MaterialTheme.typography.h5,
    styleTwo: TextStyle = MaterialTheme.typography.h5,
    fontWeightOne: FontWeight = FontWeight.Normal,
    fontWeightTwo: FontWeight = FontWeight.Normal,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = textOne,
            style = styleOne,
            fontWeight = fontWeightOne,
        )
        Text(
            text = textTwo,
            style = styleTwo,
            fontWeight = fontWeightTwo,
        )
    }
}

@Composable
private fun TotalsLoading() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
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

            Spacer(modifier = Modifier.height(24.dp.toAdaptivePadding()))

            WooPosShimmerBox(
                modifier = Modifier
                    .height(24.dp)
                    .width(332.dp)
                    .clip(RoundedCornerShape(4.dp))
            )

            Spacer(modifier = Modifier.height(24.dp.toAdaptivePadding()))

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
private fun TotalsErrorScreen(
    errorMessage: String,
    onUIEvent: (WooPosTotalsUIEvent) -> Unit
) {
    WooPosErrorScreen(
        message = stringResource(R.string.woopos_totals_main_error_label),
        reason = errorMessage,
        primaryButton = Button(
            text = stringResource(R.string.retry),
            click = { onUIEvent(WooPosTotalsUIEvent.RetryOrderCreationClicked) }
        ),
        adaptToScreenHeight = true,
    )
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenPreview(modifier: Modifier = Modifier) {
    WooPosTheme {
        WooPosTotalsScreen(
            modifier = modifier,
            state = WooPosTotalsViewState.Totals(
                orderSubtotalText = "$420.00",
                orderTotalText = "$462.00",
                orderTaxText = "$42.00",
            ),
            paymentFlowState = IppPaymentStateObserver.PaymentFlowState.Idle,
            onUIEvent = {}
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsScreenLoadingPreview() {
    WooPosTheme {
        WooPosTotalsScreen(
            state = WooPosTotalsViewState.Loading,
            paymentFlowState = IppPaymentStateObserver.PaymentFlowState.Idle,
            onUIEvent = {}
        )
    }
}

@Composable
@WooPosPreview
fun WooPosTotalsErrorScreenPreview() {
    WooPosTheme {
        TotalsErrorScreen(
            errorMessage = "An error occurred. Please try again.",
            onUIEvent = {}
        )
    }
}
