package com.woocommerce.android.iap.public

import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.internal.planpurchase.IAPManagerPlanSitePurchaseManagerImpl

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        iapStore: IAPStore,
        activity: AppCompatActivity,
        logWrapper: LogWrapper,
    ): IAPSitePurchasePlanManager = IAPManagerPlanSitePurchaseManagerImpl(iapStore, activity, logWrapper)
}
