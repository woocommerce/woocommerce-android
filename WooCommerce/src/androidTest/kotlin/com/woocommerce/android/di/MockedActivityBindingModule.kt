package com.woocommerce.android.di

import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpModule
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MockedMainModule
import com.woocommerce.android.ui.notifications.NotifsListModule
import com.woocommerce.android.ui.notifications.ReviewDetailModule
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingModule
import com.woocommerce.android.ui.orders.MockedAddOrderTrackingProviderListModule
import com.woocommerce.android.ui.orders.MockedOrderDetailModule
import com.woocommerce.android.ui.orders.MockedOrderFulfillmentModule
import com.woocommerce.android.ui.orders.MockedOrderListModule
import com.woocommerce.android.ui.orders.OrderProductListModule
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.prefs.AppSettingsModule
import com.woocommerce.android.ui.prefs.MainSettingsModule
import com.woocommerce.android.ui.prefs.PrivacySettingsModule
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.ui.sitepicker.SitePickerModule
import com.woocommerce.android.ui.stats.MockedDashboardModule
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.login.di.LoginFragmentModule

@Module
abstract class MockedActivityBindingModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(
            MockedMainModule::class,
            MockedDashboardModule::class,
            MockedOrderListModule::class,
            MockedOrderDetailModule::class,
            OrderProductListModule::class,
            MockedOrderFulfillmentModule::class,
            NotifsListModule::class,
            ReviewDetailModule::class,
            AddOrderShipmentTrackingModule::class,
            MockedAddOrderTrackingProviderListModule::class))
    abstract fun provideMainActivityInjector(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(LoginFragmentModule::class))
    abstract fun provideLoginActivityInjector(): LoginActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(SitePickerModule::class))
    abstract fun provideSitePickerActivityInjector(): SitePickerActivity

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun provideMagicLinkInterceptActivityInjector(): MagicLinkInterceptActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [
        AppSettingsModule::class,
        MainSettingsModule::class,
        PrivacySettingsModule::class
    ])
    abstract fun provideAppSettingsActivityInjector(): AppSettingsActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [HelpModule::class])
    abstract fun provideHelpActivity(): HelpActivity
}
