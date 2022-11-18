package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.mapNotNull

internal class IAPPurchasesUpdatedListener(
    private val logWrapper: IAPLogWrapper,
) : PurchasesUpdatedListener {
    private val _purchaseResult = MutableStateFlow<PurchasesResult?>(null)
    val purchaseResult: Flow<PurchasesResult> = _purchaseResult.mapNotNull { it }

    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated $billingResult $purchases")
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated: $responseCode $debugMessage")
        onPurchaseAvailable(PurchasesResult(billingResult, purchases.orEmpty()))
    }

    fun onPurchaseAvailable(purchasesResult: PurchasesResult) {
        _purchaseResult.value = purchasesResult
    }
}
