package com.woocommerce.android.iapshowcase

import com.woocommerce.android.iap.pub.IAPStore
import com.woocommerce.android.iap.pub.model.IAPProduct
import kotlinx.coroutines.delay

private const val DELAY = 1000L

class IAPShowcaseStore : IAPStore {
    override suspend fun confirmOrderOnServer(iapProduct: IAPProduct): Result<Unit> {
        delay(DELAY)
        return Result.success(Unit)
    }
}
