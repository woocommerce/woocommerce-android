package com.woocommerce.android.iap.internal.core

import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse.Error
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse.Success
import com.woocommerce.android.iap.internal.model.IAPProductType
import com.woocommerce.android.iap.internal.model.IAPPurchaseResponse
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import com.woocommerce.android.iap.pub.model.IAPError
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class IAPManager(
    private val activity: AppCompatActivity,
    private val iapLifecycleObserver: IAPLifecycleObserver,
    private val iapOutMapper: IAPOutMapper,
    private val iapInMapper: IAPInMapper,
    private val iapPurchasesUpdatedListener: IAPPurchasesUpdatedListener,
    private val logWrapper: IAPLogWrapper,
) {
    private val billingClient: BillingClient
        get() = iapLifecycleObserver.billingClient

    init {
        activity.lifecycle.addObserver(iapLifecycleObserver)
    }

    suspend fun fetchPurchases(iapProductType: IAPProductType): IAPPurchaseResponse =
        withContext(Dispatchers.IO) {
            val purchasesResult = queryPurchases(iapProductType)

            if (purchasesResult.billingResult.isSuccess) {
                return@withContext handleFetchPurchasesSuccess(purchasesResult, iapProductType)
            } else {
                return@withContext IAPPurchaseResponse.Error(
                    iapOutMapper.mapBillingResultErrorToBillingResultType(
                        purchasesResult.billingResult
                    )
                )
            }
        }

    suspend fun startPurchase(iapProduct: IAPProduct): IAPPurchaseResponse =
        when (val iapProductDetailsResponse = fetchProductDetails(iapProduct.productId, iapProduct.productType)) {
            is Success -> {
                val flowParams = buildBillingFlowParams(iapProductDetailsResponse.productDetails)
                billingClient.launchBillingFlow(activity, flowParams)

                startPeriodicPurchasesCheckJob(iapProduct) { iapPurchasesUpdatedListener.onPurchaseAvailable(it) }
                val purchasesResult = iapPurchasesUpdatedListener.getPurchaseResult()
                if (purchasesResult.billingResult.isSuccess) {
                    IAPPurchaseResponse.Success(
                        purchasesResult.purchasesList.map {
                            // TODO we might return purchase with wrong product details here
                            iapOutMapper.mapPurchaseWithProductDetailsToIAPPurchase(
                                it,
                                listOf(iapProductDetailsResponse.productDetails)
                            )
                        }
                    )
                } else {
                    IAPPurchaseResponse.Error(
                        iapOutMapper.mapBillingResultErrorToBillingResultType(
                            purchasesResult.billingResult
                        )
                    )
                }
            }

            is Error -> IAPPurchaseResponse.Error(iapProductDetailsResponse.error)
        }

    suspend fun fetchIAPProductDetails(iapProduct: IAPProduct): IAPProductDetailsResponse =
        withContext(Dispatchers.IO) {
            waitBillingClientInitialisation()
            when (val response = fetchProductDetails(iapProduct.productId, iapProduct.productType)) {
                is Success -> Success(response.productDetails)
                is Error -> Error(response.error)
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
    ): IAPPurchaseResponse {
        val purchasesProductDetailsResponses = purchasesResult.purchasesList.map {
            it to it.products.map { product -> fetchProductDetails(product, iapProductType) }
        }

        purchasesProductDetailsResponses.forEach {
            val errorQueryDetailsResponse = it.second.firstOrNull { it is Error }
            if (errorQueryDetailsResponse is Error) return IAPPurchaseResponse.Error(errorQueryDetailsResponse.error)
        }

        return IAPPurchaseResponse.Success(
            purchasesProductDetailsResponses.map {
                iapOutMapper.mapPurchaseWithProductDetailsToIAPPurchase(
                    it.first,
                    it.second.filterIsInstance<Success>().map { it.productDetails }
                )
            }
        )
    }

    private suspend fun waitBillingClientInitialisation() {
        iapLifecycleObserver.waitTillConnectionEstablished()
    }

    private fun buildBillingFlowParams(productDetails: ProductDetails): BillingFlowParams {
        val productDetailsParams = ProductDetailsParams.newBuilder()
            // TODO support for multiple offers?
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

    private fun startPeriodicPurchasesCheckJob(
        iapProduct: IAPProduct,
        onPurchaseAvailable: (PurchasesResult) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            repeat(PURCHASE_STATE_CHECK_TIMES) {
                if (isActive) {
                    delay(PURCHASE_STATE_CHECK_INTERVAL)
                    val purchasesResult = queryPurchases(iapProduct.productType)
                    logWrapper.d(IAP_LOG_TAG, "Fetching purchases. Result ${purchasesResult.billingResult}")
                    if (purchasesResult.billingResult.isSuccess &&
                        purchasesResult.purchasesList.firstOrNull {
                            it.products.contains(iapProduct.productId)
                        }?.purchaseState == Purchase.PurchaseState.PURCHASED
                    ) {
                        onPurchaseAvailable(purchasesResult)
                        cancel()
                    }
                }
            }
        }
    }

    companion object {
        private const val PURCHASE_STATE_CHECK_INTERVAL = 10_000L
        private const val PURCHASE_STATE_CHECK_TIMES = 30
    }
}
