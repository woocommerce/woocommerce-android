package com.example.iap.internal.planpurchase

import androidx.appcompat.app.AppCompatActivity
import com.example.iap.IAPSitePurchasePlanManager
import com.example.iap.IAPStore
import com.example.iap.LogWrapper
import com.example.iap.internal.core.IAPManagerFactory
import com.example.iap.model.IAPProduct
import com.example.iap.model.IAPPurchaseResponse
import com.example.iap.model.IAPPurchaseResponse.Error
import com.example.iap.model.IAPPurchaseResponse.Success

internal class IAPManagerPlanSitePurchaseManagerImpl(
    private val iapStore: IAPStore,
    activity: AppCompatActivity,
    logWrapper: LogWrapper,
) : IAPSitePurchasePlanManager {
    private val iapManager by lazy { IAPManagerFactory.createIAPManager(activity, logWrapper) }

    override suspend fun isPlanPurchased(iapProduct: IAPProduct): Boolean {
        return when (val response = iapManager.fetchPurchases(iapProduct.productType)) {
            is Success -> {
                val isPurchased = isProductPurchased(response, iapProduct)
                if (isPurchased) {
                    iapStore.confirmOrderOnServer(iapProduct)
                    true
                } else {
                    false
                }
            }
            is Error -> false
        }
    }

    override suspend fun purchasePlan(iapProduct: IAPProduct): IAPPurchaseResponse {
        when (val response = iapManager.startPurchase(iapProduct)) {
            is Success -> TODO()
            is Error -> TODO()
        }
    }

    private fun isProductPurchased(
        response: Success,
        iapProduct: IAPProduct
    ) = response.purchases?.find { it.products.find { iapProduct.name == it.name } != null } != null
}
