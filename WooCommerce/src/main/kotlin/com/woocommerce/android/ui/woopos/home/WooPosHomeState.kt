package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.R

sealed class WooPosHomeState {
    data class Cart(
        val exitConfirmationDialog: WooPosExitConfirmationDialog? = null,
    ) : WooPosHomeState()

    data object Checkout : WooPosHomeState()
}

data object WooPosExitConfirmationDialog {
    val title: Int = R.string.woopos_exit_confirmation_title
    val message: Int = R.string.woopos_exit_confirmation_message
    val positiveButton: Int = R.string.woopos_exit_confirmation_positive_button
    val negativeButton: Int = R.string.woopos_exit_confirmation_negative_button
}
