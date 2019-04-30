package com.woocommerce.android.di

import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpModule
import com.woocommerce.android.ui.dashboard.DashboardModule
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MockedMainModule
import com.woocommerce.android.ui.notifications.NotifsListModule
import com.woocommerce.android.ui.notifications.ReviewDetailModule
import com.woocommerce.android.ui.orders.OrderDetailModule
import com.woocommerce.android.ui.orders.OrderFulfillmentModule
import com.woocommerce.android.ui.orders.OrderListModule
import com.woocommerce.android.ui.orders.OrderProductListModule
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.prefs.AppSettingsModule
import com.woocommerce.android.ui.prefs.MainSettingsModule
import com.woocommerce.android.ui.prefs.PrivacySettingsModule
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.ui.sitepicker.SitePickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.login.di.LoginFragmentModule

@Module
abstract class MockedActivityBindingModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = [
        MockedMainModule::class,
        DashboardModule::class,
        OrderListModule::class,
        OrderDetailModule::class,
        OrderProductListModule::class,
        OrderFulfillmentModule::class,
        NotifsListModule::class,
        ReviewDetailModule::class
    ])
    abstract fun provideMainActivityInjector(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [LoginFragmentModule::class])
    abstract fun provideLoginActivityInjector(): LoginActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [SitePickerModule::class])
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
