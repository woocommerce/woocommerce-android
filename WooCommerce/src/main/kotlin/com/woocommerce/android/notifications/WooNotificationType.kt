package com.woocommerce.android.notifications

import org.wordpress.android.fluxc.model.notification.NotificationModel

enum class WooNotificationType {
    NEW_ORDER,
    PRODUCT_REVIEW,
    LOCAL_REMINDER,
    BLAZE;
}

fun NotificationModel.getWooType(): WooNotificationType {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> WooNotificationType.NEW_ORDER
        NotificationModel.Kind.COMMENT -> WooNotificationType.PRODUCT_REVIEW
        else -> WooNotificationType.LOCAL_REMINDER
    }
}
