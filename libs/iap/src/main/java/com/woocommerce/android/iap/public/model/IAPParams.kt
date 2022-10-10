package com.woocommerce.android.iap.public.model

import com.woocommerce.android.iap.public.model.IAPProductType.SUBS

sealed class IAPProduct(val name: String, val productType: IAPProductType) {
    object WPPremiumPlan : IAPProduct("premium_plan", SUBS)
}

enum class IAPProductType {
    SUBS, INAPP
}
