package com.woocommerce.android.iap.public

import com.woocommerce.android.iap.public.model.IAPProduct
import com.woocommerce.android.iap.public.model.IAPPurchaseResponse

interface IAPSitePurchasePlanManager {
    /**
     * This has to be called on every "onResume"
     * https://developer.android.com/google/play/billing/integrate
     */

    // TODO response type probably has to be more complex
    suspend fun isPlanPurchased(iapProduct: IAPProduct): Boolean

    suspend fun purchasePlan(iapProduct: IAPProduct): IAPPurchaseResponse
}
