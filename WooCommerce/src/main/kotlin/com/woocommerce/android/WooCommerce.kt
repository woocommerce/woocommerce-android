package com.woocommerce.android

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.android.volley.VolleyLog
import com.woocommerce.android.config.RemoteConfigRepository
import com.woocommerce.android.extensions.getCurrentProcessName
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WooCommerce : Application(), HasAndroidInjector, Configuration.Provider {
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>

    @Inject lateinit var appInitializer: Lazy<AppInitializer>

    @Inject lateinit var remoteConfigRepository: Lazy<RemoteConfigRepository>

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Stripe Tap to Pay library starts it's own process. That causes the crash:
        //  > Caused by: java.lang.IllegalStateException: Default FirebaseApp is not initialized in this process
        //  > com.woocommerce.android:stripetaptopay Make sure to call FirebaseApp.initializeApp(Context) first.
        // In this case we don't want to initialize any Firebase (or any at all) features of the app in their process.
        if (getCurrentProcessName() == "${BuildConfig.APPLICATION_ID}:$TAP_TO_PAY_STRIPE_PROCESS_NAME_SUFFIX") return

        // Disables Volley debug logging on release build and prevents the "Marker added to finished log" crash
        // https://github.com/woocommerce/woocommerce-android/issues/817
        if (!BuildConfig.DEBUG) {
            VolleyLog.DEBUG = false
        }

        remoteConfigRepository.get().fetchRemoteConfig()

        appInitializer.get().init(this)
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    companion object {
        private const val TAP_TO_PAY_STRIPE_PROCESS_NAME_SUFFIX = "stripetaptopay"
    }
}
