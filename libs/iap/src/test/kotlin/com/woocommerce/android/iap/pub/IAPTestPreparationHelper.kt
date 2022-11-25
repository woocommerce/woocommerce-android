package com.woocommerce.android.iap.pub

import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetailsResult
import com.woocommerce.android.iap.internal.core.IAPBillingClientWrapper
import com.woocommerce.android.iap.internal.core.isSuccess
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

internal class IAPTestPreparationHelper(private val billingClientMock: IAPBillingClientWrapper) {
    suspend fun setupQueryProductDetails(
        responseCode: Int,
        debugMessage: String = "",
        productId: String = iapProduct.productId,
        priceMicroCents: Long = 10_000L,
        currency: String = "USD",
        title: String = "title",
        description: String = "description"
    ) {
        val productDetails = buildProductDetails(
            productId = productId,
            name = "productName",
            price = priceMicroCents,
            currency = currency,
            title = title,
            description = description,
        )
        whenever(billingClientMock.queryProductDetails(any())).thenReturn(
            ProductDetailsResult(
                buildBillingResult(responseCode, debugMessage),
                listOf(productDetails)
            )
        )
    }

    fun setupBillingClient(connectionResult: BillingResult) {
        whenever(billingClientMock.startConnection(any())).thenAnswer {
            val listener = it.arguments[0] as BillingClientStateListener
            listener.onBillingSetupFinished(connectionResult)
            whenever(billingClientMock.isReady).thenReturn(connectionResult.isSuccess)
        }
        whenever(billingClientMock.isReady).thenReturn(false)
    }
}
