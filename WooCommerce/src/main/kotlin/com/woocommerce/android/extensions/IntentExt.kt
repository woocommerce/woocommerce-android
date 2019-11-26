package com.woocommerce.android.extensions

import android.content.Intent

fun Intent?.getBoolean(key: String, defaultValue: Boolean = false): Boolean {
    return this?.getBooleanExtra(key, defaultValue) ?: defaultValue
}
