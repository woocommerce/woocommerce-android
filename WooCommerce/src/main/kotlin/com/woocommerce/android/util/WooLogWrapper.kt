package com.woocommerce.android.util

import javax.inject.Inject

class WooLogWrapper @Inject constructor() {
    fun provideLogs(): String = WooLog.toString()

    fun e(tag: WooLog.T, message: String) {
        WooLog.e(tag, message)
    }

    fun w(tag: WooLog.T, message: String) {
        WooLog.w(tag, message)
    }

    fun i(tag: WooLog.T, message: String) {
        WooLog.i(tag, message)
    }
}
