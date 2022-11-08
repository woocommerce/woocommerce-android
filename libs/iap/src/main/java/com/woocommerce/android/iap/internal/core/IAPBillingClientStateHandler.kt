package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.woocommerce.android.iap.internal.model.IAPBillingClientConnectionResult
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import com.woocommerce.android.iap.pub.model.IAPError
import java.util.Collections
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IAPBillingClientStateHandler(
    billingClientProvider: IAPBillingClientProvider,
    private val mapper: IAPOutMapper,
    private val logWrapper: IAPLogWrapper
) {
    private val connectionEstablishingContinuations = Collections.synchronizedList(
        mutableListOf<Continuation<IAPBillingClientConnectionResult>>()
    )

    @Volatile
    private var connectionResult: IAPBillingClientConnectionResult? = null

    val billingClient = billingClientProvider.provideBillingClient()

    fun connectToIAPService() {
        if (!billingClient.isReady) {
            connectionResult = null
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) {
                    connectionResult = if (billingResult.isSuccess) {
                        IAPBillingClientConnectionResult.Success
                    } else {
                        IAPBillingClientConnectionResult.Error(
                            mapper.mapBillingResultErrorToBillingResultType(billingResult)
                        )
                    }
                    releaseWaiters(connectionResult!!)
                    logWrapper.d(IAP_LOG_TAG, "BillingClient setup finished with result $billingResult")
                }

                override fun onBillingServiceDisconnected() {
                    connectionResult = IAPBillingClientConnectionResult.Error(
                        IAPError.Billing.ServiceDisconnected(
                            "Billing service disconnected"
                        )
                    )

                    releaseWaiters(connectionResult!!)
                    logWrapper.d(IAP_LOG_TAG, "BillingClient disconnected")
                }
            })
        }
    }

    fun disconnectFromIAPService() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        } else {
            logWrapper.d(IAP_LOG_TAG, "BillingClient is not connected")
        }
    }

    suspend fun waitTillConnectionEstablished() = suspendCoroutine<IAPBillingClientConnectionResult> {
        connectionEstablishingContinuations.add(it)
        when (connectionResult) {
            IAPBillingClientConnectionResult.Success,
            is IAPBillingClientConnectionResult.Error -> releaseWaiters(connectionResult!!)
            null -> {
                // waiting for connection result
            }
        }
    }

    private fun releaseWaiters(connectionResult: IAPBillingClientConnectionResult) {
        synchronized(connectionEstablishingContinuations) {
            connectionEstablishingContinuations.forEach {
                it.resume(connectionResult)
            }
            connectionEstablishingContinuations.clear()
        }
    }
}
