package com.woocommerce.android.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import org.wordpress.android.util.BuildConfig

object PackageUtils {
    const val PACKAGE_VERSION_CODE_DEFAULT = -1

    /**
     * Return true if Debug build. false otherwise.
     */
    fun isDebugBuild(): Boolean {
        return BuildConfig.DEBUG
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        try {
            val manager = context.packageManager
            return manager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return null
        }
    }

    /**
     * Return version code, or -1 if it can't be read
     */
    fun getVersionCode(context: Context): Int {
        val packageInfo = getPackageInfo(context)
        return packageInfo?.let {
            PackageInfoCompat.getLongVersionCode(it).toInt()
        } ?: PACKAGE_VERSION_CODE_DEFAULT
    }

    /**
     * Return version name, or the string "0" if it can't be read
     */
    fun getVersionName(context: Context): String {
        val packageInfo = getPackageInfo(context)
        return packageInfo?.versionName ?: "0"
    }
}
