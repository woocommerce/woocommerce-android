package com.woocommerce.android.notifications

import com.woocommerce.android.notifications.WooNotificationType.NEW_ORDER
import com.woocommerce.android.notifications.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.notifications.WooNotificationType.ZENDESK
import org.wordpress.android.fluxc.model.notification.NotificationModel

enum class WooNotificationType {
    NEW_ORDER,
    PRODUCT_REVIEW,
    ZENDESK,
    REMINDER
}

fun NotificationModel.getWooType(): WooNotificationType {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> NEW_ORDER
        NotificationModel.Kind.COMMENT -> PRODUCT_REVIEW
        else -> ZENDESK
    }
}
