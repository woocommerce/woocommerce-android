package com.example.iap.internal.core

import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.example.iap.internal.model.IAPProductDetailsResponse
import com.example.iap.internal.model.IAPProductDetailsResponse.Error
import com.example.iap.internal.model.IAPProductDetailsResponse.Success
import com.example.iap.model.BillingErrorType
import com.example.iap.model.IAPProduct
import com.example.iap.model.IAPProductType
import com.example.iap.model.IAPPurchaseResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

            if (purchasesResult.isSuccess) {
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
        when (val iapProductDetailsResponse = queryProductDetails(iapProduct.name, iapProduct.productType)) {
            is Success -> {
                val flowParams = buildBillingFlowParams(iapProductDetailsResponse)
                billingClient.launchBillingFlow(activity, flowParams)
                suspendCoroutine { iapPurchasesUpdatedListener.waitTillNextPurchaseEvent(it) }
            }

            is Error -> IAPPurchaseResponse.Error(iapProductDetailsResponse.errorType)
        }

    private suspend fun queryProductDetails(
        iapProductName: String,
        iapProductType: IAPProductType,
    ): IAPProductDetailsResponse {
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
        return if (productDetailsResult.productDetailsList.isNullOrEmpty()) {
            Error(BillingErrorType.ItemUnavailable("Item $iapProductName not found"))
        } else {
            iapOutMapper.mapProductDetailsResultToIAPProductDetailsResponse(productDetailsResult)
        }
    }

    private suspend fun handleFetchPurchasesSuccess(
        purchasesResult: PurchasesResult,
        iapProductType: IAPProductType
    ): IAPPurchaseResponse {
        val purchasesProductDetailsResponses = purchasesResult.purchasesList.map {
            it to it.products.map { product -> queryProductDetails(product, iapProductType) }
        }

        purchasesProductDetailsResponses.forEach {
            val errorQueryDetailsResponse = it.second.firstOrNull { it is Error }
            if (errorQueryDetailsResponse is Error) return IAPPurchaseResponse.Error(
                errorQueryDetailsResponse.errorType
            )
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

    private fun buildBillingFlowParams(iapProductDetailsResponse: Success): BillingFlowParams {
        val productDetailsParams = ProductDetailsParams.newBuilder()
            .setProductDetails(iapProductDetailsResponse.productDetails)
            .build()
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams))
            .build()
    }

}
