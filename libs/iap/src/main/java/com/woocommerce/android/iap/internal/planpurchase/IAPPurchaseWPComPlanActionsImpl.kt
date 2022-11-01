package com.woocommerce.android.iap.internal.planpurchase

import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.currencyOfTheFirstPurchasedOffer
import com.woocommerce.android.iap.internal.core.priceOfTheFirstPurchasedOfferInMicros
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.internal.model.IAPPurchase
import com.woocommerce.android.iap.internal.model.IAPPurchaseResult
import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.network.model.CreateAndConfirmOrderResponse
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComPlanProduct
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

private val iapProduct = IAPProduct.WPPremiumPlanTesting

private const val SUPPORTED_CURRENCY = "USD"
private const val TEN_THOUSAND = 10_000
private const val APP_ID = "com.woocommerce.android"

internal class IAPPurchaseWPComPlanActionsImpl(
    private val iapMobilePayAPI: IAPMobilePayAPI,
    private val iapManager: IAPManager,
    private val remoteSiteId: Long,
) : PurchaseWPComPlanActions {

    init {
        iapManager.connect()
    }

    private val purchaseWpComPlanFetchingProductsError = MutableStateFlow<WPComPurchaseResult.Error?>(null)

    override val purchaseWpComPlanResult: Flow<WPComPurchaseResult> = merge(
        purchaseWpComPlanFetchingProductsError.mapNotNull { it },
        iapManager.iapPurchaseResult.map { mapPurchaseResultToWPComPurchaseResult(it) },
    )

    override suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult {
        return when (val response = iapManager.fetchPurchases(iapProduct.productType)) {
            is IAPPurchaseResult.Success -> WPComIsPurchasedResult.Success(
                isProductPurchased(
                    response.purchases,
                    iapProduct
                )
            )
            is IAPPurchaseResult.Error -> WPComIsPurchasedResult.Error(response.error)
        }
    }

    override suspend fun purchaseWPComPlan(activityWrapper: IAPActivityWrapper) {
        when (val response = iapManager.fetchIAPProductDetails(iapProduct)) {
            is IAPProductDetailsResponse.Success -> iapManager.startPurchase(activityWrapper, response.productDetails)
            is IAPProductDetailsResponse.Error -> {
                purchaseWpComPlanFetchingProductsError.value = WPComPurchaseResult.Error(response.error)
            }
        }
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

    private suspend fun mapPurchaseResultToWPComPurchaseResult(response: IAPPurchaseResult) = when (response) {
        is IAPPurchaseResult.Success -> {
            val purchase = response.purchases!!.first()
            confirmPurchaseOnBackend(remoteSiteId, purchase)
        }
        is IAPPurchaseResult.Error -> WPComPurchaseResult.Error(response.error)
    }

    private suspend fun confirmPurchaseOnBackend(
        remoteSiteId: Long,
        purchase: IAPPurchase
    ): WPComPurchaseResult {
        val apiResponse = iapMobilePayAPI.createAndConfirmOrder(
            remoteSiteId = remoteSiteId,
            productIdentifier = purchase.products.first().id,
            priceInCents = convertMicroUnitsToCents(purchase.products.first().price),
            currency = purchase.products.first().currency,
            purchaseToken = purchase.purchaseToken,
            appId = APP_ID,
        )
        return when (apiResponse) {
            is CreateAndConfirmOrderResponse.Success -> WPComPurchaseResult.Success
            CreateAndConfirmOrderResponse.Network -> WPComPurchaseResult.Error(
                IAPError.RemoteCommunication.Network
            )
            is CreateAndConfirmOrderResponse.Server ->
                WPComPurchaseResult.Error(IAPError.RemoteCommunication.Server(apiResponse.reason))
        }
    }

    private fun isCurrencySupported(response: IAPProductDetailsResponse.Success) =
        SUPPORTED_CURRENCY.equals(response.productDetails.currencyOfTheFirstPurchasedOffer, ignoreCase = true)

    private fun isProductPurchased(
        iapPurchases: List<IAPPurchase>?,
        iapProduct: IAPProduct
    ) = iapPurchases?.find { it.products.find { iapProduct.productId == it.id } != null }?.state ==
        IAPPurchase.State.PURCHASED

    private fun convertMicroUnitsToCents(priceInMicroUnits: Long) = (priceInMicroUnits / TEN_THOUSAND).toInt()
}
