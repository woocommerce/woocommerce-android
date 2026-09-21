package com.woocommerce.android.cardreader.payments

import com.woocommerce.android.cardreader.payments.CardPaymentStatus.PaymentMethodType

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
        val paymentMethodType: PaymentMethodType,
    ) : RetrieveAndCollectResult()

    data class Failed(val cause: Throwable) : RetrieveAndCollectResult()
}
