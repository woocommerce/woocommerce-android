package com.woocommerce.android

import androidx.hilt.work.HiltWorkerFactory
import androidx.multidex.MultiDexApplication
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import com.android.volley.VolleyLog
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.util.crashlogging.UploadEncryptedLoggingWorker
import com.yarolegovich.wellsql.WellSql
import dagger.Lazy
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WooCommerce : MultiDexApplication(), HasAndroidInjector, Configuration.Provider {
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    // inject it lazily to avoid creating it before initializing WellSql
    @Inject lateinit var appInitializer: Lazy<AppInitializer>
    // TODO cardreader init this field
    open val cardReaderManager: CardReaderManager? = null

    @Inject lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration() =
        Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        // Disables Volley debug logging on release build and prevents the "Marker added to finished log" crash
        // https://github.com/woocommerce/woocommerce-android/issues/817
        if (!BuildConfig.DEBUG) {
            VolleyLog.DEBUG = false
        }

        val wellSqlConfig = WooWellSqlConfig(applicationContext)
        WellSql.init(wellSqlConfig)

        FeedbackPrefs.init(this)

        appInitializer.get().init(this)

        WorkManager.getInstance(this)
            .enqueue(OneTimeWorkRequest.from(UploadEncryptedLoggingWorker::class.java))
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
