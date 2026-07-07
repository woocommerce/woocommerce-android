package com.woocommerce.android.ui.woopos.splash

import com.woocommerce.android.ui.woopos.tab.WooPosLaunchability

sealed class WooPosSplashState {
    data object Loading : WooPosSplashState()
    data object Syncing : WooPosSplashState()
    data object SyncPreparing : WooPosSplashState()
    data class SyncProgress(val processed: Int, val total: Int) : WooPosSplashState()
    data class SyncFailed(val error: String, val isServerPermissionsError: Boolean = false) : WooPosSplashState()
    data object Loaded : WooPosSplashState()
    data class NotEligible(val reason: WooPosLaunchability.NonLaunchabilityReason) : WooPosSplashState()
}
