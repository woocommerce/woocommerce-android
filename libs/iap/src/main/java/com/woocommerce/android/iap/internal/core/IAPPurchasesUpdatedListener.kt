package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

internal class IAPPurchasesUpdatedListener(
    private val logWrapper: IAPLogWrapper,
) : PurchasesUpdatedListener {
    private var purchaseContinuation: Continuation<PurchasesResult>? = null

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated $billingResult $purchases")
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated: $responseCode $debugMessage")
        onPurchaseAvailable(PurchasesResult(billingResult, purchases.orEmpty()))
    }

    suspend fun getPurchaseResult() = suspendCoroutine<PurchasesResult> { purchaseContinuation = it }

    fun onPurchaseAvailable(purchasesResult: PurchasesResult) {
        purchaseContinuation?.resume(purchasesResult)
        purchaseContinuation = null
    }
}
