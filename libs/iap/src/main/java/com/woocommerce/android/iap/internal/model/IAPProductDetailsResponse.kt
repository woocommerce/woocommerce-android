package com.woocommerce.android.iap.internal.model

import com.android.billingclient.api.ProductDetails
import com.woocommerce.android.iap.pub.model.IAPError

sealed class IAPProductDetailsResponse {
    data class Success(val productDetails: ProductDetails) : IAPProductDetailsResponse()
    data class Error(val error: IAPError.Billing) : IAPProductDetailsResponse()
}
