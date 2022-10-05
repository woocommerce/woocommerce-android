package com.example.iap.internal

import com.android.billingclient.api.BillingClient.ProductType
import com.example.iap.model.IAPProductType

internal class IAPInMapper {
    @ProductType
    fun mapProductTypeToIAPProductType(iapProductType: IAPProductType): String {
        return when (iapProductType) {
            IAPProductType.SUBS -> ProductType.SUBS
            IAPProductType.INAPP -> ProductType.INAPP
        }
    }
}
