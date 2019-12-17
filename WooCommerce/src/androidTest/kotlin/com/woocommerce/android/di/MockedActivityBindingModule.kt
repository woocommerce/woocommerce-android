package com.woocommerce.android.di

import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpModule
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.MagicLinkInterceptActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MockedMainModule
import com.woocommerce.android.ui.orders.MockedOrdersModule
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.prefs.AppSettingsModule
import com.woocommerce.android.ui.prefs.MainSettingsModule
import com.woocommerce.android.ui.prefs.PrivacySettingsModule
import com.woocommerce.android.ui.products.MockedProductDetailFragmentModule
import com.woocommerce.android.ui.reviews.MockedReviewDetailFragmentModule
import com.woocommerce.android.ui.reviews.MockedReviewListFragmentModule
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.ui.sitepicker.SitePickerModule
import com.woocommerce.android.ui.stats.MockedDashboardModule
import com.woocommerce.android.ui.stats.MockedMyStoreModule
import dagger.Module
import dagger.android.ContributesAndroidInjector
import org.wordpress.android.login.di.LoginFragmentModule

@Module
abstract class MockedActivityBindingModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = arrayOf(
            MockedMainModule::class,
            MockedDashboardModule::class,
            MockedMyStoreModule::class,
            MockedOrdersModule::class,
            MockedProductModule::class,
            MockedReviewModule::class))
    abstract fun provideMainActivityInjector(): MainActivity

    @Module(includes = [
        MockedProductDetailFragmentModule::class
    ])
    object MockedProductModule

    @Module(includes = [
        MockedReviewListFragmentModule::class,
        MockedReviewDetailFragmentModule::class
    ])
    object MockedReviewModule

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
