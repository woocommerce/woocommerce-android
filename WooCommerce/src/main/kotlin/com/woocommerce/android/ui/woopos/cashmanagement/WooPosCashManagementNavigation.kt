package com.woocommerce.android.ui.woopos.cashmanagement

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val CASH_MANAGEMENT_ROUTE = "$HOME_ROUTE/cash-management"

fun NavController.navigateToCashManagement() = navigateOnce(CASH_MANAGEMENT_ROUTE)

fun NavGraphBuilder.cashManagementScreen(onNavigationEvent: (WooPosNavigationEvent) -> Unit) {
    composable(
        route = CASH_MANAGEMENT_ROUTE,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) },
    ) {
        WooPosCashManagementScreen(
            onBack = { onNavigationEvent(WooPosNavigationEvent.GoBack) },
            onOrder = { onNavigationEvent(WooPosNavigationEvent.OpenOrderDetails(it)) },
        )
    }
}
