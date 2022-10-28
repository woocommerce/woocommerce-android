package com.woocommerce.android.iap.internal.core

import android.app.Application
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

internal class IAPBillingClientProvider(
    private val context: Application,
    private val listener: PurchasesUpdatedListener,
) {
    fun provideBillingClient() = IAPBillingClientWrapper(
        BillingClient
            .newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases()
            .build()
    )
}
