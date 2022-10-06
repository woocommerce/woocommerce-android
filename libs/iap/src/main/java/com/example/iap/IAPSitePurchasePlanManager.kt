package com.example.iap

import com.example.iap.model.IAPProduct
import com.example.iap.model.IAPPurchaseResponse

interface IAPSitePurchasePlanManager {
    /**
     * This has to be called on every "onResume"
     * https://developer.android.com/google/play/billing/integrate
     */

    // TODO response type probably has to be more complex
    suspend fun isPlanPurchased(iapProduct: IAPProduct): Boolean

    suspend fun purchasePlan(iapProduct: IAPProduct): IAPPurchaseResponse
}
