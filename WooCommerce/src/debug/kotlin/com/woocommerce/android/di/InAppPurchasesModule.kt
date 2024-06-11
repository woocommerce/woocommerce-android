package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.iap.pub.IAPSitePurchasePlanFactory
import com.woocommerce.android.iap.pub.PurchaseWPComPlanActions
import com.woocommerce.android.iap.pub.PurchaseWpComPlanSupportChecker
import com.woocommerce.android.iap.pub.network.SandboxTestingConfig
import com.woocommerce.android.iapshowcase.IAPDebugLogWrapper
import com.woocommerce.android.iapshowcase.IapMobilePayApiProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

@InstallIn(ViewModelComponent::class)
@Module
class InAppPurchasesModule {
    @ViewModelScoped
    @Provides
    fun providePurchaseWPComPlanActions(
        context: Application,
        mobilePayAPIProvider: IapMobilePayApiProvider,
    ): PurchaseWPComPlanActions =
        IAPSitePurchasePlanFactory.createIAPSitePurchasePlan(
            context,
            IAPDebugLogWrapper(),
            mobilePayAPIProvider::buildMobilePayAPI,
            SandboxTestingConfigImpl()
        )

    @ViewModelScoped
    @Provides
    fun providePurchaseWpComPlanSupportChecker(application: Application): PurchaseWpComPlanSupportChecker =
        IAPSitePurchasePlanFactory.createIAPPurchaseWpComPlanSupportChecker(
            application,
            IAPDebugLogWrapper(),
        )

    private class SandboxTestingConfigImpl(
        override val isDebug: Boolean = BuildConfig.DEBUG,
        override val iapTestingSandboxUrl: String = BuildConfig.IAP_TESTING_SANDBOX_URL,
    ) : SandboxTestingConfig
}
