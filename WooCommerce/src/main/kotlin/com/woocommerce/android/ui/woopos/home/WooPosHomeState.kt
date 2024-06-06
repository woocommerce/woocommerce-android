package com.woocommerce.android.ui.woopos.home

import androidx.annotation.StringRes

sealed class WooPosHomeState {
    data class Cart(
        val exitConfirmationDialog: WooPosExitConfirmationDialog?
    ) : WooPosHomeState()

    data object Checkout : WooPosHomeState()
}

data class WooPosExitConfirmationDialog(
    @StringRes val title: Int,
    @StringRes val message: Int,
    @StringRes val positiveButton: Int,
    @StringRes val negativeButton: Int,
)
