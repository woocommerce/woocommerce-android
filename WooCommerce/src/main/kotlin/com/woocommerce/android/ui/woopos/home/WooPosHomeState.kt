package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.R

sealed class WooPosHomeState {
    data class Cart(
        val exitConfirmationDialog: WooPosExitConfirmationDialog?,
    ) : WooPosHomeState()

    data object Checkout : WooPosHomeState()
}

data object WooPosExitConfirmationDialog {
    val title: Int = R.string.woopos_exit_confirmation_title
    val message: Int = R.string.woopos_exit_confirmation_message
    val confirmButton: Int = R.string.woopos_exit_confirmation_dismiss_button
    val dismissButton: Int = R.string.woopos_exit_confirmation_confirm_button
}
