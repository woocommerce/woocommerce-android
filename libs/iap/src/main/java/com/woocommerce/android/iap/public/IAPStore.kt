package com.woocommerce.android.iap.public

import com.woocommerce.android.iap.public.model.IAPProduct

interface IAPStore {
    fun confirmOrderOnServer(iapProduct: IAPProduct)
}
