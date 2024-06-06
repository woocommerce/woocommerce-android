package com.woocommerce.android.ui.woopos.root

import androidx.annotation.StringRes
import com.woocommerce.android.R

data class BottomToolbarState(
    val cardReaderStatus: CardReaderStatus
) {
    sealed class CardReaderStatus(@StringRes val title: Int) {
        data object NotConnected : CardReaderStatus(title = R.string.woopos_reader_disconnected)
        data object Connecting : CardReaderStatus(title = R.string.woopos_reader_connecting)
        data object Connected : CardReaderStatus(title = R.string.woopos_reader_connected)
        data object Unknown : CardReaderStatus(title = R.string.woopos_reader_unknown)
    }
}
