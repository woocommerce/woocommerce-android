package com.woocommerce.android.ui.woopos.settings

import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.woocommerce.android.ui.woopos.home.HOME_ROUTE
import com.woocommerce.android.ui.woopos.root.navigation.WooPosNavigationEvent
import com.woocommerce.android.ui.woopos.root.navigation.navigateOnce
import com.woocommerce.android.ui.woopos.settings.categories.WooPosSettingsCategory

const val SETTINGS_ROUTE = "$HOME_ROUTE/settings"
const val BARCODE_SCANNER_SETTINGS_ROUTE = "$HOME_ROUTE/settings/barcode_scanners"

fun NavController.navigateToSettingsScreen() {
    navigateOnce(SETTINGS_ROUTE)
}

internal fun NavController.navigateToBarcodeScannerSettingsScreen() {
    navigateOnce(BARCODE_SCANNER_SETTINGS_ROUTE)
}

fun NavGraphBuilder.settingsScreen(
    onNavigationEvent: (WooPosNavigationEvent) -> Unit
) {
    composable(
        route = SETTINGS_ROUTE,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) {
        WooPosSettingsScreen(
            onNavigationEvent = onNavigationEvent,
        )
    }

    composable(
        route = BARCODE_SCANNER_SETTINGS_ROUTE,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
            )
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth },
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
            )
        },
    ) {
        WooPosSettingsScreen(
            onNavigationEvent = onNavigationEvent,
            initial = WooPosSettingsCategory.HARDWARE to WooPosSettingsDetailDestination.Hardware.BarcodeScanners
        )
    }
}
