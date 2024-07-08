package com.woocommerce.android.ui.woopos.home

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

const val HOME_ROUTE = "home"

fun NavController.navigateToHomeScreen() {
    navigate(HOME_ROUTE)
}

fun NavGraphBuilder.homeScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(HOME_ROUTE) {
        WooPosHomeScreen(onNavigationEvent)
    }
}
