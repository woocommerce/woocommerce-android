package com.woocommerce.android.iap.internal.planpurchase

import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.currencyOfTheFirstPurchasedOffer
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iap.pub.model.IAPSupportedResult

private const val SUPPORTED_CURRENCY = "USD"

internal class IAPPurchaseWpComPlanSupportCheckerImpl(
    private val iapManager: IAPManager,
    private val iapProduct: IAPProduct = IAPProduct.WPPremiumPlanTesting,
) : PurchaseWpComPlanSupportChecker {
    init {
        iapManager.connect()
    }

    override suspend fun isIAPSupported(): IAPSupportedResult {
        return when (val response = iapManager.fetchIAPProductDetails(iapProduct)) {
            is IAPProductDetailsResponse.Success -> IAPSupportedResult.Success(isCurrencySupported(response))
            is IAPProductDetailsResponse.Error -> IAPSupportedResult.Error(response.error)
        }
    }

    override fun close() {
        iapManager.disconnect()
    }

    private fun isCurrencySupported(response: IAPProductDetailsResponse.Success) =
        SUPPORTED_CURRENCY.equals(response.productDetails.currencyOfTheFirstPurchasedOffer, ignoreCase = true)
}
