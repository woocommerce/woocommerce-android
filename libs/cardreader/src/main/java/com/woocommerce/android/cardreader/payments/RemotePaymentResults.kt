package com.woocommerce.android.cardreader.payments

sealed class CreatePaymentIntentResult {
    data class Success(
        val paymentIntentId: String,
        val clientSecret: String,
    ) : CreatePaymentIntentResult()

    data class Failed(val cause: Throwable) : CreatePaymentIntentResult()
}

sealed class RetrieveAndCollectResult {
    data class Success(
        val paymentIntentId: String,
        val status: String,
    ) : RetrieveAndCollectResult()

    data class Failed(val cause: Throwable) : RetrieveAndCollectResult()
}
