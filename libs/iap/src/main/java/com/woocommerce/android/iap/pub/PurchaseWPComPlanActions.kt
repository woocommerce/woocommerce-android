package com.woocommerce.android.iap.pub

import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.internal.model.IAPSupportedResult
import com.woocommerce.android.iap.pub.model.WPComIsPurchasedResult
import com.woocommerce.android.iap.pub.model.WPComProductResult
import com.woocommerce.android.iap.pub.model.WPComPurchaseResult

interface PurchaseWPComPlanActions {
    /**
     * Has to be called for every instance of activity, e.g. from `onCreate` method
     */
    fun initIAPWithNewActivity(activity: AppCompatActivity)

    suspend fun isWPComPlanPurchased(): WPComIsPurchasedResult
    suspend fun purchaseWPComPlan(remoteSiteId: Long): WPComPurchaseResult
    suspend fun fetchWPComPlanProduct(): WPComProductResult
    suspend fun isIAPSupported(): IAPSupportedResult
}
