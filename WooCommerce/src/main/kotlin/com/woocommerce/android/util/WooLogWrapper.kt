package com.woocommerce.android.util

import javax.inject.Inject

class WooLogWrapper @Inject constructor() {
    fun provideLogs(): String = WooLog.toString()
}
