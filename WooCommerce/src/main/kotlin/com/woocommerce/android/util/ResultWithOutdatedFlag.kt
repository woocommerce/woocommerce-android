package com.woocommerce.android.util

data class ResultWithOutdatedFlag<T>(
    val value: T,
    val isOutdated: Boolean = false
)
