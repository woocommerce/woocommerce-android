package com.woocommerce.android.cardreader

sealed class CardReaderStatus {
    object NotConnected : CardReaderStatus()
    data class Connected(val cardReader: CardReader) : CardReaderStatus()
    object Connecting : CardReaderStatus()
}
