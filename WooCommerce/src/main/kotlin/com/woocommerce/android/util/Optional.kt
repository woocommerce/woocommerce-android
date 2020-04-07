package com.woocommerce.android.util

class Optional<T>(var value: T? = null) {
    val hasValue: Boolean
        get() = value != null
}
