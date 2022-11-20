package com.woocommerce.android.iap.internal.network.model

internal sealed class CreateAndConfirmOrderResponse {
    data class Success(val orderId: Long) : CreateAndConfirmOrderResponse()

    object Network : CreateAndConfirmOrderResponse()
    data class Server(val reason: String) : CreateAndConfirmOrderResponse()
}
