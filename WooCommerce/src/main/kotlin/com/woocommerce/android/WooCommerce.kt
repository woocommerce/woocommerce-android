package com.woocommerce.android

import androidx.multidex.MultiDexApplication
import com.android.volley.VolleyLog
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.di.WooCommerceGlideModule
import com.yarolegovich.wellsql.WellSql
import dagger.Lazy
import dagger.MembersInjector
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WooCommerce : MultiDexApplication(), HasAndroidInjector {
    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var membersInjector: MembersInjector<WooCommerceGlideModule>
    @Inject lateinit var appInitializer: Lazy<AppInitializer>
    // TODO cardreader init this field
    open val cardReaderManager: CardReaderManager? = null

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
    }

    override fun androidInjector(): AndroidInjector<Any> = androidInjector
}
