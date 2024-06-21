package com.woocommerce.android.ui.woopos.home

import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.root.navigation.MAIN_GRAPH_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

const val HOME_ROUTE = "home"

fun NavController.navigateToHomeScreen() {
    navigate(HOME_ROUTE)
}

fun NavGraphBuilder.homeScreen(
    navController: NavHostController,
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(HOME_ROUTE) { backStackEntry ->
        val viewModelStoreOwner = remember(backStackEntry) { navController.getBackStackEntry(MAIN_GRAPH_ROUTE) }
        WooPosHomeScreen(viewModelStoreOwner, onNavigationEvent)
    }
}
