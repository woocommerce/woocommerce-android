package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.media.ProductImagesServiceModule
import com.woocommerce.android.push.FCMServiceModule
import com.woocommerce.android.ui.login.LoginAnalyticsModule
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjectionModule
import org.wordpress.android.fluxc.module.DebugOkHttpClientModule
import org.wordpress.android.fluxc.module.ReleaseBaseModule
import org.wordpress.android.fluxc.module.ReleaseNetworkModule
import org.wordpress.android.fluxc.module.ReleaseWCNetworkModule
import org.wordpress.android.login.di.LoginServiceModule
import javax.inject.Singleton

@Singleton
@Component(modules = [
        AndroidInjectionModule::class,
        ApplicationModule::class,
        AppConfigModule::class,
        ReleaseBaseModule::class,
        ReleaseNetworkModule::class,
        ReleaseWCNetworkModule::class,
        DebugOkHttpClientModule::class,
        SelectedSiteModule::class,
        InterceptorModule::class,
        ActivityBindingModule::class,
        ThreadModule::class,
        FCMServiceModule::class,
        LoginAnalyticsModule::class,
        LoginServiceModule::class,
        NetworkStatusModule::class,
        CurrencyModule::class,
        ProductImagesServiceModule::class,
        ThreadModule::class,
        SupportModule::class,
        OrderFetcherModule::class])
interface AppComponentDebug : AppComponent {
    @Component.Builder
    interface Builder : AppComponent.Builder {
        @BindsInstance
        override fun application(application: Application): Builder
    }
}
