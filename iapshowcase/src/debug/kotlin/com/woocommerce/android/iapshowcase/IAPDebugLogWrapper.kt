package com.woocommerce.android.iapshowcase

import android.util.Log
import com.woocommerce.android.iap.public.LogWrapper

class IAPDebugLogWrapper : LogWrapper {
    override fun w(tag: String, message: String) {
        Log.w(tag, message)
    }

    override fun d(tag: String, message: String) {
        Log.d(tag, message)
    }

    override fun e(tag: String, message: String) {
        Log.e(tag, message)
    }
}
