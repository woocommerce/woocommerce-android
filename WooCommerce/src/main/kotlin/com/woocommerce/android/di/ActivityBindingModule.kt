package com.woocommerce.android.di

import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpModule
import com.woocommerce.android.ui.aztec.AztecModule
import com.woocommerce.android.ui.dashboard.DashboardModule
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import com.woocommerce.android.ui.login.WooLoginFragmentModule
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainModule
import com.woocommerce.android.ui.mystore.MyStoreModule
import com.woocommerce.android.ui.orders.OrdersModule
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelsModule
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.prefs.AppSettingsModule
import com.woocommerce.android.ui.prefs.MainSettingsModule
import com.woocommerce.android.ui.prefs.PrivacySettingsModule
import com.woocommerce.android.ui.products.ProductsModule
import com.woocommerce.android.ui.refunds.RefundsModule
import com.woocommerce.android.ui.reviews.ReviewsModule
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.ui.sitepicker.SitePickerModule
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.login.di.LoginFragmentModule

@Module
abstract class ActivityBindingModule {
    @ActivityScope
    @ContributesAndroidInjector(
            modules = [
            MainModule::class,
            DashboardModule::class,
            MyStoreModule::class,
            OrdersModule::class,
            RefundsModule::class,
            ProductsModule::class,
            ReviewsModule::class,
            SitePickerModule::class,
            AztecModule::class,
            ShippingLabelsModule::class
    ])
    abstract fun provideMainActivityInjector(): MainActivity

    @ActivityScope
    @ContributesAndroidInjector(modules = [
        LoginFragmentModule::class,
        WooLoginFragmentModule::class])
    abstract fun provideLoginActivityInjector(): LoginActivity

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

    @ActivityScope
    @ContributesAndroidInjector(modules = [SitePickerModule::class])
    abstract fun provideSitePickerActivityInjector(): SitePickerActivity
}
