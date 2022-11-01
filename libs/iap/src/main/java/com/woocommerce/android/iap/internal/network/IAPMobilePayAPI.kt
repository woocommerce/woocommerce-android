package com.woocommerce.android.iap.internal.network

import com.woocommerce.android.iap.internal.network.model.CreateAndConfirmOrderResponse
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

internal interface IAPMobilePayAPI {
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

@Suppress("MagicNumber")
internal class IAPMobilePayAPIStub(private val iapLogWrapper: IAPLogWrapper) : IAPMobilePayAPI {
    override suspend fun createAndConfirmOrder(
        remoteSiteId: Long,
        productIdentifier: String,
        priceInCents: Int,
        currency: String,
        purchaseToken: String,
        appId: String,
    ) = withContext(Dispatchers.IO) {
        iapLogWrapper.d(
            IAP_LOG_TAG,
            "Stubbed request: " +
                "remoteSiteId $remoteSiteId, " +
                "productIdentifier $productIdentifier, " +
                "price $priceInCents, " +
                "currency $currency, " +
                "purchaseToken $purchaseToken, " +
                "appId $appId"
        )
        delay(1000)
        if (Math.random() > 0.5) {
            CreateAndConfirmOrderResponse.Success(1)
        } else {
            if (Math.random() > 0.5) {
                CreateAndConfirmOrderResponse.Network
            } else {
                CreateAndConfirmOrderResponse.Server("Server error")
            }
        }
    }
}
