package com.example.iap.internal

import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryPurchasesAsync
import com.example.iap.internal.model.IAPProductDetailsResponse
import com.example.iap.internal.model.IAPProductDetailsResponse.Error
import com.example.iap.internal.model.IAPProductDetailsResponse.Success
import com.example.iap.model.BillingErrorType.ServiceDisconnected
import com.example.iap.model.IAPProduct
import com.example.iap.model.IAPProductType
import com.example.iap.model.IAPPurchaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IAPManager(
    private val activity: AppCompatActivity,
    private val iapLifecycleObserver: IAPLifecycleObserver,
    private val iapOutMapper: IAPOutMapper,
    private val iapInMapper: IAPInMapper,
    private val iapPurchasesUpdatedListener: IAPPurchasesUpdatedListener,
) {
    private val billingClient: BillingClient
        get() = iapLifecycleObserver.billingClient

    init {
        activity.lifecycle.addObserver(iapLifecycleObserver)
    }

    suspend fun fetchPurchases(iapProductType: IAPProductType): IAPPurchaseResponse =
        withContext(Dispatchers.IO) {
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(iapInMapper.mapProductTypeToIAPProductType(iapProductType))
            val purchasesResult = billingClient.queryPurchasesAsync(params.build())
            return@withContext iapOutMapper.mapPurchaseResponseToIAPPurchaseResponse(
                purchasesResult.billingResult,
                purchasesResult.purchasesList
            )
        }

    suspend fun startPurchase(iapProductType: IAPProduct): IAPPurchaseResponse {
        if (!billingClient.isReady) {
            return IAPPurchaseResponse.Error(ServiceDisconnected("BillingClient is not ready"))
        }

        return when (val iapProductDetailsResponse = queryProductDetails(iapProductType)) {
            is Success -> {
                val flowParams = buildBillingFlowParams(iapProductDetailsResponse)
                billingClient.launchBillingFlow(activity, flowParams)
                suspendCoroutine { iapPurchasesUpdatedListener.waitTillNextPurchaseEvent(it) }
            }
            is Error -> IAPPurchaseResponse.Error(iapProductDetailsResponse.errorType)
        }
    }

    private suspend fun queryProductDetails(iapProductType: IAPProduct) =
        withContext(Dispatchers.IO) {
            suspendCoroutine<IAPProductDetailsResponse> { cont ->
                val params = QueryProductDetailsParams.newBuilder()
                    .setProductList(
                        listOf(
                            QueryProductDetailsParams.Product.newBuilder()
                                .setProductId(iapProductType.value)
                                .setProductType(iapInMapper.mapProductTypeToIAPProductType(iapProductType.productType))
                                .build()
                        )
                    ).build()
                billingClient.queryProductDetailsAsync(params)
                { billingResult, productDetails ->
                    cont.resume(
                        iapOutMapper.mapProductDetailsResponseToIAPProductDetailsResponse(
                            billingResult,
                            productDetails
                        )
                    )
                }
            }
        }

    private fun buildBillingFlowParams(iapProductDetailsResponse: Success): BillingFlowParams {
        val productDetailsParams = ProductDetailsParams.newBuilder()
            .setProductDetails(iapProductDetailsResponse.productDetails)
            .build()
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
    }
}
