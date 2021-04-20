package com.woocommerce.android

import android.content.Context
import com.yarolegovich.wellsql.WellSql
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WellSqlInitializer @Inject constructor(@ApplicationContext private val context: Context) {
    init {
        val wellSqlConfig = WooWellSqlConfig(context)
        WellSql.init(wellSqlConfig)
    }
}
