package com.woocommerce.android.ui.woopos.home.phone

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.ui.woopos.home.WooPosHomeDialogs
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
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

    BackHandler {
        when (state.screenPositionState) {
            WooPosHomeState.ScreenPositionState.Products ->
                onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
            WooPosHomeState.ScreenPositionState.Cart ->
                onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCartClicked)
            is WooPosHomeState.ScreenPositionState.Checkout ->
                onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCheckoutClicked)
        }
    }

    val targetScreen = state.phoneScreen

    Box(
        modifier = Modifier.wooPosHomeRootContainer(state, onHomeUIEvent)
    ) {
        AnimatedContent(
            targetState = phoneScreenFor(state.screenPositionState),
            transitionSpec = { screenTransitionFor(initialState, targetState) },
            modifier = Modifier.fillMaxSize(),
            label = "phone_screen_transition",
        ) { screen ->
            val isLeaving = screen != targetScreen
            when (screen) {
                WooPosHomeState.PhoneScreen.Products -> WooPosPhoneCartSheet(
                    isVisible = rememberKeptWhile(isLeaving, state.isPhoneCartSheetVisible),
                    cartState = cartState,
                    onOpenCartClick = { onHomeUIEvent(WooPosHomeUIEvent.PhoneOpenCartClicked) },
                    onDismissRequest = { onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCartClicked) },
                    onCartUIEvent = cartViewModel::onUIEvent,
                ) {
                    WooPosPhoneProductsScreen(itemsViewModel = itemsViewModel)
                }

                WooPosHomeState.PhoneScreen.Checkout -> WooPosPhoneTotalsScreen(
                    onBack = { onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCheckoutClicked) },
                    viewModel = totalsViewModel,
                )
            }
        }

        WooPosHomeDialogs(state.dialogState, onHomeUIEvent)
    }
}

@Composable
private fun <T> rememberKeptWhile(isKept: Boolean, value: T): T {
    var keptValue by remember { mutableStateOf(value) }
    if (!isKept) keptValue = value
    return keptValue
}

private fun AnimatedContentTransitionScope<WooPosHomeState.PhoneScreen>.screenTransitionFor(
    from: WooPosHomeState.PhoneScreen,
    to: WooPosHomeState.PhoneScreen,
): ContentTransform {
    return when {
        from == WooPosHomeState.PhoneScreen.Products && to == WooPosHomeState.PhoneScreen.Checkout ->
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }

        from == WooPosHomeState.PhoneScreen.Checkout && to == WooPosHomeState.PhoneScreen.Products ->
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }

        else -> fadeIn() togetherWith fadeOut()
    }
}
