package com.woocommerce.android.extensions

import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import com.woocommerce.android.util.SystemVersionUtils

fun PackageManager.packageInfo(packageName: String, flags: Int): PackageInfo = when {
    SystemVersionUtils.isAtLeastT() -> getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(flags.toLong()))
    else ->
        @Suppress("DEPRECATION")
        getPackageInfo(packageName, flags)
}

fun PackageManager.intentActivities(intent: Intent, flag: Int): List<ResolveInfo> = when {
    SystemVersionUtils.isAtLeastT() -> queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(flag.toLong()))
    else ->
        @Suppress("DEPRECATION")
        queryIntentActivities(intent, flag)
}

fun PackageManager.service(intent: Intent, flag: Int): ResolveInfo? = when {
    SystemVersionUtils.isAtLeastT() -> resolveService(intent, PackageManager.ResolveInfoFlags.of(flag.toLong()))
    else ->
        @Suppress("DEPRECATION")
        resolveService(intent, flag)
}
