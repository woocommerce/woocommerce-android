package com.woocommerce.android.cardreader

interface WCPayStoreWrapper {
    suspend fun getConnectionToken(): String

    suspend fun capturePaymentIntent(id: String): Boolean
}
