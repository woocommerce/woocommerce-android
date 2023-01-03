package com.woocommerce.android

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooLog
import leakcanary.AppWatcher

internal class LeakCanaryInstaller : ContentProvider() {
    override fun onCreate(): Boolean {
        if (!PackageUtils.isTesting()) {
            WooLog.v(WooLog.T.DEVICE, "Installing LeakCanary")
            val application = context!!.applicationContext as Application
            AppWatcher.manualInstall(application)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projectionArg: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
