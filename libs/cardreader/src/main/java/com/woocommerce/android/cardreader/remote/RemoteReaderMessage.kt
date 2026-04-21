package com.woocommerce.android.cardreader.remote

sealed class RemoteReaderMessage {
    abstract val requestId: String

    data class ConnectRequest(
        override val requestId: String,
        val connectionToken: String,
        val locationId: String,
    ) : RemoteReaderMessage()

    data class ConnectAck(
        override val requestId: String,
        val readerSerial: String?,
    ) : RemoteReaderMessage()

    data class CollectPaymentRequest(
        override val requestId: String,
        val paymentIntentClientSecret: String,
    ) : RemoteReaderMessage()

    data class PaymentIntentResult(
        override val requestId: String,
        val paymentIntentId: String,
        val status: String,
    ) : RemoteReaderMessage()

    data class ErrorMessage(
        override val requestId: String,
        val code: String,
        val description: String,
    ) : RemoteReaderMessage()
}
