package com.woocommerce.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.android.volley.VolleyLog
import com.woocommerce.android.config.RemoteConfigRepository
import com.yarolegovich.wellsql.WellSql
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WooCommerce : Application(), HasAndroidInjector, Configuration.Provider {
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>

    // inject it lazily to avoid creating it before initializing WellSql
    @Inject lateinit var appInitializer: Lazy<AppInitializer>
    @Inject lateinit var remoteConfigRepository: RemoteConfigRepository
    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        // Disables Volley debug logging on release build and prevents the "Marker added to finished log" crash
        // https://github.com/woocommerce/woocommerce-android/issues/817
        if (!BuildConfig.DEBUG) {
            VolleyLog.DEBUG = false
        }

        val wellSqlConfig = WooWellSqlConfig(applicationContext)
        WellSql.init(wellSqlConfig)

        remoteConfigRepository.fetchRemoteConfig()

        appInitializer.get().init(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
