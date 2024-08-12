package com.woocommerce.android.app

import android.app.Application
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.yarolegovich.wellsql.WellSql
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.WellSqlConfig.Companion.ADDON_WOOCOMMERCE

@HiltAndroidApp
open class WooCommerceWear : Application() {

    @Inject
    lateinit var crashLogging: CrashLogging

    override fun onCreate() {
        super.onCreate()
        WellSql.init(WellSqlConfig(applicationContext, ADDON_WOOCOMMERCE))
        crashLogging.initialize()
    }
}
