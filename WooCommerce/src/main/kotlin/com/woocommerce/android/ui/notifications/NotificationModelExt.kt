package com.woocommerce.android.ui.notifications

import com.woocommerce.android.ui.notifications.WooNotificationType.NEW_ORDER
import com.woocommerce.android.ui.notifications.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.ui.notifications.WooNotificationType.UNKNOWN
import org.wordpress.android.fluxc.model.NotificationModel

enum class WooNotificationType {
    NEW_ORDER,
    PRODUCT_REVIEW,
    UNKNOWN
}

/**
 * Returns a simplified Woo Notification type
 */
fun NotificationModel.getWooType(): WooNotificationType {
    return when {
        this.type == NotificationModel.Kind.STORE_ORDER -> NEW_ORDER
        this.subtype == NotificationModel.Subkind.STORE_REVIEW -> PRODUCT_REVIEW
        else -> UNKNOWN
    }
}
