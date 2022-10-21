package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult

interface PurchaseWPComPlanActions {
    suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult
    suspend fun purchaseWPComPlan(remoteSiteId: Long): WPComPurchaseResult
    suspend fun fetchWPComPlanProduct(): WPComProductResult
    suspend fun isIAPSupported(): IAPSupportedResult
}
