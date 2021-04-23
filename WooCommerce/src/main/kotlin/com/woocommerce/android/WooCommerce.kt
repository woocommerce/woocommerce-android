package com.woocommerce.android

import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.os.StrictMode.VmPolicy
import androidx.multidex.MultiDexApplication
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.util.AppThemeUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.yarolegovich.wellsql.WellSql
import dagger.android.AndroidInjector
import dagger.android.HasAndroidInjector
import javax.inject.Inject

open class WooCommerce : MultiDexApplication(), HasAndroidInjector {
    // TODO cardreader init this field
    open val cardReaderManager: CardReaderManager? = null

    @Inject lateinit var appInitializer: AppInitializer

    override fun onCreate() {
        super.onCreate()
        val wellSqlConfig = WooWellSqlConfig(this)
        WellSql.init(wellSqlConfig)

        appInitializer.init(this)

        // Apply Theme
        AppThemeUtils.setAppTheme()
    }

    /**
     * enables "strict mode" for testing - should NEVER be used in release builds
     */
    private fun enableStrictMode() {
        // return if the build is not a debug build
        if (!BuildConfig.DEBUG) {
            WooLog.e(T.UTILS, "You should not call enableStrictMode() on a non debug build")
            return
        }

        StrictMode.setThreadPolicy(
            ThreadPolicy.Builder()
                .detectDiskWrites()
                .detectNetwork()
                .penaltyLog()
                .penaltyFlashScreen()
                .build()
        )

        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectActivityLeaks()
                .detectLeakedSqlLiteObjects()
                .detectLeakedClosableObjects()
                .detectLeakedRegistrationObjects()
                .penaltyLog()
                .build()
        )
        WooLog.w(T.UTILS, "Strict mode enabled")
    }

    override fun androidInjector(): AndroidInjector<Any> = appInitializer.androidInjector
}
