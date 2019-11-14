package com.woocommerce.android.di

import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpModule
import com.woocommerce.android.ui.dashboard.DashboardModule
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginNoJetpackFragmentModule
import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import com.woocommerce.android.ui.login.MagicLinkInterceptFragmentModule
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainModule
import com.woocommerce.android.ui.mystore.MyStoreModule
import com.woocommerce.android.ui.orders.notes.AddOrderNoteModule
import com.woocommerce.android.ui.orders.AddOrderShipmentTrackingModule
import com.woocommerce.android.ui.orders.AddOrderTrackingProviderListModule
import com.woocommerce.android.ui.orders.OrderDetailModule
import com.woocommerce.android.ui.orders.OrderFulfillmentModule
import com.woocommerce.android.ui.orders.OrderListModule
import com.woocommerce.android.ui.orders.OrderProductListModule
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.prefs.AppSettingsModule
import com.woocommerce.android.ui.prefs.MainSettingsModule
import com.woocommerce.android.ui.prefs.PrivacySettingsModule
import com.woocommerce.android.ui.products.ProductDetailFragmentModule
import com.woocommerce.android.ui.products.ProductListFragmentModule
import com.woocommerce.android.ui.products.ProductVariantsFragmentModule
import com.woocommerce.android.ui.refunds.IssueRefundFragmentModule
import com.woocommerce.android.ui.refunds.RefundByAmountFragmentModule
import com.woocommerce.android.ui.refunds.RefundDetailFragmentModule
import com.woocommerce.android.ui.refunds.RefundSummaryFragmentModule
import com.woocommerce.android.ui.reviews.ReviewDetailFragmentModule
import com.woocommerce.android.ui.reviews.ReviewListFragmentModule
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
            OrderListModule::class,
            OrderDetailModule::class,
            OrderProductListModule::class,
            OrderFulfillmentModule::class,
            RefundModule::class,
            ProductModule::class,
            ReviewModule::class,
            AddOrderNoteModule::class,
            SitePickerModule::class,
            AddOrderShipmentTrackingModule::class,
            AddOrderTrackingProviderListModule::class
    ])
    abstract fun provideMainActivityInjector(): MainActivity

    @Module(includes = [
        IssueRefundFragmentModule::class,
        RefundByAmountFragmentModule::class,
        RefundSummaryFragmentModule::class,
        RefundDetailFragmentModule::class
    ])
    object RefundModule

    @Module(includes = [
        ProductDetailFragmentModule::class,
        ProductListFragmentModule::class,
        ProductVariantsFragmentModule::class
    ])
    object ProductModule

    @Module(includes = [
        ReviewDetailFragmentModule::class,
        ReviewListFragmentModule::class
    ])
    object ReviewModule

    @ActivityScope
    @ContributesAndroidInjector(modules = [
        LoginFragmentModule::class,
        MagicLinkInterceptFragmentModule::class,
        LoginNoJetpackFragmentModule::class])
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
