package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface PurchaseWPComPlanActions : Closeable {
    val purchaseWpComPlanResult: Flow<WPComPurchaseResult>

    suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult
    suspend fun purchaseWPComPlan(activityWrapper: IAPActivityWrapper)
    suspend fun fetchWPComPlanProduct(): WPComProductResult
    suspend fun isIAPSupported(): IAPSupportedResult
}
