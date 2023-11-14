package com.woocommerce.android.iap.internal.core

import android.app.Application
import com.woocommerce.android.iap.pub.IAPLogWrapper

internal object IAPManagerFactory {
    fun createIAPManager(
        context: Application,
        logWrapper: IAPLogWrapper
    ): IAPManager {
        val iapOutMapper = IAPOutMapper()
        val iapPurchasesUpdatedListener = IAPPurchasesUpdatedListener(logWrapper)
        val iapBillingClientStateHandler = IAPBillingClientStateHandler(
            IAPBillingClientProvider(context, iapPurchasesUpdatedListener),
            iapOutMapper,
            logWrapper,
        )
        val iapInMapper = IAPInMapper()
        return IAPManager(
            iapBillingClientStateHandler,
            iapOutMapper,
            iapInMapper,
            iapPurchasesUpdatedListener,
            IAPBillingFlowParamsBuilder(),
            IAPPeriodicPurchaseStatusChecker(logWrapper),
            logWrapper,
        )
    }
}
