package com.woocommerce.android.iap.internal.planpurchase

import com.woocommerce.android.iap.internal.core.IAPManager
import com.woocommerce.android.iap.internal.core.findPurchaseWithProduct
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.internal.model.IAPPurchase
import com.woocommerce.android.iap.internal.model.IAPPurchaseResult
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.network.model.CreateAndConfirmOrderResponse
import com.woocommerce.android.iap.pub.IAPActivityWrapper
import com.woocommerce.android.iap.pub.model.IAPError
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull

internal class IAPPurchaseWpComPlanHandler(
    private val iapMobilePayAPI: IAPMobilePayAPI,
    private val iapManager: IAPManager,
) {
    private val _purchaseWpComProductResult = MutableStateFlow<WPComPurchaseResult?>(null)
    val purchaseWpComProductResult: Flow<WPComPurchaseResult> = _purchaseWpComProductResult.mapNotNull { it }

    suspend fun purchaseWPComPlan(
        activityWrapper: IAPActivityWrapper,
        iapProduct: IAPProduct,
        remoteSiteId: Long
    ) {
        _purchaseWpComProductResult.value = null

        when (val response = iapManager.fetchPurchases(iapProduct.productType)) {
            is IAPPurchaseResult.Success -> {
                val existentPurchase = response.purchases.findPurchaseWithProduct(iapProduct)
                if (existentPurchase != null && existentPurchase.state == IAPPurchase.State.PURCHASED) {
                    handleExistingPurchase(existentPurchase, remoteSiteId)
                    return
                }
            }
            is IAPPurchaseResult.Error -> {
                _purchaseWpComProductResult.value = WPComPurchaseResult.Error(response.error)
                return
            }
        }

        when (val response = iapManager.fetchIAPProductDetails(iapProduct)) {
            is IAPProductDetailsResponse.Success -> iapManager.startPurchase(activityWrapper, response.productDetails)
            is IAPProductDetailsResponse.Error -> {
                _purchaseWpComProductResult.value = WPComPurchaseResult.Error(response.error)
            }
        }
    }

    private suspend fun handleExistingPurchase(purchaseWithProduct: IAPPurchase, remoteSiteId: Long) {
        _purchaseWpComProductResult.value = if (purchaseWithProduct.isAcknowledged) {
            WPComPurchaseResult.Success
        } else {
            when (val confirmOrderResponse = confirmPurchaseOnBackend(remoteSiteId, purchaseWithProduct)) {
                WPComPurchaseResult.Success -> WPComPurchaseResult.Success
                is WPComPurchaseResult.Error -> WPComPurchaseResult.Error(confirmOrderResponse.errorType)
            }
        }
    }

    suspend fun handleNewPurchaseResultEvent(response: IAPPurchaseResult, remoteSiteId: Long) = when (response) {
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

    private fun convertMicroUnitsToCents(priceInMicroUnits: Long) = (priceInMicroUnits / TEN_THOUSAND).toInt()

    companion object {
        private const val TEN_THOUSAND = 10_000
        private const val APP_ID = "com.woocommerce.android"
    }
}
