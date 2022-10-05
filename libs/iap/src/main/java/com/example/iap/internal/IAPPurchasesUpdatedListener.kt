package com.example.iap.internal

import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.example.iap.IAP_LOG_TAG
import com.example.iap.LogWrapper
import com.example.iap.model.IAPPurchaseResponse
import kotlinx.coroutines.flow.MutableSharedFlow

internal class IAPPurchasesUpdatedListener(
    private val logWrapper: LogWrapper,
    private val iapOutMapper: IAPOutMapper,
    private val purchasesFlow: MutableSharedFlow<IAPPurchaseResponse>,
) : PurchasesUpdatedListener {
    override fun onPurchasesUpdated(billingResult: BillingResult, purchases: List<Purchase>?) {
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated $billingResult $purchases")
        val responseCode = billingResult.responseCode
        val debugMessage = billingResult.debugMessage
        logWrapper.d(IAP_LOG_TAG, "onPurchasesUpdated: $responseCode $debugMessage")
        purchasesFlow.tryEmit(
            iapOutMapper.mapPurchaseResponseToIAPPurchaseResponse(
                billingResult,
                purchases
            )
        )
    }
}
