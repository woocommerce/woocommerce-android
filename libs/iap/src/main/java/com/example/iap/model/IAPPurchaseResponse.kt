package com.example.iap.model

sealed class IAPPurchaseResponse {
    data class Success(val purchases: List<IAPPurchase>?) : IAPPurchaseResponse()
    data class Error(val errorType: BillingErrorType) : IAPPurchaseResponse()
}

data class IAPPurchase(
    val orderId: String,
    val products: List<String>,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
    val state: State,
    val developerPayload: String,
    val purchaseToken: String,
    val signature: String
) {
    enum class State {
        UNSPECIFIED_STATE, PURCHASED, PENDING
    }
}
