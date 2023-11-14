package com.woocommerce.android.mediapicker

import org.wordpress.android.mediapicker.api.Log
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class MediaPickerLogger @Inject constructor() : Log {
    override fun e(message: String) {
        AppLog.e(AppLog.T.MEDIA, message)
    }

    override fun e(message: String, throwable: Throwable) {
        AppLog.e(AppLog.T.MEDIA, message, throwable)
    }

    override fun e(e: Throwable) {
        AppLog.e(AppLog.T.MEDIA, e)
    }

    override fun w(message: String) {
        AppLog.w(AppLog.T.MEDIA, message)
    }

    override fun d(message: String) {
        AppLog.d(AppLog.T.MEDIA, message)
    }

    override fun i(message: String) {
        AppLog.i(AppLog.T.MEDIA, message)
    }
}
