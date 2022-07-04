package com.woocommerce.android.util

import android.os.Build

@Suppress("Unused")
object SystemVersionUtils {
    fun isAtLeastS() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isAtLeastR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun isAtLeastQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isAtLeastP() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P

    fun isAtLeastO() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    fun isAtLeastN() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N

    fun isAtLeastM() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
}
