package com.woocommerce.android.iap.internal.planpurchase

import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.internal.core.IAPManagerFactory
import com.woocommerce.android.iap.public.IAPSitePurchasePlanManager
import com.woocommerce.android.iap.public.IAPStore
import com.woocommerce.android.iap.public.LogWrapper
import com.woocommerce.android.iap.public.model.IAPProduct
import com.woocommerce.android.iap.public.model.IAPPurchaseResponse
import com.woocommerce.android.iap.public.model.IAPPurchaseResponse.Error
import com.woocommerce.android.iap.public.model.IAPPurchaseResponse.Success

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
