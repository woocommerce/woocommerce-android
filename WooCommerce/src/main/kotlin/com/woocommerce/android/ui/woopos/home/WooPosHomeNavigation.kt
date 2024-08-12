package com.woocommerce.android.ui.woopos.home

import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent

private const val HOME_ROUTE = "home"

fun NavController.navigateToHomeScreen() {
    navigate(HOME_ROUTE)
}

fun NavGraphBuilder.homeScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = HOME_ROUTE,
        enterTransition = { slideInVertically(initialOffsetY = { -it }) },
        exitTransition = { fadeOut() },
        popEnterTransition = { slideInVertically(initialOffsetY = { -it }) },
        popExitTransition = { fadeOut() }
    ) {
        WooPosHomeScreen(onNavigationEvent)
    }
}
