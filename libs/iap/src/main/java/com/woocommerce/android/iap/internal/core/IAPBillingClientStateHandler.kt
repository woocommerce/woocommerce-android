package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import java.util.Collections
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IAPBillingClientStateHandler(
    billingClientProvider: IAPBillingClientProvider,
    private val logWrapper: IAPLogWrapper
) {
    private val connectionEstablishingContinuations = Collections.synchronizedList(mutableListOf<Continuation<Unit>>())

    val billingClient = billingClientProvider.provideBillingClient()

    fun connectToIAPService() {
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

    fun disconnectFromIAPService() {
        releaseWaiters()
        if (billingClient.isReady) {
            billingClient.endConnection()
        } else {
            logWrapper.d(IAP_LOG_TAG, "BillingClient is not connected")
        }
    }

    suspend fun waitTillConnectionEstablished() = suspendCoroutine<Unit> {
        if (billingClient.isReady) {
            it.resume(Unit)
        } else {
            connectionEstablishingContinuations.add(it)
        }
    }

    private fun releaseWaiters() {
        synchronized(connectionEstablishingContinuations) {
            connectionEstablishingContinuations.forEach {
                it.resume(Unit)
            }
            connectionEstablishingContinuations.clear()
        }
    }
}
