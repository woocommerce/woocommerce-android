package com.example.iap.model

import com.example.iap.model.IAPProductType.SUBS

sealed class IAPProduct(val name: String, val productType: IAPProductType) {
    object PremiumPlan : IAPProduct("premium_plan", SUBS)
}

enum class IAPProductType {
    SUBS, INAPP
}
