package com.woocommerce.android.iap.internal.core

import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.pub.IAPLogWrapper

internal object IAPManagerFactory {
    fun createIAPManager(
        activity: AppCompatActivity,
        logWrapper: IAPLogWrapper
    ): IAPManager {
        val iapOutMapper = IAPOutMapper()
        val iapPurchasesUpdatedListener = IAPPurchasesUpdatedListener(
            logWrapper,
        )
        val iapLifecycleObserver = IAPLifecycleObserver(
            activity,
            iapPurchasesUpdatedListener,
            logWrapper
        )
        val iapInMapper = IAPInMapper()
        return IAPManager(
            activity,
            iapLifecycleObserver,
            iapOutMapper,
            iapInMapper,
            iapPurchasesUpdatedListener,
            logWrapper,
        )
    }
}
