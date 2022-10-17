package com.woocommerce.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.yarolegovich.wellsql.WellSql
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
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AndroidInjectorEntryPoint {
        fun injector(): DispatchingAndroidInjector<Any>
    }

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface HiltWorkerFactoryEntryPoint {
        fun workerFactory(): HiltWorkerFactory
    }

    override fun onCreate() {
        super.onCreate()
        val wellSqlConfig = WooWellSqlConfig(this)
        WellSql.init(wellSqlConfig)
    }

    override fun androidInjector(): AndroidInjector<Any> {
        return EntryPoints.get(
            applicationContext,
            AndroidInjectorEntryPoint::class.java
        ).injector()
    }

    override fun getWorkManagerConfiguration(): Configuration {
        val hiltWorkerFactory = EntryPoints.get(
            applicationContext,
            HiltWorkerFactoryEntryPoint::class.java
        ).workerFactory()

        return Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()
    }
}
