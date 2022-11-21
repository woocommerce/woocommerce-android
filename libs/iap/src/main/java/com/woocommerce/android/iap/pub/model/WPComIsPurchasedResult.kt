package com.woocommerce.android.iap.pub.model

sealed class WPComIsPurchasedResult {
    data class Success(val isPlanPurchased: Boolean) : WPComIsPurchasedResult()
    data class Error(val errorType: IAPError) : WPComIsPurchasedResult()
}
