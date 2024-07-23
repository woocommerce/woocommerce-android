package com.woocommerce.android.notifications

import androidx.annotation.StringRes
import com.woocommerce.android.notifications.NotificationChannelType.BACKGROUND_WORKS
import com.woocommerce.android.notifications.NotificationChannelType.NEW_ORDER
import com.woocommerce.android.notifications.NotificationChannelType.OTHER
import com.woocommerce.android.notifications.NotificationChannelType.REVIEW

enum class NotificationChannelType {
    NEW_ORDER,
    REVIEW,
    BACKGROUND_WORKS,
    OTHER
}

private const val GROUP_NOTIFICATION_ID_ORDER = 30001
private const val GROUP_NOTIFICATION_ID_REVIEW = 30002
private const val GROUP_NOTIFICATION_ID_OTHER = 30003
private const val GROUP_NOTIFICATION_ID_BACKGROUND_WORKS = 30004

@StringRes
fun NotificationChannelType.getGroupId(): Int {
    return when (this) {
        NEW_ORDER -> GROUP_NOTIFICATION_ID_ORDER
        REVIEW -> GROUP_NOTIFICATION_ID_REVIEW
        OTHER -> GROUP_NOTIFICATION_ID_OTHER
        BACKGROUND_WORKS -> GROUP_NOTIFICATION_ID_BACKGROUND_WORKS
    }
}

fun NotificationChannelType.shouldCircularizeNoteIcon(): Boolean {
    return when (this) {
        REVIEW -> true
        else -> false
    }
}
