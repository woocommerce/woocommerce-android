package com.woocommerce.android.ui.woopos.root.navigation

import androidx.activity.ComponentActivity
import androidx.navigation.NavHostController

fun NavHostController.handleNavigationEvent(
    event: WooPosNavigationEvent,
    activity: ComponentActivity
) {
    when (event) {
        is WooPosNavigationEvent.ExitPosClicked -> activity.finish()
    }
}
