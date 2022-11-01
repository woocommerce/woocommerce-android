package com.woocommerce.android.iap.internal.model

import com.woocommerce.android.iap.pub.model.IAPError

internal sealed class IAPBillingClientConnectionResult {
    object Success : IAPBillingClientConnectionResult()
    data class Error(val errorType: IAPError.Billing) : IAPBillingClientConnectionResult()
}
