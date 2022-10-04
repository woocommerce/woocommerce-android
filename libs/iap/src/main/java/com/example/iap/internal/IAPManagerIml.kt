package com.example.iap.internal

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.example.iap.BillingServiceDisconnectedError
import com.example.iap.IAPManager
import com.example.iap.IAPPurchaseState
import com.example.iap.IAPPurchaseState.Unknown
import com.example.iap.IAP_LOG_TAG
import com.example.iap.LogWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class IAPManagerIml(
    private val context: Context,
    private val logWrapper: LogWrapper,
) : IAPManager {
    private lateinit var bp: BillingClient

    override val iapPurchaseState: StateFlow<IAPPurchaseState> = MutableStateFlow(Unknown)

    override suspend fun connectToIAPService() {
        withContext(Dispatchers.IO) {
            suspendCancellableCoroutine<Unit> { cont ->
                bp = BillingClient
                    .newBuilder(context)
                    .setListener(::onPurchaseUpdated)
                    .enablePendingPurchases()
                    .build()
                bp.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(billingResult: BillingResult) {
                        if (cont.isActive) cont.resume(Unit)
                    }

                    override fun onBillingServiceDisconnected() {
                        logWrapper.d(IAP_LOG_TAG, "onBillingServiceDisconnected")
                        if (cont.isActive) cont.resumeWithException(BillingServiceDisconnectedError())
                    }
                })
            }
        }
    }

    override suspend fun disconnectFromIAPService() {
        bp.endConnection()
    }

    private fun onPurchaseUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated $billingResult $purchases")
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated: $responseCode $debugMessage")
    }
}
