package com.woocommerce.android.ui.woopos.home.phone

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.home.WooPosHomeDialogs
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartUIEvent
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
                onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
        }
    }

    val cartTitle = stringResource(R.string.woopos_cart_title)
    val checkoutLabel = stringResource(R.string.woopos_checkout_button)

    val resolver = remember(cartTitle, checkoutLabel) {
        WooPosPhonePersistentButtonStateResolver(
            buildCartLabel = { count -> "$cartTitle ($count)" },
            checkoutLabel = checkoutLabel,
        )
    }
    val persistentButtonState = resolver.resolve(
        screenPositionState = state.screenPositionState,
        cartState = cartState,
    )

    Box(
        modifier = Modifier.wooPosHomeRootContainer(state, onHomeUIEvent)
    ) {
        AnimatedContent(
            targetState = state.screenPositionState,
            contentKey = { phoneScreenKey(it) },
            transitionSpec = { screenTransitionFor(initialState, targetState) },
            modifier = Modifier.fillMaxSize(),
            label = "phone_screen_transition",
        ) { screen ->
            when (screen) {
                WooPosHomeState.ScreenPositionState.Products ->
                    WooPosPhoneProductsScreen(itemsViewModel = itemsViewModel)

                WooPosHomeState.ScreenPositionState.Cart -> WooPosPhoneCartScreen(
                    onBack = { onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCartClicked) },
                    viewModel = cartViewModel,
                )

                is WooPosHomeState.ScreenPositionState.Checkout ->
                    WooPosPhoneTotalsScreen(
                        onBack = { onHomeUIEvent(WooPosHomeUIEvent.PhoneBackFromCheckoutClicked) },
                        viewModel = totalsViewModel,
                    )
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
                }
            },
            modifier = Modifier.align(Alignment.BottomCenter),
        )

        WooPosHomeDialogs(state.dialogState, onHomeUIEvent)
    }
}

private enum class PhoneScreenKey { Products, Cart, Checkout }

private fun phoneScreenKey(
    state: WooPosHomeState.ScreenPositionState,
): PhoneScreenKey = when (state) {
    WooPosHomeState.ScreenPositionState.Products -> PhoneScreenKey.Products
    WooPosHomeState.ScreenPositionState.Cart -> PhoneScreenKey.Cart
    is WooPosHomeState.ScreenPositionState.Checkout -> PhoneScreenKey.Checkout
}

private fun AnimatedContentTransitionScope<WooPosHomeState.ScreenPositionState>.screenTransitionFor(
    from: WooPosHomeState.ScreenPositionState,
    to: WooPosHomeState.ScreenPositionState,
): ContentTransform {
    val spec = tween<IntOffset>(durationMillis = 300)

    return when {
        from == WooPosHomeState.ScreenPositionState.Products &&
            to == WooPosHomeState.ScreenPositionState.Cart ->
            slideInVertically(spec) { it } togetherWith ExitTransition.KeepUntilTransitionsFinished

        // targetContentZIndex must be on the ContentTransform, not the scope, in Compose 1.9.x.
        from == WooPosHomeState.ScreenPositionState.Cart &&
            to == WooPosHomeState.ScreenPositionState.Products ->
            (EnterTransition.None togetherWith slideOutVertically(spec) { it })
                .apply { targetContentZIndex = -1f }

        from == WooPosHomeState.ScreenPositionState.Cart &&
            to is WooPosHomeState.ScreenPositionState.Checkout ->
            slideInHorizontally(spec) { it } togetherWith slideOutHorizontally(spec) { -it }

        from is WooPosHomeState.ScreenPositionState.Checkout &&
            to == WooPosHomeState.ScreenPositionState.Cart ->
            slideInHorizontally(spec) { -it } togetherWith slideOutHorizontally(spec) { it }

        else -> fadeIn() togetherWith fadeOut()
    }
}
