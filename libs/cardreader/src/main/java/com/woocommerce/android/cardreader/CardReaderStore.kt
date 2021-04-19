package com.woocommerce.android.cardreader

interface CardReaderStore {
    suspend fun getConnectionToken(): String

    suspend fun capturePaymentIntent(id: String): Boolean
}
