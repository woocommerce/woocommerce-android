package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingClient.ProductType
import com.woocommerce.android.iap.internal.model.IAPProductType

internal class IAPInMapper {
    @ProductType
    fun mapProductTypeToIAPProductType(iapProductType: IAPProductType): String {
        return when (iapProductType) {
            IAPProductType.SUBS -> ProductType.SUBS
            IAPProductType.INAPP -> ProductType.INAPP
        }
    }
}
