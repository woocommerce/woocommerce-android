package com.woocommerce.android.ui.woopos.home.toolbar

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R

data class WooPosHomeFloatingToolbarState(
    val cardReaderStatus: WooPosCardReaderStatus,
    val menu: Menu,
) {
    sealed class WooPosCardReaderStatus(@StringRes val title: Int) {
        data object NotConnected : WooPosCardReaderStatus(title = R.string.woopos_reader_disconnected)
        data class Connected(
            val batteryState: BatteryState = BatteryState.NOMINAL
        ) : WooPosCardReaderStatus(title = R.string.woopos_reader_connected)
        data object Reconnecting : WooPosCardReaderStatus(title = R.string.woopos_reader_reconnecting)
    }

    enum class BatteryState {
        NOMINAL,
        LOW,
        CRITICAL
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
