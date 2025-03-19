package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosToolbarState(
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

        sealed class MenuItem(
            @StringRes open val title: Int,
            @DrawableRes open val icon: Int
        ) {
            data class Standard(
                @StringRes override val title: Int,
                @DrawableRes override val icon: Int
            ) : MenuItem(title, icon)

            data class Toggleable(
                @StringRes override val title: Int,
                @DrawableRes override val icon: Int,
                val isToggled: Boolean
            ) : MenuItem(title, icon)
        }
    }
}
