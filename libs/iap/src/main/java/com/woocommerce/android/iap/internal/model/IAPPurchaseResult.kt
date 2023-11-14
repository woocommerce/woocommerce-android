package com.woocommerce.android.iap.internal.model

import com.woocommerce.android.iap.pub.model.IAPError

internal sealed class IAPPurchaseResult {
    data class Success(val purchases: List<IAPPurchase>?) : IAPPurchaseResult()
    data class Error(val error: IAPError.Billing) : IAPPurchaseResult()
}

internal data class IAPPurchase(
    val orderId: String,
    val products: List<Product>,
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

    data class Product(
        val name: String,
        val id: String,
        val price: Long,
        val currency: String,
    )
}
