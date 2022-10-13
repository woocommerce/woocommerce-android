package com.woocommerce.android.iap.internal.core

import android.app.Activity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import java.util.Collections
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IAPLifecycleObserver(
    private val activity: Activity,
    private val onPurchaseUpdated: PurchasesUpdatedListener,
    private val logWrapper: IAPLogWrapper,
) : DefaultLifecycleObserver {
    private val connectionEstablishingContinuations = Collections.synchronizedList(mutableListOf<Continuation<Unit>>())

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
                    releaseWaiters()
                    logWrapper.d(IAP_LOG_TAG, "BillingClient setup finished")
                }

                override fun onBillingServiceDisconnected() {
                    releaseWaiters()
                    logWrapper.d(IAP_LOG_TAG, "BillingClient disconnected")
                }
            })
        }
    }

    suspend fun waitTillConnectionEstablished() = suspendCoroutine<Unit> {
        if (billingClient.isReady) {
            it.resume(Unit)
        } else {
            connectionEstablishingContinuations.add(it)
        }
    }

    private fun disconnectFromIAPService() {
        releaseWaiters()
        if (billingClient.isReady) {
            billingClient.endConnection()
        } else {
            logWrapper.d(IAP_LOG_TAG, "BillingClient is not connected")
        }
    }

    private fun releaseWaiters() {
        connectionEstablishingContinuations.forEach {
            it.resume(Unit)
        }
        connectionEstablishingContinuations.clear()
    }
}
