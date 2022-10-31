package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse.Error
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse.Success
import com.woocommerce.android.iap.internal.model.IAPProductType
import com.woocommerce.android.iap.internal.model.IAPPurchaseResult
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import com.woocommerce.android.iap.pub.model.IAPError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class IAPManager(
    private val iapBillingClientStateHandler: IAPBillingClientStateHandler,
    private val iapOutMapper: IAPOutMapper,
    private val iapInMapper: IAPInMapper,
    private val iapPurchasesUpdatedListener: IAPPurchasesUpdatedListener,
    private val logWrapper: IAPLogWrapper,
) {
    private val billingClient: IAPBillingClientWrapper
        get() = iapBillingClientStateHandler.billingClient

    private var purchaseStatusCheckerJob: Job? = null

    val purchaseWpComPlanResult: Flow<IAPPurchaseResult> = iapPurchasesUpdatedListener.purchaseWpComPlanResult.map {
        purchaseStatusCheckerJob?.cancel()
        mapPurchaseResultToIAPPurchaseResult(it)
    }

    fun connect() {
        iapBillingClientStateHandler.connectToIAPService()
    }

    fun disconnect() {
        iapBillingClientStateHandler.disconnectFromIAPService()
    }

    suspend fun fetchPurchases(iapProductType: IAPProductType): IAPPurchaseResult =
        withContext(Dispatchers.IO) {
            val purchasesResult = queryPurchases(iapProductType)

            if (purchasesResult.billingResult.isSuccess) {
                return@withContext handleFetchPurchasesSuccess(purchasesResult, iapProductType)
            } else {
                return@withContext IAPPurchaseResult.Error(
                    iapOutMapper.mapBillingResultErrorToBillingResultType(
                        purchasesResult.billingResult
                    )
                )
            }
        }

    fun startPurchase(activityWrapper: IAPActivityWrapper, productDetails: ProductDetails) {
        val flowParams = buildBillingFlowParams(productDetails)
        billingClient.launchBillingFlow(activityWrapper.activity, flowParams)
        purchaseStatusCheckerJob = startPeriodicPurchasesCheckJob(productDetails) {
            iapPurchasesUpdatedListener.onPurchaseAvailable(it)
        }
    }

    suspend fun fetchIAPProductDetails(iapProduct: IAPProduct): IAPProductDetailsResponse =
        withContext(Dispatchers.IO) {
            waitBillingClientInitialisation()
            when (val response = fetchProductDetails(iapProduct.productId, iapProduct.productType)) {
                is Success -> Success(response.productDetails)
                is Error -> Error(response.error)
            }
        }

    private fun startPeriodicPurchasesCheckJob(
        productDetails: ProductDetails,
        onPurchaseAvailable: (PurchasesResult) -> Unit,
    ) = CoroutineScope(Dispatchers.IO).launch {
        repeat(PURCHASE_STATE_CHECK_TIMES) {
            if (isActive) {
                delay(PURCHASE_STATE_CHECK_INTERVAL)
                // TODO INAPP support?
                val purchasesResult = queryPurchases(IAPProductType.SUBS)
                logWrapper.d(IAP_LOG_TAG, "Fetching purchases. Result ${purchasesResult.billingResult}")
                if (purchasesResult.billingResult.isSuccess &&
                    purchasesResult.purchasesList.firstOrNull {
                        it.products.contains(productDetails.productId)
                    }?.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    if (isActive) {
                        onPurchaseAvailable(purchasesResult)
                        cancel()
                    }
                }
            }
        }
    }

    private suspend fun mapPurchaseResultToIAPPurchaseResult(purchasesResult: PurchasesResult): IAPPurchaseResult {
        return if (purchasesResult.billingResult.isSuccess) {
            val purchase = purchasesResult.purchasesList.first()
            // TODO more than 1 product in a purchase
            // TODO INAPP support?
            when (val iapProductDetailsResponse = fetchProductDetails(purchase.products.first(), IAPProductType.SUBS)) {
                is Success -> {
                    IAPPurchaseResult.Success(
                        purchasesResult.purchasesList.map {
                            // TODO we might return purchase with wrong product details here
                            iapOutMapper.mapPurchaseWithProductDetailsToIAPPurchase(
                                it,
                                listOf(iapProductDetailsResponse.productDetails)
                            )
                        }
                    )
                }
                is Error -> IAPPurchaseResult.Error(iapProductDetailsResponse.error)
            }
        } else {
            IAPPurchaseResult.Error(
                iapOutMapper.mapBillingResultErrorToBillingResultType(
                    purchasesResult.billingResult
                )
            )
        }
    }

    private suspend fun fetchProductDetails(
        iapProductName: String,
        iapProductType: IAPProductType,
    ): IAPProductDetailsResponse =
        withContext(Dispatchers.IO) {
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(iapProductName)
                            .setProductType(iapInMapper.mapProductTypeToIAPProductType(iapProductType))
                            .build()
                    )
                ).build()
            val productDetailsResult = billingClient.queryProductDetails(params)
            logWrapper.d(
                IAP_LOG_TAG,
                "queryProductDetails result: ${productDetailsResult.billingResult}, " +
                    "${productDetailsResult.productDetailsList?.joinToString { ", " }}"
            )
            if (productDetailsResult.billingResult.isSuccess) {
                if (productDetailsResult.productDetailsList.isNullOrEmpty()) {
                    Error(IAPError.Billing.ItemUnavailable("Item $iapProductName not found"))
                } else {
                    Success(productDetailsResult.productDetailsList!!.first())
                }
            } else {
                Error(iapOutMapper.mapBillingResultErrorToBillingResultType(productDetailsResult.billingResult))
            }
        }

    private suspend fun handleFetchPurchasesSuccess(
        purchasesResult: PurchasesResult,
        iapProductType: IAPProductType
    ): IAPPurchaseResult {
        val purchasesProductDetailsResponses = purchasesResult.purchasesList.map {
            it to it.products.map { product -> fetchProductDetails(product, iapProductType) }
        }

        purchasesProductDetailsResponses.forEach {
            val errorQueryDetailsResponse = it.second.firstOrNull { it is Error }
            if (errorQueryDetailsResponse is Error) return IAPPurchaseResult.Error(errorQueryDetailsResponse.error)
        }

        return IAPPurchaseResult.Success(
            purchasesProductDetailsResponses.map {
                iapOutMapper.mapPurchaseWithProductDetailsToIAPPurchase(
                    it.first,
                    it.second.filterIsInstance<Success>().map { it.productDetails }
                )
            }
        )
    }

    private suspend fun waitBillingClientInitialisation() {
        iapBillingClientStateHandler.waitTillConnectionEstablished()
    }

    private fun buildBillingFlowParams(productDetails: ProductDetails): BillingFlowParams {
        val productDetailsParams = ProductDetailsParams.newBuilder()
            .setOfferToken(productDetails.firstOfferToken)
            .setProductDetails(productDetails)
            .build()
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
    }

    private suspend fun queryPurchases(iapProductType: IAPProductType): PurchasesResult {
        logWrapper.d(IAP_LOG_TAG, "Fetching purchases")
        waitBillingClientInitialisation()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(iapInMapper.mapProductTypeToIAPProductType(iapProductType))
        return billingClient.queryPurchasesAsync(params.build())
    }

    companion object {
        private const val PURCHASE_STATE_CHECK_INTERVAL = 10_000L
        private const val PURCHASE_STATE_CHECK_TIMES = 30
    }
}
