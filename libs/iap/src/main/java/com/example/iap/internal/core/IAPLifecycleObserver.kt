package com.example.iap.internal.core

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.example.iap.IAP_LOG_TAG
import com.example.iap.LogWrapper

internal class IAPLifecycleObserver(
    private val activity: Activity,
    private val onPurchaseUpdated: PurchasesUpdatedListener,
    private val logWrapper: LogWrapper,
) : DefaultLifecycleObserver {
    val billingClient by lazy {
        BillingClient
            .newBuilder(activity)
            .setListener(onPurchaseUpdated)
            .enablePendingPurchases()
            .build()
    }

    override fun onCreate(owner: LifecycleOwner) {
        connectToIAPService()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        disconnectFromIAPService()
    }

    private fun connectToIAPService() {
        if (!billingClient.isReady) {
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    logWrapper.d(IAP_LOG_TAG, "BillingClient setup finished")
                }

                override fun onBillingServiceDisconnected() {
                    logWrapper.d(IAP_LOG_TAG, "BillingClient disconnected")
                }
            })
        }
    }

    private fun disconnectFromIAPService() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        } else {
            logWrapper.d(IAP_LOG_TAG, "BillingClient is not connected")
        }
    }
}
