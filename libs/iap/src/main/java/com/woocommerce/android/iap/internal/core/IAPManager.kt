package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.woocommerce.android.iap.internal.model.IAPBillingClientConnectionResult
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withContext

@Suppress("LongParameterList")
internal class IAPManager(
    private val iapBillingClientStateHandler: IAPBillingClientStateHandler,
    private val iapOutMapper: IAPOutMapper,
    private val iapInMapper: IAPInMapper,
    private val iapPurchasesUpdatedListener: IAPPurchasesUpdatedListener,
    private val billingFlowParamsBuilder: IAPBillingFlowParamsBuilder,
    private val periodicPurchaseStatusChecker: IAPPeriodicPurchaseStatusChecker,
    private val logWrapper: IAPLogWrapper,
) {
    private val billingClient: IAPBillingClientWrapper
        get() = iapBillingClientStateHandler.billingClient

    private var purchaseStatusCheckerJob: Job? = null

    private val purchaseError = MutableStateFlow<IAPPurchaseResult.Error?>(null)

    val iapPurchaseResult: Flow<IAPPurchaseResult> = merge(
        purchaseError.mapNotNull { it },
        iapPurchasesUpdatedListener.purchaseResult.map {
            purchaseStatusCheckerJob?.cancel()
            mapPurchaseResultToIAPPurchaseResult(it)
        }
    )

    fun connect() {
        iapBillingClientStateHandler.connectToIAPService()
    }

    fun disconnect() {
        iapBillingClientStateHandler.disconnectFromIAPService()
    }

    suspend fun fetchPurchases(iapProductType: IAPProductType): IAPPurchaseResult =
        withContext(Dispatchers.IO) {
            val connectionResult = getConnectionResult()
            if (connectionResult is IAPBillingClientConnectionResult.Error) {
                return@withContext IAPPurchaseResult.Error(connectionResult.errorType)
            }

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

    suspend fun startPurchase(activityWrapper: IAPActivityWrapper, productDetails: ProductDetails) {
        val connectionResult = getConnectionResult()
        if (connectionResult is IAPBillingClientConnectionResult.Error) {
            purchaseError.value = IAPPurchaseResult.Error(connectionResult.errorType)
            return
        }

        val flowParams = billingFlowParamsBuilder.buildBillingFlowParams(productDetails)
        billingClient.launchBillingFlow(activityWrapper.activity, flowParams)
        purchaseStatusCheckerJob = periodicPurchaseStatusChecker.startPeriodicPurchasesCheckJob(
            productDetails,
            { queryPurchases(it) },
        ) {
            iapPurchasesUpdatedListener.onPurchaseAvailable(it)
        }
    }

    suspend fun fetchIAPProductDetails(iapProduct: IAPProduct): IAPProductDetailsResponse =
        withContext(Dispatchers.IO) {
            val connectionResult = getConnectionResult()
            if (connectionResult is IAPBillingClientConnectionResult.Error) {
                return@withContext Error(connectionResult.errorType)
            }

            when (val response = fetchProductDetails(iapProduct.productId, iapProduct.productType)) {
                is Success -> Success(response.productDetails)
                is Error -> Error(response.error)
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

    private suspend fun getConnectionResult() = iapBillingClientStateHandler.waitTillConnectionEstablished()

    private suspend fun queryPurchases(iapProductType: IAPProductType): PurchasesResult {
        logWrapper.d(IAP_LOG_TAG, "Fetching purchases")
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(iapInMapper.mapProductTypeToIAPProductType(iapProductType))
            .build()
        return billingClient.queryPurchasesAsync(params)
    }
}
