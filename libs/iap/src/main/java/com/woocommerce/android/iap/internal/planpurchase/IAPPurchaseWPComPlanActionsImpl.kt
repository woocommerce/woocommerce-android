package com.woocommerce.android.iap.internal.planpurchase

import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.currencyOfTheFirstPurchasedOffer
import com.woocommerce.android.iap.internal.core.isProductPurchased
import com.woocommerce.android.iap.internal.core.priceOfTheFirstPurchasedOfferInMicros
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.internal.model.IAPPurchaseResult
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComPlanProduct
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

private const val SUPPORTED_CURRENCY = "USD"

internal class IAPPurchaseWPComPlanActionsImpl(
    private val purchaseWpComPlanHandler: IAPPurchaseWpComPlanHandler,
    private val iapManager: IAPManager,
    private val remoteSiteId: Long,
    private val iapProduct: IAPProduct = IAPProduct.WPPremiumPlanTesting
) : PurchaseWPComPlanActions {
    @Volatile
    private var isPurchaseInProgress = false

    init {
        iapManager.connect()
    }

    override val purchaseWpComPlanResult: Flow<WPComPurchaseResult> = merge(
        purchaseWpComPlanHandler.purchaseWpComProductResult,
        iapManager.iapPurchaseResult.map { purchaseWpComPlanHandler.handleNewPurchaseResultEvent(it, remoteSiteId) },
    )

    override suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult {
        return when (val response = iapManager.fetchPurchases(iapProduct.productType)) {
            is IAPPurchaseResult.Success -> WPComIsPurchasedResult.Success(
                response.purchases.isProductPurchased(iapProduct)
            )
            is IAPPurchaseResult.Error -> WPComIsPurchasedResult.Error(response.error)
        }
    }

    override suspend fun purchaseWPComPlan(activityWrapper: IAPActivityWrapper) {
        if (isPurchaseInProgress) return

        isPurchaseInProgress = true
        purchaseWpComPlanHandler.purchaseWPComPlan(activityWrapper, iapProduct, remoteSiteId)
        isPurchaseInProgress = false
    }

    override suspend fun fetchWPComPlanProduct(): WPComProductResult {
        return when (val response = iapManager.fetchIAPProductDetails(iapProduct)) {
            is IAPProductDetailsResponse.Success -> WPComProductResult.Success(
                WPComPlanProduct(
                    localizedTitle = response.productDetails.title,
                    localizedDescription = response.productDetails.description,
                    price = response.productDetails.priceOfTheFirstPurchasedOfferInMicros,
                    currency = response.productDetails.currencyOfTheFirstPurchasedOffer,
                )
            )
            is IAPProductDetailsResponse.Error -> WPComProductResult.Error(response.error)
        }
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
