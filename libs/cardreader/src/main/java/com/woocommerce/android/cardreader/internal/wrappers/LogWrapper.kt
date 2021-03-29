package com.woocommerce.android.cardreader.internal.wrappers

import android.util.Log

class LogWrapper {
    fun w(tag: String, message: String) = Log.w(tag, message)
}
