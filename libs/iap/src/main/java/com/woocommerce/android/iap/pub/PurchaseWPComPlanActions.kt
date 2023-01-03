package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult
import kotlinx.coroutines.flow.Flow
import java.io.Closeable

interface PurchaseWPComPlanActions : Closeable {
    fun getPurchaseWpComPlanResult(remoteSiteId: Long): Flow<WPComPurchaseResult>
    suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult
    suspend fun purchaseWPComPlan(activityWrapper: IAPActivityWrapper, remoteSiteId: Long)
    suspend fun fetchWPComPlanProduct(): WPComProductResult
}
