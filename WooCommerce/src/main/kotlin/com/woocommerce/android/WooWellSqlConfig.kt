package com.woocommerce.android

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.view.Gravity
import android.widget.Toast
import com.woocommerce.android.util.FeatureFlag
import com.yarolegovich.wellsql.WellTableManager
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T

class WooWellSqlConfig(context: Context) : WellSqlConfig(context, ADDON_WOOCOMMERCE) {
    /**
     * Detect when the database is downgraded in debug and beta builds so we can recreate all the tables.
     * The initial purpose of this was to avoid the hassle of devs switching branches and having to clear
     * storage and login again due to a version downgrade, but we've had a couple of cases where a beta
     * build with a DB downgrade was released, resulting in a lot of crashes.
     */
    override fun onDowngrade(db: SQLiteDatabase?, helper: WellTableManager?, oldVersion: Int, newVersion: Int) {
        if (FeatureFlag.DB_DOWNGRADE.isEnabled(context)) {
            // note: don't call super() here because it throws an exception
            AppLog.w(T.DB, "Resetting database due to downgrade from version $oldVersion to $newVersion")

            // for debug builds, alert the dev to the downgrade
            if (BuildConfig.DEBUG) {
                val toast = Toast.makeText(
                        context,
                        R.string.database_downgraded,
                        Toast.LENGTH_LONG
                )
                toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 0)
                toast.show()
            }

            // the main activity uses this to determine when it needs to load the site list
            AppPrefs.setDatabaseDowngraded(true)
            helper?.let { reset(it) }
        } else {
            super.onDowngrade(db, helper, oldVersion, newVersion)
        }
    }

    /**
     * Useful during development when we want to test features with a "fresh" database. This can be
     * called from WooCommerce.onCreate() after we initialize the database. For safety, this has no
     * effect when called from a release build.
     */
    fun resetDatabase() {
        if (BuildConfig.DEBUG) {
            val toast = Toast.makeText(
                    context,
                    "Resetting database",
                    Toast.LENGTH_LONG
            )
            toast.setGravity(Gravity.CENTER_HORIZONTAL or Gravity.BOTTOM, 0, 0)
            toast.show()
            AppPrefs.setDatabaseDowngraded(true)
            reset()
        }
    }

    /**
     * Increase the cursor window size to 5MB for devices running API 28 and above. This should reduce the
     * number of SQLiteBlobTooBigExceptions. Note that this is only called on API 28 and
     * above since earlier versions don't allow adjusting the cursor window size.
     */
    override fun getCursorWindowSize() = (1024L * 1024L * 5L)
}
