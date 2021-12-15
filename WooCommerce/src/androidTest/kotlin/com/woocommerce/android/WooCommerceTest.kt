package com.woocommerce.android

import android.app.Application
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

open class BaseWooCommerce : Application(), HasAndroidInjector {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AndroidInjectorEntryPoint {
        fun injector(): DispatchingAndroidInjector<Any>
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
}
