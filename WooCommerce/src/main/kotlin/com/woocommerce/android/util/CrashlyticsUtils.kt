package com.woocommerce.android.util

import com.crashlytics.android.Crashlytics

import io.fabric.sdk.android.Fabric
import org.wordpress.android.util.AppLog.T

object CrashlyticsUtils {
    private const val TAG_KEY = "tag"
    private const val MESSAGE_KEY = "message"

    fun logException(tr: Throwable, tag: T? = null, message: String? = null) {
        if (!Fabric.isInitialized()) { return }

        tag?.let { Crashlytics.setString(TAG_KEY, it.name) }

        message?.let { Crashlytics.setString(MESSAGE_KEY, it) }

        Crashlytics.logException(tr)
    }

    fun log(message: String) {
        if (!Fabric.isInitialized()) { return }

        Crashlytics.log(message)
    }
}
