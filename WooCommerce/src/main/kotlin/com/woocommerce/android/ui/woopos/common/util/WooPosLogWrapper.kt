package com.woocommerce.android.ui.woopos.common.util

import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLogWrapper
import javax.inject.Inject

class WooPosLogWrapper @Inject constructor(
    private val wooLogWrapper: WooLogWrapper
) {
    fun d(message: String) {
        WooLog.d(WooLog.T.POS, message)
    }

    fun e(message: String) {
        wooLogWrapper.e(WooLog.T.POS, message)
    }

    fun w(message: String) {
        wooLogWrapper.w(WooLog.T.POS, message)
    }

    fun i(message: String) {
        wooLogWrapper.i(WooLog.T.POS, message)
    }

    companion object {
        fun d(message: String) {
            WooLog.d(WooLog.T.POS, message)
        }
    }
}
