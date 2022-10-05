package com.example.iap.internal

import androidx.appcompat.app.AppCompatActivity
import com.example.iap.LogWrapper
import com.example.iap.model.IAPPurchaseResponse
import kotlinx.coroutines.flow.MutableSharedFlow

internal object IAPManagerFactory {
    fun createIAPManager(
        activity: AppCompatActivity,
        logWrapper: LogWrapper
    ): IAPManager {
        val iapOutMapper = IAPOutMapper()
        val purchasesFlow = MutableSharedFlow<IAPPurchaseResponse>(
            replay = 0,
            extraBufferCapacity = 0
        )
        val onPurchaseUpdated = IAPPurchasesUpdatedListener(
            logWrapper,
            iapOutMapper,
            purchasesFlow,
        )
        val iapLifecycleObserver = IAPLifecycleObserver(
            activity,
            onPurchaseUpdated,
            logWrapper
        )
        val iapInMapper = IAPInMapper()
        return IAPManager(
            activity,
            iapLifecycleObserver,
            iapOutMapper,
            iapInMapper,
            purchasesFlow,
        )
    }
}
