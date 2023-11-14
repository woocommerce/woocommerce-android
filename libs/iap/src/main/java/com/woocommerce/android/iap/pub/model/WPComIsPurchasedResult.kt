package com.woocommerce.android.iap.pub.model

sealed class WPComIsPurchasedResult {
    data class Success(val purchaseStatus: PurchaseStatus) : WPComIsPurchasedResult()
    data class Error(val errorType: IAPError) : WPComIsPurchasedResult()
}

enum class PurchaseStatus {
    PURCHASED_AND_ACKNOWLEDGED,
    PURCHASED,
    NOT_PURCHASED,
}
