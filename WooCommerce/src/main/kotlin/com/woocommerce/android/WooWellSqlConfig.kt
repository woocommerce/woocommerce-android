package com.woocommerce.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.yarolegovich.wellsql.WellTableManager
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class WooWellSqlConfig(context: Context?) : WellSqlConfig(context, WellSqlConfig.ADDON_WOOCOMMERCE) {
    override fun onDowngrade(db: SQLiteDatabase?, helper: WellTableManager?, oldVersion: Int, newVersion: Int) {
        if (BuildConfig.DEBUG) {
            AppLog.w(T.DB, "Resetting database due to downgrade from version $oldVersion to $newVersion")
            recreateTables(helper)
        } else {
            super.onDowngrade(db, helper, oldVersion, newVersion)
        }
    }

    // TODO: remove this
    override fun getDbVersion(): Int {
        return 999
    }

    private fun recreateTables(helper: WellTableManager?) {
        for (table in mTables) {
            AppLog.w(T.DB, "recreating table ${table.simpleName}")
            helper?.dropTable(table)
            helper?.createTable(table)
        }
    }
}
