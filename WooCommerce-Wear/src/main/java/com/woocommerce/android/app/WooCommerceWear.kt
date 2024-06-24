package com.woocommerce.android.app

import android.app.Application
import com.yarolegovich.wellsql.WellSql
import dagger.hilt.android.HiltAndroidApp
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.persistence.WellSqlConfig.Companion.ADDON_WOOCOMMERCE

@HiltAndroidApp
open class WooCommerceWear : Application() {
    override fun onCreate() {
        super.onCreate()
        WellSql.init(WellSqlConfig(applicationContext, ADDON_WOOCOMMERCE))
    }
}
