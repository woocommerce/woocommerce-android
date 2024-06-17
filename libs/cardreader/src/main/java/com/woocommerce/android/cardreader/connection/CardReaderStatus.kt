package com.woocommerce.android.cardreader.connection

sealed class CardReaderStatus {
    data class NotConnected(val errorMessage: String? = null) : CardReaderStatus()
    data class Connected(val cardReader: CardReader) : CardReaderStatus()
    data object Connecting : CardReaderStatus()
}
