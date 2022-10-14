package com.woocommerce.android.iap.internal.model

import com.woocommerce.android.iap.pub.model.IAPError

sealed class IAPSupportedResult {
    data class Success(val isSupported: Boolean) : IAPSupportedResult()
    data class Error(val errorType: IAPError) : IAPSupportedResult()
}

