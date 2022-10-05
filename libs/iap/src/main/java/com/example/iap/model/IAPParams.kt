package com.example.iap.model

import com.example.iap.model.IAPProductType.SUBS

enum class IAPProductType {
    SUBS, INAPP
}

sealed class IAPProduct(val value: String, val productType: IAPProductType) {
    object PremiumPlan : IAPProduct("premium_plan", SUBS)
}
