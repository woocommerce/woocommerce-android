package com.woocommerce.android.ui.woopos.splash

sealed class WooPosSplashState {
    data object Loading : WooPosSplashState()
    data object Loaded : WooPosSplashState()
}
