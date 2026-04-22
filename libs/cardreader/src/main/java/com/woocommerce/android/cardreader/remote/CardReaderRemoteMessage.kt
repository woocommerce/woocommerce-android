package com.woocommerce.android.cardreader.remote

internal sealed class CardReaderRemoteMessage {
    abstract val requestId: String

    data class ConnectRequest(
        override val requestId: String,
        val connectionToken: String,
        val locationId: String,
    ) : CardReaderRemoteMessage()

    data class ConnectAck(
        override val requestId: String,
        val readerSerial: String?,
    ) : CardReaderRemoteMessage()

    data class CollectPaymentRequest(
        override val requestId: String,
        val paymentIntentClientSecret: String,
    ) : CardReaderRemoteMessage()

    data class PaymentIntentResult(
        override val requestId: String,
        val paymentIntentId: String,
        val status: String,
    ) : CardReaderRemoteMessage()

    data class ErrorMessage(
        override val requestId: String,
        val code: String,
        val description: String,
    ) : CardReaderRemoteMessage()
}
