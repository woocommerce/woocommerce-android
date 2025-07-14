package com.woocommerce.android.cardreader.connection

sealed class CardReaderStatus {
    data class NotConnected(
        val errorCode: ErrorCode? = null,
        val errorMessage: String? = null
    ) : CardReaderStatus() {
        enum class ErrorCode {
            BATTERY_CRITICALLY_LOW,
            OTHER,
        }
    }
    data class Connected(val cardReader: CardReader) : CardReaderStatus()
    data object Connecting : CardReaderStatus()
}
