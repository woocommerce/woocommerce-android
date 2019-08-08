package com.woocommerce.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.graphics.Color
import android.graphics.PorterDuff
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import com.woocommerce.android.util.PackageUtils
import com.yarolegovich.wellsql.WellTableManager
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class WooWellSqlConfig(context: Context?) : WellSqlConfig(context, WellSqlConfig.ADDON_WOOCOMMERCE) {
    /**
     * Detect when the database is downgraded in debug and beta builds so we can recreate all the tables and
     * show a toast about the downgrade. The initial purpose of this was to avoid the hassle of devs switching
     * branches and having to clear storage and login again due to a version downgrade, but we've had a couple
     * cases where a beta build with a DB downgrade was released, resulting in a lot of crashes.
     */
    override fun onDowngrade(db: SQLiteDatabase?, helper: WellTableManager?, oldVersion: Int, newVersion: Int) {
        if (PackageUtils.isDebugBuild() || PackageUtils.isBetaBuild(context)) {
            // note: don't call super() here because it throws an exception
            AppLog.w(T.DB, "Resetting database due to downgrade from version $oldVersion to $newVersion")

            val toast = Toast.makeText(
                    context,
                    R.string.database_downgraded,
                    Toast.LENGTH_LONG
            )
            toast.view?.let { view ->
                view.background.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN)
                view.findViewById<TextView>(android.R.id.message)?.setTextColor(Color.WHITE)
            }
            toast.setGravity(Gravity.CENTER, 0, 0)
            toast.show()

            reset(helper)
        } else {
            super.onDowngrade(db, helper, oldVersion, newVersion)
        }
    }
}
