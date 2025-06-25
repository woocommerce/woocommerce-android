package com.woocommerce.android.ui.woopos.common.util

import com.woocommerce.android.util.WooLog
import dagger.Reusable
import javax.inject.Inject

@Reusable
class WooPosLogWrapper @Inject constructor() {
    fun d(message: String) {
        WooLog.d(WooLog.T.POS, message)
    }

    fun e(message: String) {
        WooLog.e(WooLog.T.POS, message)
    }

    fun w(message: String) {
        WooLog.w(WooLog.T.POS, message)
    }

    fun i(message: String) {
        WooLog.i(WooLog.T.POS, message)
    }

    companion object {
        fun d(message: String) {
            WooLog.d(WooLog.T.POS, message)
        }
    }
}
