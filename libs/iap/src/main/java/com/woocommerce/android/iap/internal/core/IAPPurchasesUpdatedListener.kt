package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import com.woocommerce.android.iap.pub.LogWrapper
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

internal class IAPPurchasesUpdatedListener(
    private val logWrapper: LogWrapper,
) : PurchasesUpdatedListener {
    private var purchaseContinuation: Continuation<PurchasesResult>? = null

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated $billingResult $purchases")
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated: $responseCode $debugMessage")
        purchaseContinuation?.resume(PurchasesResult(billingResult, purchases.orEmpty()))
        purchaseContinuation = null
    }

    fun waitTillNextPurchaseEvent(continuation: Continuation<PurchasesResult>) {
        purchaseContinuation = continuation
    }
}
