package com.woocommerce.android.iap.internal.core

import com.woocommerce.android.iap.pub.IAPLogWrapper

internal object IAPManagerFactory {
    fun createIAPManager(logWrapper: IAPLogWrapper): IAPManager {
        val iapOutMapper = IAPOutMapper()
        val iapPurchasesUpdatedListener = IAPPurchasesUpdatedListener(
            logWrapper,
        )
        val iapLifecycleObserver = IAPLifecycleObserver(
            iapPurchasesUpdatedListener,
            logWrapper
        )
        val iapInMapper = IAPInMapper()
        return IAPManager(
            iapLifecycleObserver,
            iapOutMapper,
            iapInMapper,
            iapPurchasesUpdatedListener,
            logWrapper,
        )
    }
}
