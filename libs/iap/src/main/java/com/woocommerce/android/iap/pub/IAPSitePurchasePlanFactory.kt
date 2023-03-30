package com.woocommerce.android.iap.pub

import android.app.Application
import com.woocommerce.android.iap.internal.core.IAPManagerFactory
import com.woocommerce.android.iap.internal.network.ApiImplementationProvider
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanHandler
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanSupportCheckerImpl
import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        context: Application,
        logWrapper: IAPLogWrapper,
        realMobilePayApiProvider: (String?) -> IAPMobilePayAPI,
    ): PurchaseWPComPlanActions {
        val iapManager = IAPManagerFactory.createIAPManager(context, logWrapper)
        val apiImplementationProvider = ApiImplementationProvider()
        val purchaseWpComPlanHandler = IAPPurchaseWpComPlanHandler(
            apiImplementationProvider.providerMobilePayAPI(logWrapper, realMobilePayApiProvider),
            iapManager,
        )
        return IAPPurchaseWPComPlanActionsImpl(purchaseWpComPlanHandler, iapManager)
    }

    fun createIAPPurchaseWpComPlanSupportChecker(
        context: Application,
        logWrapper: IAPLogWrapper,
    ): PurchaseWpComPlanSupportChecker {
        val iapManager = IAPManagerFactory.createIAPManager(context, logWrapper)
        return IAPPurchaseWpComPlanSupportCheckerImpl(iapManager)
    }
}
