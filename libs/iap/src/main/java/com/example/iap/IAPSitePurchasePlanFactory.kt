package com.example.iap

import androidx.appcompat.app.AppCompatActivity
import com.example.iap.internal.planpurchase.IAPManagerPlanSitePurchaseManagerImpl

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        iapStore: IAPStore,
        activity: AppCompatActivity,
        logWrapper: LogWrapper,
    ): IAPSitePurchasePlanManager = IAPManagerPlanSitePurchaseManagerImpl(iapStore, activity, logWrapper)
}
