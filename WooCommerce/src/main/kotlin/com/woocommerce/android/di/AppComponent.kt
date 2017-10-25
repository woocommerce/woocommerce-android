package com.woocommerce.android.di

import android.app.Application
import com.woocommerce.android.WooCommerce
import dagger.BindsInstance
import dagger.Component
import dagger.android.AndroidInjector
import dagger.android.support.AndroidSupportInjectionModule
import javax.inject.Singleton

@Singleton
@Component(modules = arrayOf(
        AndroidSupportInjectionModule::class,
        ApplicationModule::class))
interface AppComponent : AndroidInjector<WooCommerce> {
    override fun inject(app: WooCommerce)

    // Allows us to inject the application without having to instantiate any modules, and provides the Application
    // in the app graph
    @Component.Builder
    interface Builder {
        @BindsInstance
        fun application(application: Application): Builder

        fun build(): AppComponent
    }
}
