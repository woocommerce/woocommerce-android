package com.woocommerce.android.di

import com.woocommerce.android.ui.dashboard.DashboardModule
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEpilogueActivity
import com.woocommerce.android.ui.login.LoginEpilogueModule
import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainModule
import com.woocommerce.android.ui.orders.OrderDetailModule
import com.woocommerce.android.ui.orders.OrderFulfillmentModule
import com.woocommerce.android.ui.orders.OrderListModule
import com.woocommerce.android.ui.orders.OrderProductListModule
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.login.di.LoginFragmentModule

@Module
abstract class ActivityBindingModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(
            MainModule::class,
            DashboardModule::class,
            OrderListModule::class,
            OrderDetailModule::class,
            OrderProductListModule::class,
            OrderFulfillmentModule::class))
    abstract fun provideMainActivityInjector(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(LoginFragmentModule::class))
    abstract fun provideLoginActivityInjector(): LoginActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(LoginEpilogueModule::class))
    abstract fun provideLoginEpilogueActivityInjector(): LoginEpilogueActivity

    @ActivityScope
    @ContributesAndroidInjector
    abstract fun provideMagicLinkInterceptActivityInjector(): MagicLinkInterceptActivity
}
