package com.woocommerce.android.ui.woopos.root.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.woocommerce.android.ui.woopos.home.homeScreen
import com.woocommerce.android.ui.woopos.splash.SPLASH_ROUTE
import com.woocommerce.android.ui.woopos.splash.splashScreen

const val MAIN_GRAPH_ROUTE = "main-graph"

fun NavGraphBuilder.mainGraph(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    navigation(
        startDestination = SPLASH_ROUTE,
        route = MAIN_GRAPH_ROUTE,
    ) {
        splashScreen(onNavigationEvent = onNavigationEvent)
        homeScreen(onNavigationEvent = onNavigationEvent)
    }
}
