package com.woocommerce.android.ui.woopos.home.phone

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosText
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosSpacing
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTypography
import com.woocommerce.android.ui.woopos.home.WooPosHomeCartPane
import com.woocommerce.android.ui.woopos.home.WooPosHomeDialogs
import com.woocommerce.android.ui.woopos.home.WooPosHomeState
import com.woocommerce.android.ui.woopos.home.WooPosHomeTotalsPane
import com.woocommerce.android.ui.woopos.home.WooPosHomeUIEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartViewModel
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsViewModel
import com.woocommerce.android.ui.woopos.home.totals.WooPosTotalsViewModel
import com.woocommerce.android.ui.woopos.home.wooPosHomeRootContainer
import com.woocommerce.android.util.PackageUtils
import org.wordpress.android.util.ToastUtils

private const val PHONE_PRODUCTS_ROUTE = "phone_products"
private const val PHONE_CART_ROUTE = "phone_cart"
private const val PHONE_TOTALS_ROUTE = "phone_totals"

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
    val navController = rememberNavController()

    // Acquire the shared ViewModels once at the parent NavBackStackEntry scope so they
    // stay alive across inner navigation between products / cart / totals. Without this,
    // hiltViewModel() calls inside each destination would scope to that destination's
    // inner NavBackStackEntry and be recreated on every navigation.
    val itemsViewModel: WooPosItemsViewModel = hiltViewModel()
    val cartViewModel: WooPosCartViewModel = hiltViewModel()
    val totalsViewModel: WooPosTotalsViewModel = hiltViewModel()

    var previousState by remember {
        mutableStateOf<WooPosHomeState.ScreenPositionState?>(null)
    }

    LaunchedEffect(state.screenPositionState) {
        val currentRoute = navController.currentDestination?.route
        when (state.screenPositionState) {
            is WooPosHomeState.ScreenPositionState.Cart -> {
                val cameFromCartWithTotals =
                    previousState is WooPosHomeState.ScreenPositionState.Checkout.CartWithTotals
                when (currentRoute) {
                    PHONE_TOTALS_ROUTE if cameFromCartWithTotals -> {
                        navController.popBackStack()
                    }
                    PHONE_TOTALS_ROUTE, PHONE_CART_ROUTE -> {
                        navController.popBackStack(PHONE_PRODUCTS_ROUTE, inclusive = false)
                    }
                }
            }
            is WooPosHomeState.ScreenPositionState.Checkout -> {
                if (currentRoute != PHONE_TOTALS_ROUTE) {
                    navController.navigate(PHONE_TOTALS_ROUTE) {
                        launchSingleTop = true
                    }
                }
            }
        }
        previousState = state.screenPositionState
    }

    BackHandler {
        if (!navController.popBackStack()) {
            onHomeUIEvent(WooPosHomeUIEvent.SystemBackClicked)
        }
    }

    Box(
        modifier = Modifier.wooPosHomeRootContainer(state, onHomeUIEvent)
    ) {
        NavHost(
            navController = navController,
            startDestination = PHONE_PRODUCTS_ROUTE,
            modifier = Modifier.fillMaxSize(),
        ) {
            composable(PHONE_PRODUCTS_ROUTE) {
                Box(modifier = Modifier.fillMaxSize()) {
                    WooPosPhoneProductsScreen(itemsViewModel = itemsViewModel)

                    if (PackageUtils.isDebugBuild()) {
                        // Temporary trigger so Cart is reachable until the persistent
                        // bottom button lands in WOOMOB-2657. Remove with that ticket.
                        ExtendedFloatingActionButton(
                            text = { WooPosText(text = "Cart (debug)", style = WooPosTypography.BodyLarge) },
                            icon = {},
                            onClick = {
                                navController.navigate(PHONE_CART_ROUTE) {
                                    launchSingleTop = true
                                }
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(WooPosSpacing.Medium.value)
                        )
                    }
                }
            }
            composable(PHONE_CART_ROUTE) {
                WooPosHomeCartPane(viewModel = cartViewModel)
            }
            composable(PHONE_TOTALS_ROUTE) {
                WooPosHomeTotalsPane(viewModel = totalsViewModel)
            }
        }

        WooPosHomeDialogs(state.dialogState, onHomeUIEvent)
    }
}
