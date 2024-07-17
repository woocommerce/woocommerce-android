package com.woocommerce.android.ui.woopos.root

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosRootScreenState(
    val cardReaderStatus: WooPosCardReaderStatus,
    val menu: Menu,
    val exitConfirmationDialog: WooPosExitConfirmationDialog?,
) {
    sealed class WooPosCardReaderStatus(@StringRes val title: Int) {
        data object NotConnected : WooPosCardReaderStatus(title = R.string.woopos_reader_disconnected)
        data object Connected : WooPosCardReaderStatus(title = R.string.woopos_reader_connected)
    }

    data object WooPosExitConfirmationDialog {
        val title: Int = R.string.woopos_exit_confirmation_title
        val message: Int = R.string.woopos_exit_confirmation_message
        val confirmButton: Int = R.string.woopos_exit_confirmation_confirm_button
        val dismissButton: Int = R.string.woopos_exit_confirmation_dismiss_button
    }

    sealed class Menu {
        data object Hidden : Menu()
        data class Visible(val items: List<MenuItem>) : Menu()

        data class MenuItem(
            val id: Int,
            val title: Int,
            val icon: Int,
        )
    }
}
