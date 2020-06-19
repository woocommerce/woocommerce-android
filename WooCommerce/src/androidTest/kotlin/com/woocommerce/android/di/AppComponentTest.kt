package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.push.FCMServiceModule
import com.woocommerce.android.ui.login.LoginAnalyticsModule
import com.woocommerce.android.ui.products.MockedProductCategoriesModule
import com.woocommerce.android.ui.products.MockedWooStoreModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import org.wordpress.android.fluxc.module.ReleaseBaseModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseOkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseWCNetworkModule
import org.wordpress.android.login.di.LoginServiceModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
        AndroidInjectionModule::class,
        ThreadModule::class,
        MockedViewModelAssistedFactoriesModule::class,
        ApplicationModule::class,
        AppConfigModule::class,
        ReleaseBaseModule::class,
        ReleaseNetworkModule::class,
        ReleaseWCNetworkModule::class,
        ReleaseOkHttpClientModule::class,
        MockedActivityBindingModule::class,
        MockedWooStoreModule::class,
        MockedProductDetailRepositoryModule::class,
        MockedProductCategoriesModule::class,
        MockedSelectedSiteModule::class,
        FCMServiceModule::class,
        LoginAnalyticsModule::class,
        LoginServiceModule::class,
        MockedNetworkStatusModule::class,
        MockedCurrencyModule::class,
        SupportModule::class,
        OrderFetcherModule::class])
interface AppComponentTest : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder {
        @BindsInstance
        override fun application(application: Application): Builder

        override fun build(): AppComponentTest
    }
}
