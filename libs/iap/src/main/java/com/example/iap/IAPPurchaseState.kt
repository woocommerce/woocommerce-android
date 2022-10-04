package com.example.iap

import org.json.JSONObject

sealed class IAPPurchaseState {
    data class Purchased(val purchases: List<IAPPurchase>?) : IAPPurchaseState()
    sealed class Error(val debugMessage: String) : IAPPurchaseState() {
        class UserCancelled(debugMessage: String) : Error(debugMessage)
        class ItemAlreadyOwned(debugMessage: String) : Error(debugMessage)
        class DeveloperError(debugMessage: String) : Error(debugMessage)
        class ServiceDisconnected(debugMessage: String) : Error(debugMessage)
        class BillingUnavailable(debugMessage: String) : Error(debugMessage)
        class ItemUnavailable(debugMessage: String) : Error(debugMessage)
        class FeatureNotSupported(debugMessage: String) : Error(debugMessage)
        class ServiceTimeout(debugMessage: String) : Error(debugMessage)
        class Unknown(debugMessage: String) : Error(debugMessage)
        class ItemNotOwned(debugMessage: String) : Error(debugMessage)
    }

    object Unknown : IAPPurchaseState()
}

data class IAPPurchase(
    val orderId: String,
    val products: List<String>,
    val isAcknowledged: Boolean,
    val isAutoRenewing: Boolean,
    val state: PurchaseState,
    val developerPayload: String,
    val originalJson: JSONObject,
    val purchaseToken: String,
    val signature: String
) {
    enum class PurchaseState {
        UNSPECIFIED_STATE, PURCHASED, PENDING
    }
}
