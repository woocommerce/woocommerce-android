package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetails

internal class IAPBillingFlowParamsBuilder {
    fun buildBillingFlowParams(productDetails: ProductDetails): BillingFlowParams {
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setOfferToken(productDetails.firstOfferToken)
            .setProductDetails(productDetails)
            .build()
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
    }
}
