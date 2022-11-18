package com.woocommerce.android.iap.pub.model

sealed class WPComProductResult {
    data class Success(val productInfo: WPComPlanProduct) : WPComProductResult()
    data class Error(val errorType: IAPError) : WPComProductResult()
}

data class WPComPlanProduct(
    val localizedTitle: String,
    val localizedDescription: String,
    val price: Long,
    val currency: String,
)
