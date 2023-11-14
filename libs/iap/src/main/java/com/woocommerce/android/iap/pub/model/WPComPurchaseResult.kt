package com.woocommerce.android.iap.pub.model

sealed class WPComPurchaseResult {
    object Success : WPComPurchaseResult()
    data class Error(val errorType: IAPError) : WPComPurchaseResult()
}
