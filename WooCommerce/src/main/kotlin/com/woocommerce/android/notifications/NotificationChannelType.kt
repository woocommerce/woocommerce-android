package com.woocommerce.android.notifications

import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.NotificationChannelType.NEW_ORDER
import com.woocommerce.android.notifications.NotificationChannelType.OTHER
import com.woocommerce.android.notifications.NotificationChannelType.REVIEW

enum class NotificationChannelType {
    NEW_ORDER,
    REVIEW,
    OTHER
}

private const val GROUP_NOTIFICATION_ID_ORDER = 30001
private const val GROUP_NOTIFICATION_ID_REVIEW = 30002
private const val GROUP_NOTIFICATION_ID_OTHER = 30003

@StringRes
fun NotificationChannelType.getGroupId(): Int {
    return when (this) {
        NEW_ORDER -> GROUP_NOTIFICATION_ID_ORDER
        REVIEW -> GROUP_NOTIFICATION_ID_REVIEW
        OTHER -> GROUP_NOTIFICATION_ID_OTHER
    }
}

fun NotificationChannelType.shouldCircularizeNoteIcon(): Boolean {
    return when (this) {
        REVIEW -> true
        else -> false
    }
}

fun NotificationChannelType.getDefaults(appPrefsWrapper: AppPrefsWrapper): Int {
    return when {
        this == NEW_ORDER && appPrefsWrapper.isOrderNotificationsChaChingEnabled() -> {
            NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE
        } else -> NotificationCompat.DEFAULT_ALL
    }
}
