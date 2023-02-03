package com.woocommerce.android.util

fun <R> Boolean?.ifTrue(block: () -> R): R? {
    return if (this == true) block() else null
}
