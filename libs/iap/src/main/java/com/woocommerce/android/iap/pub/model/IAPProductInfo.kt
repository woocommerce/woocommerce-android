package com.woocommerce.android.iap.pub.model

sealed class IAPProductInfoResponse {
    data class Success(val productInfo: IAPProductInfo) : IAPProductInfoResponse()
    data class Error(val errorType: IAPBillingErrorType) : IAPProductInfoResponse()
}

data class IAPProductInfo(
    val localizedTitle: String,
    val localizedDescription: String,
    val displayPrice: String,
)
