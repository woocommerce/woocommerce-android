package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosHomeToolbarState(
    val cardReaderStatus: WooPosCardReaderStatus,
    val menu: Menu,
) {
    sealed class WooPosCardReaderStatus(@StringRes val title: Int) {
        data object NotConnected : WooPosCardReaderStatus(title = R.string.woopos_reader_disconnected)
        data object Connected : WooPosCardReaderStatus(title = R.string.woopos_reader_connected)
    }

    sealed class Menu {
        data object Hidden : Menu()
        data class Visible(val items: List<MenuItem>) : Menu()

        data class MenuItem(
            @StringRes val title: Int,
            @DrawableRes val icon: Int,
        )
    }
}
