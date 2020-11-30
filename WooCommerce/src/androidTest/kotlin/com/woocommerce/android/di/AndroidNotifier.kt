package com.woocommerce.android.di

import android.util.Log
import com.github.tomakehurst.wiremock.common.Notifier

class AndroidNotifier : Notifier {
    override fun info(message: String) {
        Log.i(TAG, message)
    }

    override fun error(message: String) {
        Log.e(TAG, message)
    }

    override fun error(message: String, t: Throwable) {
        Log.e(TAG, message, t)
    }

    companion object {
        private const val TAG = "WooCommerceMocks"
    }
}
