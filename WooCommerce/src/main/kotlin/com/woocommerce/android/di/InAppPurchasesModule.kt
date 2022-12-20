package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iapshowcase.IAPDebugLogWrapper
import com.woocommerce.android.iapshowcase.purchase.IAPShowcaseMobilePayAPIProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent

@InstallIn(ViewModelComponent::class)
@Module
class InAppPurchasesModule {
    @Provides
    fun providePurchaseWPComPlanActions(
        context: Application,
        mobilePayAPIProvider: IAPShowcaseMobilePayAPIProvider
    ): PurchaseWPComPlanActions =
        IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
            context,
            IAPDebugLogWrapper(),
            mobilePayAPIProvider::buildMobilePayAPI
        )

    @Provides
    fun providePurchaseWpComPlanSupportChecker(application: Application): PurchaseWpComPlanSupportChecker =
        IAPSitePurchasePlanFactory.createIAPPurchaseWpComPlanSupportChecker(
            application,
            IAPDebugLogWrapper(),
        )
}
