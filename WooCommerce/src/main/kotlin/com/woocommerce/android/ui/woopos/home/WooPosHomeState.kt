package com.woocommerce.android.ui.woopos.home

import com.woocommerce.android.R

data class WooPosHomeState(
    val screenPositionState: ScreenPositionState,
    val exitConfirmationDialog: WooPosExitConfirmationDialog? = null,
) {
    sealed class ScreenPositionState {
        sealed class Cart : ScreenPositionState() {
            data object Empty : Cart()
            data object NotEmpty : Cart()
        }

        sealed class Checkout : ScreenPositionState() {
            data object NotPaid : Checkout()
            data object Paid : Checkout()
        }
    }
}

data object WooPosExitConfirmationDialog {
    val title: Int = R.string.woopos_exit_confirmation_title
    val message: Int = R.string.woopos_exit_confirmation_message
    val confirmButton: Int = R.string.woopos_exit_confirmation_confirm_button
    val dismissButton: Int = R.string.woopos_exit_confirmation_dismiss_button
}
