package com.woocommerce.android.cardreader.internal.wrappers

interface LogWrapper {
    fun w(tag: String, message: String)
    fun d(tag: String, message: String)
    fun e(tag: String, message: String)
}
