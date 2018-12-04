package com.woocommerce.android.util

import android.content.Context
import android.support.v4.app.NotificationManagerCompat

object NotificationsUtils {
    /**
     * Checks if global notifications toggle is enabled in the Android app settings.
     */
    fun isNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context.applicationContext).areNotificationsEnabled()
    }
}
