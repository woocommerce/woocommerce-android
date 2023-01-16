package com.woocommerce.android

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.android.volley.VolleyLog
import com.google.firebase.FirebaseApp
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.ui.payments.taptopay.IsTapToPayAvailable
import com.yarolegovich.wellsql.WellSql
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WooCommerce : Application(), HasAndroidInjector, Configuration.Provider {
    @Inject
    lateinit var androidInjector: DispatchingAndroidInjector<Any>

    // inject it lazily to avoid creating it before initializing WellSql
    @Inject lateinit var appInitializer: Lazy<AppInitializer>

    @Inject lateinit var remoteConfigRepository: Lazy<RemoteConfigRepository>

    @Inject lateinit var workerFactory: HiltWorkerFactory

    @Inject lateinit var isTapToPayAvailable: IsTapToPayAvailable

    override fun onCreate() {
        super.onCreate()

        initFirebaseIfSeparateProcess()

        // Disables Volley debug logging on release build and prevents the "Marker added to finished log" crash
        // https://github.com/woocommerce/woocommerce-android/issues/817
        if (!BuildConfig.DEBUG) {
            VolleyLog.DEBUG = false
        }

        val wellSqlConfig = WooWellSqlConfig(applicationContext)
        WellSql.init(wellSqlConfig)

        remoteConfigRepository.get().fetchRemoteConfig()

        appInitializer.get().init(this)
    }

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    /**
     * Looks like that stripe Tap to Pay starts it's own process
     * > Caused by: java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process
     * > com.stripe.cots.aidlservice. Make sure to call FirebaseApp.initializeApp(Context) first.
     *
     * In this case we have to initialise Firebase manually in this process as per documentation
     * https://firebase.google.com/docs/reference/android/com/google/firebase/FirebaseApp
     */
    private fun initFirebaseIfSeparateProcess() {
        if (isTapToPayAvailable()) {
            if (getCurrentProcessName() == TAP_TO_PAY_STRIPE_PROCESS_NAME) {
                FirebaseApp.initializeApp(this)
            }
        }
    }

    private fun getCurrentProcessName() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.runningAppProcesses.firstOrNull { it.pid == android.os.Process.myPid() }?.processName
        }

    companion object {
        private const val TAP_TO_PAY_STRIPE_PROCESS_NAME = "com.stripe.cots.aidlservice"
    }
}
