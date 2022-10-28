package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import java.io.Closeable

interface PurchaseWPComPlanActions : Closeable {
    suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult
    suspend fun purchaseWPComPlan(activityWrapper: IAPActivityWrapper, remoteSiteId: Long): WPComPurchaseResult
    suspend fun fetchWPComPlanProduct(): WPComProductResult
    suspend fun isIAPSupported(): IAPSupportedResult
}
