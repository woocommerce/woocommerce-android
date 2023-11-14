package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.pub.model.IAPSupportedResult
import java.io.Closeable

interface PurchaseWpComPlanSupportChecker : Closeable {
    suspend fun isIAPSupported(): IAPSupportedResult
}
