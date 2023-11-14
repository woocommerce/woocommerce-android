package com.woocommerce.android.iap.internal.planpurchase

import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.currencyOfTheFirstPurchasedOffer
import com.woocommerce.android.iap.internal.core.isProductAcknowledged
import com.woocommerce.android.iap.internal.core.isProductPurchased
import com.woocommerce.android.iap.internal.core.priceOfTheFirstPurchasedOfferInMicros
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.internal.model.IAPPurchase
import com.woocommerce.android.iap.internal.model.IAPPurchaseResult
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.PurchaseStatus
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComPlanProduct
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

internal class IAPPurchaseWPComPlanActionsImpl(
    private val purchaseWpComPlanHandler: IAPPurchaseWpComPlanHandler,
    private val iapManager: IAPManager,
    private val iapProduct: IAPProduct = IAPProduct.WPPremiumPlanTesting
) : PurchaseWPComPlanActions {
    private companion object {
        private const val MILLION = 1_000_000.0
    }

    init {
        iapManager.connect()
    }

    override fun getPurchaseWpComPlanResult(remoteSiteId: Long): Flow<WPComPurchaseResult> = merge(
        purchaseWpComPlanHandler.purchaseWpComProductResult,
        iapManager.iapPurchaseResult.map { purchaseWpComPlanHandler.handleNewPurchaseResultEvent(it, remoteSiteId) },
    )

    override suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult {
        return when (val response = iapManager.fetchPurchases(iapProduct.productType)) {
            is IAPPurchaseResult.Success -> {
                val purchases = response.purchases
                WPComIsPurchasedResult.Success(determinePurchaseStatus(purchases))
            }

            is IAPPurchaseResult.Error -> WPComIsPurchasedResult.Error(response.error)
        }
    }

    override suspend fun purchaseWPComPlan(activityWrapper: IAPActivityWrapper, remoteSiteId: Long) {
        purchaseWpComPlanHandler.purchaseWPComPlan(activityWrapper, iapProduct, remoteSiteId)
    }

    override suspend fun fetchWPComPlanProduct(): WPComProductResult {
        return when (val response = iapManager.fetchIAPProductDetails(iapProduct)) {
            is IAPProductDetailsResponse.Success -> WPComProductResult.Success(
                WPComPlanProduct(
                    localizedTitle = response.productDetails.title,
                    localizedDescription = response.productDetails.description,
                    price = response.productDetails.priceOfTheFirstPurchasedOfferInMicros / MILLION,
                    currency = response.productDetails.currencyOfTheFirstPurchasedOffer,
                )
            )

            is IAPProductDetailsResponse.Error -> WPComProductResult.Error(response.error)
        }
    }

    override fun close() {
        iapManager.disconnect()
    }

    private fun determinePurchaseStatus(purchases: List<IAPPurchase>?) =
        if (purchases.isProductPurchased(iapProduct) && purchases.isProductAcknowledged(iapProduct)) {
            PurchaseStatus.PURCHASED_AND_ACKNOWLEDGED
        } else if (purchases.isProductPurchased(iapProduct)) {
            PurchaseStatus.PURCHASED
        } else {
            PurchaseStatus.NOT_PURCHASED
        }
}
