package com.woocommerce.android.iap.pub

import android.app.Application
import com.woocommerce.android.iap.BuildConfig
import com.woocommerce.android.iap.internal.core.IAPManagerFactory
import com.woocommerce.android.iap.internal.network.IAPMobilePayAPIStub
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWPComPlanActionsImpl
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanHandler
import com.woocommerce.android.iap.internal.planpurchase.IAPPurchaseWpComPlanSupportCheckerImpl
import com.woocommerce.android.iap.pub.network.IAPMobilePayAPI

object IAPSitePurchasePlanFactory {
    fun createIAPSitePurchasePlan(
        context: Application,
        remoteSiteId: Long,
        logWrapper: IAPLogWrapper,
        realMobilePayApiProvider: (String?) -> IAPMobilePayAPI,
    ): PurchaseWPComPlanActions {
        val iapManager = IAPManagerFactory.createIAPManager(context, logWrapper)
        val purchaseWpComPlanHandler = IAPPurchaseWpComPlanHandler(
            providerMobilePayAPI(logWrapper, realMobilePayApiProvider),
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

    private fun providerMobilePayAPI(
        logWrapper: IAPLogWrapper,
        realMobilePayApiProvider: (String?) -> IAPMobilePayAPI
    ): IAPMobilePayAPI =
        if (BuildConfig.IAP_TESTING_SANDBOX_URL.isNotEmpty()) {
            realMobilePayApiProvider(BuildConfig.IAP_TESTING_SANDBOX_URL)
        } else {
            if (BuildConfig.DEBUG) {
                IAPMobilePayAPIStub(logWrapper)
            } else {
                realMobilePayApiProvider(null)
            }
        }
}
