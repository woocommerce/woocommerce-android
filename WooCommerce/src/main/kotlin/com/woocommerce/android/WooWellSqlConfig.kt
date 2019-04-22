package com.woocommerce.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.yarolegovich.wellsql.WellTableManager
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class WooWellSqlConfig(context: Context?) : WellSqlConfig(context, WellSqlConfig.ADDON_WOOCOMMERCE) {
    override fun onDowngrade(db: SQLiteDatabase?, helper: WellTableManager?, oldVersion: Int, newVersion: Int) {
        // note: don't call super since it throws an exception
        AppLog.w(T.DB, "Resetting database due to downgrade from version $oldVersion to $newVersion")
        recreateDatabase(helper)
    }

    // TODO: remove this
    override fun getDbVersion(): Int {
        return super.getDbVersion() - 1
    }
}
