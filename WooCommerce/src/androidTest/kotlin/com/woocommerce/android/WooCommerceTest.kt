package com.woocommerce.android

import android.app.Application
import androidx.work.Configuration
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.android.testing.CustomTestApplication
import dagger.hilt.components.SingletonComponent

@CustomTestApplication(BaseWooCommerce::class)
interface WooCommerceTest

open class BaseWooCommerce : Application(), HasAndroidInjector, Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .build()

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AndroidInjectorEntryPoint {
        fun injector(): DispatchingAndroidInjector<Any>
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return EntryPoints.get(
            this,
            AndroidInjectorEntryPoint::class.java
        ).injector()
    }
}
