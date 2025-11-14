package com.woocommerce.android.ui.woopos.splash

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce

const val SPLASH_ROUTE = "splash"

fun NavController.navigateToSplashScreen() {
    navigateOnce(SPLASH_ROUTE)
}

fun NavGraphBuilder.splashScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(SPLASH_ROUTE) {
        WooPosSplashScreen(onNavigationEvent)
    }
}
