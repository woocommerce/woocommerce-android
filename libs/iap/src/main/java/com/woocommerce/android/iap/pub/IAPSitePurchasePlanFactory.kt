package com.woocommerce.android.iap.pub

import android.app.Application
import com.woocommerce.android.iap.internal.core.IAPManagerFactory
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPIStub
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanHandler

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        context: Application,
        remoteSiteId: Long,
        logWrapper: IAPLogWrapper,
    ): PurchaseWPComPlanActions {
        val iapMobilePayAPI: IAPMobilePayAPI = IAPMobilePayAPIStub(logWrapper)
        val iapManager = IAPManagerFactory.createIAPManager(context, logWrapper)
        val purchaseWpComPlanHandler = IAPPurchaseWpComPlanHandler(
            iapMobilePayAPI,
            iapManager,
        )
        return IAPPurchaseWPComPlanActionsImpl(purchaseWpComPlanHandler, iapManager, remoteSiteId)
    }
}
