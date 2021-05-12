package com.woocommerce.android.cardreader

interface CardReaderStore {
    suspend fun getConnectionToken(): String

    suspend fun capturePaymentIntent(orderId: Long, paymentId: String): CapturePaymentResponse

    enum class CapturePaymentResponse {
        SUCCESS,
        GENERIC_ERROR,
        PAYMENT_ALREADY_CAPTURED,
        MISSING_ORDER,
        CAPTURE_ERROR,
        SERVER_ERROR,
        NETWORK_ERROR;
    }
}
