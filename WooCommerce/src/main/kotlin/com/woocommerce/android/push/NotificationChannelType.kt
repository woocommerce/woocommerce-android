package com.woocommerce.android.push

import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.push.NotificationChannelType.*

/**
 * Note that we have separate notification channels for orders with and without the cha-ching sound - this is
 * necessary because once a channel is created we can't change it, and if we delete the channel and re-create
 * it then it will be re-created with the same settings it previously had (ie: we can't simply have a single
 * channel for orders and add/remove the sound from it)
 */
enum class NotificationChannelType {
    NEW_ORDER,
    REVIEW,
    OTHER;
}

private const val GROUP_NOTIFICATION_ID_ORDER = 30001
private const val GROUP_NOTIFICATION_ID_REVIEW = 30002
private const val GROUP_NOTIFICATION_ID_OTHER = 30003

@StringRes
fun NotificationChannelType.getChannelId(): Int {
    return when (this) {
        NEW_ORDER -> R.string.notification_channel_order_id
        REVIEW -> R.string.notification_channel_review_id
        OTHER -> R.string.notification_channel_general_id
    }
}

@StringRes
fun NotificationChannelType.getChannelTitle(): Int {
    return when (this) {
        NEW_ORDER -> R.string.notification_channel_order_title
        REVIEW -> R.string.notification_channel_review_title
        OTHER -> R.string.notification_channel_general_title
    }
}

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
