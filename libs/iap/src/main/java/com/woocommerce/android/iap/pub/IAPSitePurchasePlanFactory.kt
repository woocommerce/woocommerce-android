package com.woocommerce.android.iap.pub

import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        activity: AppCompatActivity,
        logWrapper: IAPLogWrapper,
    ): PurchaseWPComPlanActions = IAPPurchaseWPComPlanActionsImpl(activity, logWrapper)
}
