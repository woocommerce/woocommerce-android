package com.woocommerce.android.iap.pub

import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPIStub
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        activity: AppCompatActivity,
        logWrapper: IAPLogWrapper,
    ): PurchaseWPComPlanActions {
        val iapMobilePayAPI: IAPMobilePayAPI = IAPMobilePayAPIStub(logWrapper)
        return IAPPurchaseWPComPlanActionsImpl(activity, logWrapper, iapMobilePayAPI)
    }
}
