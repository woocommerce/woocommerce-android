package com.woocommerce.android.iap.internal.core

import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.PurchasesUpdatedListener

internal class IAPBillingClientProvider {
    fun provideBillingClient(
        context: Context,
        listener: PurchasesUpdatedListener,
    ) = IAPBillingClientWrapper(
        BillingClient
            .newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases()
            .build()
    )
}
