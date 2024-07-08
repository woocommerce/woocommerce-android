package com.woocommerce.android.ui.woopos.root.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.home.homeScreen

const val MAIN_GRAPH_ROUTE = "main-graph"

fun NavGraphBuilder.mainGraph(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    navigation(
        startDestination = HOME_ROUTE,
        route = MAIN_GRAPH_ROUTE,
    ) {
        homeScreen(onNavigationEvent = onNavigationEvent)
    }
}
