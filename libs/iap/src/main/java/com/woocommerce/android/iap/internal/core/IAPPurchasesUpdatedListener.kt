package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.woocommerce.android.iap.public.IAP_LOG_TAG
import com.woocommerce.android.iap.public.LogWrapper
import com.woocommerce.android.iap.public.model.IAPPurchaseResponse
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

internal class IAPPurchasesUpdatedListener(
    private val logWrapper: LogWrapper,
    private val iapOutMapper: IAPOutMapper,
) : PurchasesUpdatedListener {
    private var purchaseContinuation: Continuation<IAPPurchaseResponse>? = null

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated $billingResult $purchases")
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated: $responseCode $debugMessage")
        purchaseContinuation?.resume(
            iapOutMapper.mapPurchaseResultToIAPPurchaseResponse(
                billingResult,
                purchases
            )
        )
        purchaseContinuation = null
    }

    fun waitTillNextPurchaseEvent(continuation: Continuation<IAPPurchaseResponse>) {
        purchaseContinuation = continuation
    }
}
