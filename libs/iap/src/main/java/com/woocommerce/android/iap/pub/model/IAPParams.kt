package com.woocommerce.android.iap.pub.model

import com.woocommerce.android.iap.pub.model.IAPProductType.SUBS

sealed class IAPProduct(val productId: String, val productType: IAPProductType) {
    object WPPremiumPlan : IAPProduct("premium_plan", SUBS)
    object WPPremiumPlanTesting : IAPProduct("test_product_11.10.2022", SUBS)
}

enum class IAPProductType {
    SUBS, INAPP
}
