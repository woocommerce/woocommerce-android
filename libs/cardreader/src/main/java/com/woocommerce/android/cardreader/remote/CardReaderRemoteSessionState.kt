package com.woocommerce.android.cardreader.remote

sealed class CardReaderRemoteSessionState {
    object Idle : CardReaderRemoteSessionState()

    data class ReadyToPair(
        val deviceName: String,
        val fingerprintSuffix: String,
    ) : CardReaderRemoteSessionState()

    data class WaitingForPayment(
        val tabletName: String?,
    ) : CardReaderRemoteSessionState()
}
