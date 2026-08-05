package com.woocommerce.android.cardreader.remote

sealed class CardReaderRemoteSessionState {
    object Idle : CardReaderRemoteSessionState()

    object Starting : CardReaderRemoteSessionState()

    data class ReadyToPair(
        val deviceName: String,
        val fingerprintSuffix: String,
    ) : CardReaderRemoteSessionState()

    data class WaitingForPayment(
        val tabletName: String?,
    ) : CardReaderRemoteSessionState()

    /**
     * [message] is shown to the merchant, [errorDescription] carries the full cause chain for analytics.
     */
    data class Error(
        val message: String?,
        val errorDescription: String? = null,
    ) : CardReaderRemoteSessionState()
}
