package com.woocommerce.android.iap.internal.core

import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import java.util.Collections
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class IAPLifecycleObserver(
    private val onPurchaseUpdated: PurchasesUpdatedListener,
    private val billingClientProvider: IAPBillingClientProvider,
    private val logWrapper: IAPLogWrapper
) : DefaultLifecycleObserver {
    private val connectionEstablishingContinuations = Collections.synchronizedList(mutableListOf<Continuation<Unit>>())

    lateinit var billingClient: IAPBillingClientWrapper
    lateinit var activity: AppCompatActivity

    fun initBillingClient(activity: AppCompatActivity) {
        this.activity = activity
        billingClient = billingClientProvider.provideBillingClient(activity, onPurchaseUpdated)
        activity.lifecycle.addObserver(this)
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
        if (this.activity.isDestroyed) {
            it.resumeWithException(
                IllegalStateException(
                    "Activity is destroyed. Make sure IAPManager::initIAP in every onCreate"
                )
            )
        } else if (billingClient.isReady) {
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
        synchronized(connectionEstablishingContinuations) {
            connectionEstablishingContinuations.forEach {
                it.resume(Unit)
            }
            connectionEstablishingContinuations.clear()
        }
    }
}
