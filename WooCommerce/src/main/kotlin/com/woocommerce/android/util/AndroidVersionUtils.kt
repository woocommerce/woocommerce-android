package com.woocommerce.android.util

import android.os.Build

object AndroidVersionUtils {
    fun isAtLeastS() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun isAtLeastR() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    fun isAtLeastQ() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    fun isAtLeastO() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
}
