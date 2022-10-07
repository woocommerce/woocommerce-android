package com.woocommerce.android.iap.internal.model

import com.android.billingclient.api.ProductDetails
import com.woocommerce.android.iap.public.model.BillingErrorType

internal sealed class IAPProductDetailsResponse {
    data class Success(val productDetails: List<ProductDetails>) : IAPProductDetailsResponse()
    data class Error(val errorType: BillingErrorType) : IAPProductDetailsResponse()
}
