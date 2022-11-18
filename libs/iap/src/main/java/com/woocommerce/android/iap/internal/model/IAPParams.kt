package com.woocommerce.android.iap.internal.model

import com.woocommerce.android.iap.internal.model.IAPProductType.SUBS

internal sealed class IAPProduct(val productId: String, val productType: IAPProductType) {
    object WPPremiumPlan : IAPProduct("premium_plan", SUBS)
    object WPPremiumPlanTesting : IAPProduct("test_product_11.10.2022", SUBS)
}

internal enum class IAPProductType {
    SUBS, INAPP
}
