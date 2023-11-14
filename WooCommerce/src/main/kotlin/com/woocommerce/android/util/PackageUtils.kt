package com.woocommerce.android.util

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import androidx.core.content.pm.PackageInfoCompat
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.extensions.packageInfo
import java.util.Locale

object PackageUtils {
    const val PACKAGE_VERSION_CODE_DEFAULT = -1

    private var isTesting: Boolean? = null

    /**
     * Return true if Debug build. false otherwise.
     */
    fun isDebugBuild() = BuildConfig.DEBUG

    fun isTesting(): Boolean {
        if (isTesting == null) {
            isTesting = try {
                Class.forName("org.junit.Test")
                true
            } catch (e: ClassNotFoundException) {
                false
            }
        }
        return isTesting!!
    }

    fun isBetaBuild(context: Context): Boolean {
        val versionName = getVersionName(context).lowercase(Locale.ROOT)
        return (versionName.contains("beta") || versionName.contains("rc"))
    }

    private fun getPackageInfo(context: Context): PackageInfo? {
        return try {
            val manager = context.packageManager
            manager.packageInfo(context.packageName, 0)
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
