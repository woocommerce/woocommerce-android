package com.woocommerce.android.iap.pub.model

sealed class IAPSupportedResult {
    data class Success(val isSupported: Boolean) : IAPSupportedResult()
    data class Error(val errorType: IAPError) : IAPSupportedResult()
}
