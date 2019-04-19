package com.woocommerce.android.di

import com.woocommerce.android.ui.dashboard.DashboardModule
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MockedMainModule
import com.woocommerce.android.ui.notifications.NotifsListModule
import com.woocommerce.android.ui.notifications.ReviewDetailModule
import com.woocommerce.android.ui.orders.MockedOrderListModule
import com.woocommerce.android.ui.orders.OrderDetailModule
import com.woocommerce.android.ui.orders.OrderFulfillmentModule
import com.woocommerce.android.ui.orders.OrderProductListModule
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.ui.sitepicker.SitePickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.login.di.LoginFragmentModule

@Module
abstract class MockedActivityBindingModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(
            MockedMainModule::class,
            DashboardModule::class,
            MockedOrderListModule::class,
            OrderDetailModule::class,
            OrderProductListModule::class,
            OrderFulfillmentModule::class,
            NotifsListModule::class,
            ReviewDetailModule::class))
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
}
