package com.woocommerce.android.cardreader

interface CardReaderStore {
    suspend fun getConnectionToken(): String

    suspend fun capturePaymentIntent(orderId: Long, paymentId: String): Boolean
}
