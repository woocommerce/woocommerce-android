package com.woocommerce.android.ui.woopos.home.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.WooPosHomeDialogs
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartUIEvent
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsUIEvent
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel
import com.woocommerce.android.ui.woopos.home.wooPosHomeRootContainer
import org.wordpress.android.util.ToastUtils

@Composable
fun WooPosHomePhoneScreen(
    isPaymentCompletedViaCash: Boolean,
    viewModel: WooPosHomeViewModel,
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (isPaymentCompletedViaCash) {
            viewModel.onUIEvent(WooPosHomeUIEvent.OnPaymentCompletedViaCash)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            ToastUtils.showToast(context, message, ToastUtils.Duration.LONG)
        }
    }

    WooPosHomePhoneContent(
        state = state,
        onHomeUIEvent = { viewModel.onUIEvent(it) },
    )
}

@Composable
private fun WooPosHomePhoneContent(
    state: WooPosHomeState,
    onHomeUIEvent: (WooPosHomeUIEvent) -> Unit,
) {
    val itemsViewModel: WooPosItemsViewModel = hiltViewModel()
    val cartViewModel: WooPosCartViewModel = hiltViewModel()
    val totalsViewModel: WooPosTotalsViewModel = hiltViewModel()

    val cartState by cartViewModel.state.observeAsState()
    val totalsState by totalsViewModel.state.collectAsState()

    BackHandler {
        when (state.screenPositionState) {
            WooPosHomeState.ScreenPositionState.Products ->
                onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
            WooPosHomeState.ScreenPositionState.Cart ->
                onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCartClicked)
            is WooPosHomeState.ScreenPositionState.Checkout ->
                onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
        }
    }

    val cartTitle = stringResource(R.string.woopos_cart_title)
    val checkoutLabel = stringResource(R.string.woopos_checkout_button)
    val cashPaymentLabel = stringResource(R.string.woopos_payment_take_cash_payment_label)

    val resolver = remember(cartTitle, checkoutLabel, cashPaymentLabel) {
        WooPosPhonePersistentButtonStateResolver(
            buildCartLabel = { count -> "$cartTitle ($count)" },
            checkoutLabel = checkoutLabel,
            cashPaymentLabel = cashPaymentLabel,
        )
    }
    val persistentButtonState = resolver.resolve(
        screenPositionState = state.screenPositionState,
        cartState = cartState,
        totalsState = totalsState,
    )

    Box(
        modifier = Modifier.wooPosHomeRootContainer(state, onHomeUIEvent)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (state.screenPositionState) {
                    WooPosHomeState.ScreenPositionState.Products ->
                        WooPosPhoneProductsScreen(itemsViewModel = itemsViewModel)

                    WooPosHomeState.ScreenPositionState.Cart -> WooPosPhoneCartScreen(
                        onBack = { onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCartClicked) },
                        viewModel = cartViewModel,
                    )

                    is WooPosHomeState.ScreenPositionState.Checkout ->
                        WooPosPhoneTotalsScreen(viewModel = totalsViewModel)
                }
            }

            WooPosHomePhonePersistentButton(
                state = persistentButtonState,
                onAction = { action ->
                    when (action) {
                        WooPosPhonePersistentButtonAction.OpenCart ->
                            onHomeUIEvent(WooPosHomeUIEvent.PhoneOpenCartClicked)
                        WooPosPhonePersistentButtonAction.Checkout ->
                            cartViewModel.onUIEvent(WooPosCartUIEvent.CheckoutClicked)
                        WooPosPhonePersistentButtonAction.CashPayment ->
                            totalsViewModel.onUIEvent(WooPosTotalsUIEvent.OnCashPaymentClicked)
                    }
                },
            )
        }

        WooPosHomeDialogs(state.dialogState, onHomeUIEvent)
    }
}
