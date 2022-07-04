package com.woocommerce.android.push

import com.woocommerce.android.push.WooNotificationType.NEW_ORDER
import com.woocommerce.android.push.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.push.WooNotificationType.ZENDESK
import org.wordpress.android.fluxc.model.notification.NotificationModel

enum class WooNotificationType {
    NEW_ORDER,
    PRODUCT_REVIEW,
    ZENDESK
}

fun NotificationModel.getWooType(): WooNotificationType {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> NEW_ORDER
        NotificationModel.Kind.COMMENT -> PRODUCT_REVIEW
        else -> ZENDESK
    }
}
