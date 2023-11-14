package com.woocommerce.android.iap.pub.network.model

sealed class CreateAndConfirmOrderResponse {
    data class Success(val orderId: Long) : CreateAndConfirmOrderResponse()

    object Network : CreateAndConfirmOrderResponse()
    data class Server(val reason: String) : CreateAndConfirmOrderResponse()
}
