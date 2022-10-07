package com.woocommerce.android.iap.internal.core

import androidx.appcompat.app.AppCompatActivity
import com.woocommerce.android.iap.public.LogWrapper

internal object IAPManagerFactory {
    fun createIAPManager(
        activity: AppCompatActivity,
        logWrapper: LogWrapper
    ): IAPManager {
        val iapOutMapper = IAPOutMapper()
        val iapPurchasesUpdatedListener = IAPPurchasesUpdatedListener(
            logWrapper,
            iapOutMapper,
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
        )
    }
}
