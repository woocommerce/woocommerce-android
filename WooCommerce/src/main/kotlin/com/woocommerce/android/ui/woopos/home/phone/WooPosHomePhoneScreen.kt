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

    val targetScreen = phoneScreenFor(state.screenPositionState)

    Box(
        modifier = Modifier.wooPosHomeRootContainer(state, onHomeUIEvent)
    ) {
        AnimatedContent(
            targetState = targetScreen,
            transitionSpec = { screenTransitionFor(initialState, targetState) },
            modifier = Modifier.fillMaxSize(),
            label = "phone_screen_transition",
        ) { screen ->
            val isLeaving = screen != targetScreen
            when (screen) {
                WooPosPhoneScreen.Products -> WooPosPhoneCartSheet(
                    isVisible = rememberKeptWhile(isLeaving, state.isPhoneCartSheetVisible),
                    cartState = cartState,
                    onOpenCartClick = { onHomeUIEvent(WooPosHomeUIEvent.PhoneOpenCartClicked) },
                    onDismissRequest = { onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCartClicked) },
                    onCartUIEvent = cartViewModel::onUIEvent,
                ) {
                    WooPosPhoneProductsScreen(itemsViewModel = itemsViewModel)
                }

                WooPosPhoneScreen.Checkout -> WooPosPhoneTotalsScreen(
                    onBack = { onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCheckoutClicked) },
                    viewModel = totalsViewModel,
                    isLeaving = isLeaving,
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

internal enum class WooPosPhoneScreen { Products, Checkout }

internal fun phoneScreenFor(
    state: WooPosHomeState.ScreenPositionState,
): WooPosPhoneScreen = when (state) {
    WooPosHomeState.ScreenPositionState.Products,
    WooPosHomeState.ScreenPositionState.Cart -> WooPosPhoneScreen.Products
    is WooPosHomeState.ScreenPositionState.Checkout -> WooPosPhoneScreen.Checkout
}

private fun AnimatedContentTransitionScope<WooPosPhoneScreen>.screenTransitionFor(
    from: WooPosPhoneScreen,
    to: WooPosPhoneScreen,
): ContentTransform {
    return when {
        from == WooPosPhoneScreen.Products && to == WooPosPhoneScreen.Checkout ->
            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }

        from == WooPosPhoneScreen.Checkout && to == WooPosPhoneScreen.Products ->
            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }

        else -> fadeIn() togetherWith fadeOut()
    }
}
