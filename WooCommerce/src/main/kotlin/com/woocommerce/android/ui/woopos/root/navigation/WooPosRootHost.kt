package com.woocommerce.android.ui.woopos.root.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.woocommerce.android.ui.woopos.home.ChildToParentEvent.NavigationEvent
import com.woocommerce.android.ui.woopos.home.WooPosHomeViewModel
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent.ExitPosClicked
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent.OpenCashPayment
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent.OpenEmailReceipt
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent.ReturnHomeFromCashPayment

@Composable
fun WooPosRootHost(
    modifier: Modifier = Modifier,
    rootController: NavHostController,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    val homeViewModel = hiltViewModel<WooPosHomeViewModel>()
    LaunchedEffect(Unit) {
        homeViewModel.navigationEvent.collect {
            when (it) {
                is NavigationEvent.ToCashPayment -> onNavigationEvent(OpenCashPayment(it.orderId))
                is NavigationEvent.ToEmailReceipt -> onNavigationEvent(OpenEmailReceipt(it.orderId))
                NavigationEvent.ExitPos -> onNavigationEvent(ExitPosClicked)
                NavigationEvent.ReturnHomeFromCashWhenCardPaymentStarted -> onNavigationEvent(ReturnHomeFromCashPayment)
            }
        }
    }
    NavHost(
        modifier = modifier,
        navController = rootController,
        startDestination = MAIN_GRAPH_ROUTE,
    ) {
        mainGraph(
            onNavigationEvent = onNavigationEvent,
            homeViewModel = homeViewModel,
        )
    }
}
