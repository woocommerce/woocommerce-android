package com.woocommerce.android.iap.pub

import android.app.Application
import com.woocommerce.android.iap.internal.core.IAPManagerFactory
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPIStub
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        context: Application,
        remoteSiteId: Long,
        logWrapper: IAPLogWrapper,
    ): PurchaseWPComPlanActions {
        val iapMobilePayAPI: IAPMobilePayAPI = IAPMobilePayAPIStub(logWrapper)
        val iapManager = IAPManagerFactory.createIAPManager(context, logWrapper)
        return IAPPurchaseWPComPlanActionsImpl(iapMobilePayAPI, iapManager, remoteSiteId)
    }
}
