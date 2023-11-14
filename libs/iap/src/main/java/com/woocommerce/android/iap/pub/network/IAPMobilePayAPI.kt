package com.woocommerce.android.iap.pub.network

import com.woocommerce.android.iap.pub.network.model.CreateAndConfirmOrderResponse

interface IAPMobilePayAPI {
    @Suppress("LongParameterList")
    suspend fun createAndConfirmOrder(
        remoteSiteId: Long,
        productIdentifier: String,
        priceInCents: Int,
        currency: String,
        purchaseToken: String,
        appId: String,
    ): CreateAndConfirmOrderResponse
}
