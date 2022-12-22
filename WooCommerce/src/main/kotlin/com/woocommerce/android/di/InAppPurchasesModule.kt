package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iapshowcase.purchase.IAPShowcaseMobilePayAPIProvider
import com.woocommerce.android.ui.login.storecreation.iap.WooIapLogWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@InstallIn(SingletonComponent::class)
@Module
class InAppPurchasesModule {
    @Provides
    fun providePurchaseWPComPlanActions(
        context: Application,
        mobilePayAPIProvider: IAPShowcaseMobilePayAPIProvider
    ): PurchaseWPComPlanActions =
        IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
            context,
            1L,
            WooIapLogWrapper(),
            mobilePayAPIProvider::buildMobilePayAPI
        )

    @Provides
    fun providePurchaseWpComPlanSupportChecker(application: Application): PurchaseWpComPlanSupportChecker =
        IAPSitePurchasePlanFactory.createIAPPurchaseWpComPlanSupportChecker(
            application,
            WooIapLogWrapper(),
        )
}
