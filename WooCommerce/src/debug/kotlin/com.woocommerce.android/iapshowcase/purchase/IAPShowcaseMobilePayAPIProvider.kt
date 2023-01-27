package com.woocommerce.android.iapshowcase.purchase

import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI
import com.woocommerce.android.iap.pub.network.model.CreateAndConfirmOrderResponse
import org.wordpress.android.fluxc.network.rest.wpcom.mobilepay.MobilePayRestClient
import org.wordpress.android.fluxc.store.MobilePayStore
import javax.inject.Inject

class IAPShowcaseMobilePayAPIProvider @Inject constructor(private val mobilePayStore: MobilePayStore) {
    fun buildMobilePayAPI(customUrl: String?) = object : IAPMobilePayAPI {
        override suspend fun createAndConfirmOrder(
            remoteSiteId: Long,
            productIdentifier: String,
            priceInCents: Int,
            currency: String,
            purchaseToken: String,
            appId: String
        ): CreateAndConfirmOrderResponse {
            val response = mobilePayStore.createOrder(
                productIdentifier,
                priceInCents,
                currency,
                purchaseToken,
                appId,
                remoteSiteId,
                customUrl = customUrl
            )
            return when (response) {
                is MobilePayRestClient.CreateOrderResponse.Success -> {
                    CreateAndConfirmOrderResponse.Success(response.orderId)
                }
                is MobilePayRestClient.CreateOrderResponse.Error -> {
                    when (response.type) {
                        MobilePayRestClient.CreateOrderErrorType.API_ERROR,
                        MobilePayRestClient.CreateOrderErrorType.AUTH_ERROR,
                        MobilePayRestClient.CreateOrderErrorType.GENERIC_ERROR,
                        MobilePayRestClient.CreateOrderErrorType.NETWORK_ERROR,
                        MobilePayRestClient.CreateOrderErrorType.INVALID_RESPONSE ->
                            CreateAndConfirmOrderResponse.Server(response.message ?: "Reason is not provided")
                        MobilePayRestClient.CreateOrderErrorType.TIMEOUT -> CreateAndConfirmOrderResponse.Network
                    }
                }
            }
        }
    }
}
