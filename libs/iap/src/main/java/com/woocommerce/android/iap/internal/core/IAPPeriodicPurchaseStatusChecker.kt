package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesResult
import com.woocommerce.android.iap.internal.model.IAPProductType
import com.woocommerce.android.iap.pub.IAPLogWrapper
import com.woocommerce.android.iap.pub.IAP_LOG_TAG
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

internal class IAPPeriodicPurchaseStatusChecker(
    private val logWrapper: IAPLogWrapper,
) {
    fun startPeriodicPurchasesCheckJob(
        productDetails: ProductDetails,
        queryPurchases: suspend (IAPProductType) -> PurchasesResult,
        onPurchaseAvailable: (PurchasesResult) -> Unit,
    ) = CoroutineScope(Dispatchers.IO).launch {
        repeat(PURCHASE_STATE_CHECK_TIMES) {
            if (isActive) {
                delay(PURCHASE_STATE_CHECK_INTERVAL)
                // TODO INAPP support?
                val purchasesResult = queryPurchases(IAPProductType.SUBS)
                logWrapper.d(IAP_LOG_TAG, "Fetching purchases. Result ${purchasesResult.billingResult}")
                if (purchasesResult.billingResult.isSuccess &&
                    purchasesResult.purchasesList.firstOrNull {
                        it.products.contains(productDetails.productId)
                    }?.purchaseState == Purchase.PurchaseState.PURCHASED
                ) {
                    if (isActive) {
                        onPurchaseAvailable(purchasesResult)
                        cancel()
                    }
                }
            } else {
                return@launch
            }
        }
    }

    companion object {
        private const val PURCHASE_STATE_CHECK_INTERVAL = 10_000L
        private const val PURCHASE_STATE_CHECK_TIMES = 30
    }
}
