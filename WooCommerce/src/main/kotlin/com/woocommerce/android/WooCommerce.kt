package com.woocommerce.android

import android.content.Context
import com.android.volley.VolleyLog
import com.google.android.play.core.splitcompat.SplitCompat
import com.google.android.play.core.splitcompat.SplitCompatApplication
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.woocommerce.android.util.WooLog
import com.yarolegovich.wellsql.WellSql
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WooCommerce : SplitCompatApplication(), HasAndroidInjector {
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    // inject it lazily to avoid creating it before initializing WellSql
    @Inject lateinit var appInitializer: Lazy<AppInitializer>

    override fun onCreate() {
        super.onCreate()

        // Disables Volley debug logging on release build and prevents the "Marker added to finished log" crash
        // https://github.com/woocommerce/woocommerce-android/issues/817
        if (!BuildConfig.DEBUG) {
            VolleyLog.DEBUG = false
        }

        val wellSqlConfig = WooWellSqlConfig(applicationContext)
        WellSql.init(wellSqlConfig)

        appInitializer.get().init(this)
    }

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(base)
        // Emulates installation of future on demand modules using SplitCompat.
        SplitCompat.install(this)
    }

    fun installBarcodeCameraModule() {
        val splitInstallManager = SplitInstallManagerFactory.create(applicationContext)
        val request = SplitInstallRequest.newBuilder()
            .addModule("barcode")
            .build()

        splitInstallManager.startInstall(request)
            .addOnSuccessListener {
                WooLog.d(WooLog.T.UTILS, it.toString())
            }
            .addOnFailureListener {
                WooLog.d(WooLog.T.UTILS, it.toString())
            }
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
