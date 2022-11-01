package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.internal.core.IAPManagerFactory
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPI
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPIStub
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(logWrapper: IAPLogWrapper): PurchaseWPComPlanActions {
        val iapMobilePayAPI: IAPMobilePayAPI = IAPMobilePayAPIStub(logWrapper)
        val iapManager = IAPManagerFactory.createIAPManager(logWrapper)
        return IAPPurchaseWPComPlanActionsImpl(iapMobilePayAPI, iapManager)
    }
}
