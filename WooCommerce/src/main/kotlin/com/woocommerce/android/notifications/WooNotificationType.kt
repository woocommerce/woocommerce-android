package com.woocommerce.android.notifications

import org.wordpress.android.fluxc.model.notification.NotificationModel

enum class WooNotificationType {
    NEW_ORDER,
    PRODUCT_REVIEW,
    LOCAL_REMINDER,
    BLAZE
}

fun NotificationModel.getWooType(): WooNotificationType {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> WooNotificationType.NEW_ORDER
        NotificationModel.Kind.COMMENT -> WooNotificationType.PRODUCT_REVIEW
        NotificationModel.Kind.BLAZE_APPROVED_NOTE,
        NotificationModel.Kind.BLAZE_REJECTED_NOTE,
        NotificationModel.Kind.BLAZE_CANCELLED_NOTE,
        NotificationModel.Kind.BLAZE_PERFORMED_NOTE -> WooNotificationType.BLAZE
        else -> WooNotificationType.LOCAL_REMINDER
    }
}
