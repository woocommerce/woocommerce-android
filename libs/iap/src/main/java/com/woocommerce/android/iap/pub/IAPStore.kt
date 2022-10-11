package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.pub.model.IAPProduct

interface IAPStore {
    fun confirmOrderOnServer(iapProduct: IAPProduct)
}
