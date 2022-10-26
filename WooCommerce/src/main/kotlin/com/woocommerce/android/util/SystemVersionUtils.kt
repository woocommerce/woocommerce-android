package com.woocommerce.android.util

import android.os.Build

@Suppress("Unused", "TooManyFunctions")
object SystemVersionUtils {
    fun isAtLeastT() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    fun isAtMostT() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.TIRAMISU

    fun isAtLeastS() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    fun isAtMostS() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.S

    fun isAtLeastR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
    fun isAtMostR() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.R

    fun isAtLeastQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    fun isAtMostQ() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q

    fun isAtLeastP() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
    fun isAtMostP() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.P

    fun isAtLeastO() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    fun isAtMostO() = Build.VERSION.SDK_INT <= Build.VERSION_CODES.O
}
