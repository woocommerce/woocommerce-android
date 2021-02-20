package com.woocommerce.android.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import org.wordpress.android.util.BuildConfig
import java.util.Locale

object PackageUtils {
    const val PACKAGE_VERSION_CODE_DEFAULT = -1

    /**
     * Return true if Debug build. false otherwise.
     */
    fun isDebugBuild() = BuildConfig.DEBUG

    fun isTesting(): Boolean {
        return try {
            Class.forName("com.woocommerce.android.viewmodel.BaseUnitTest")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    fun isBetaBuild(context: Context): Boolean {
        val versionName = getVersionName(context).toLowerCase(Locale.ROOT)
        return (versionName.contains("beta") || versionName.contains("rc"))
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            val manager = context.packageManager
            manager.getPackageInfo(context.packageName, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            null
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
    fun getVersionName(context: Context) = getPackageInfo(context)?.versionName ?: "0"
}
