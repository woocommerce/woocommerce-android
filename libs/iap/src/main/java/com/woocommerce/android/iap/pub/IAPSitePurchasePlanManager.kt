package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.pub.model.IAPProduct
import com.woocommerce.android.iap.pub.model.IAPProductInfoResponse
import com.woocommerce.android.iap.pub.model.IAPPurchaseResponse

interface IAPSitePurchasePlanManager {
    /**
     * This has to be called on every "onResume"
     * https://developer.android.com/google/play/billing/integrate
     */

    // TODO response type probably has to be more complex
    suspend fun isPlanPurchased(iapProduct: IAPProduct): Boolean

    suspend fun purchasePlan(iapProduct: IAPProduct): IAPPurchaseResponse

    suspend fun fetchIapProductInfo(iapProduct: IAPProduct): IAPProductInfoResponse
}
