package com.woocommerce.android.iap.pub

import com.woocommerce.android.iap.pub.model.IAPProduct

interface IAPStore {
    suspend fun confirmOrderOnServer(iapProduct: IAPProduct): Result<Unit>
}
