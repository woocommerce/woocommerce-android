package com.woocommerce.android.iap.internal.core

import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.PurchasesResult
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync

internal class IAPBillingClientWrapper(val billingClient: BillingClient) {
    val isReady: Boolean
        get() = billingClient.isReady

    fun startConnection(billingClientStateListener: BillingClientStateListener) {
        billingClient.startConnection(billingClientStateListener)
    }

    fun endConnection() {
        billingClient.endConnection()
    }

    fun launchBillingFlow(activity: AppCompatActivity, flowParams: BillingFlowParams) {
        billingClient.launchBillingFlow(activity, flowParams)
    }

    suspend fun queryProductDetails(params: QueryProductDetailsParams): ProductDetailsResult =
        billingClient.queryProductDetails(params)

    suspend fun queryPurchasesAsync(build: QueryPurchasesParams): PurchasesResult =
        billingClient.queryPurchasesAsync(build)
}
