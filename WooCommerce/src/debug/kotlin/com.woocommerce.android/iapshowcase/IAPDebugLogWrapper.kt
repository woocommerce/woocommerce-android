package com.woocommerce.android.iapshowcase

import android.util.Log
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.iap.pub.IAPLogWrapper

class IAPDebugLogWrapper : IAPLogWrapper {
    override fun w(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.w(tag, message)
    }

    override fun d(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.d(tag, message)
    }

    override fun e(tag: String, message: String) {
        if (BuildConfig.DEBUG) Log.e(tag, message)
    }
}
