package com.woocommerce.android.iap.pub

import android.app.Application
import com.woocommerce.android.iap.internal.core.IAPManagerFactory
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanHandler
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanSupportCheckerImpl
import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        context: Application,
        remoteSiteId: Long,
        mobilePayAPI: IAPMobilePayAPI,
        logWrapper: IAPLogWrapper,
    ): PurchaseWPComPlanActions {
        val iapManager = IAPManagerFactory.createIAPManager(context, logWrapper)
        val purchaseWpComPlanHandler = IAPPurchaseWpComPlanHandler(
            mobilePayAPI,
            iapManager,
        )
        return IAPPurchaseWPComPlanActionsImpl(purchaseWpComPlanHandler, iapManager, remoteSiteId)
    }

    fun createIAPPurchaseWpComPlanSupportChecker(
        context: Application,
        logWrapper: IAPLogWrapper,
    ): PurchaseWpComPlanSupportChecker {
        val iapManager = IAPManagerFactory.createIAPManager(context, logWrapper)
        return IAPPurchaseWpComPlanSupportCheckerImpl(iapManager)
    }
}
