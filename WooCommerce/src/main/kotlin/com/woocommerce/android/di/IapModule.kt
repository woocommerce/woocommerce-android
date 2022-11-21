package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iapshowcase.IAPDebugLogWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class IapModule {
    @Provides
    fun providePurchaseWPComPlanActions(context: Application): PurchaseWPComPlanActions =
        IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
            context,
            1L,
            IAPDebugLogWrapper(),
        )
}
