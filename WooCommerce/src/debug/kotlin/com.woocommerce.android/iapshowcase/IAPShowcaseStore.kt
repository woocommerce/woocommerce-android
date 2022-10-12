package com.woocommerce.android.iapshowcase

import com.woocommerce.android.iap.pub.IAPStore
import com.woocommerce.android.iap.pub.model.IAPProduct
import kotlinx.coroutines.delay

class IAPShowcaseStore : IAPStore {
    override suspend fun confirmOrderOnServer(iapProduct: IAPProduct): Result<Unit> {
        delay(1000)
        return Result.success(Unit)
    }
}
