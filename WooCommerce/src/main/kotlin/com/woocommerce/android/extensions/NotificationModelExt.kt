package com.woocommerce.android.extensions

import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.WooNotificationType
import com.woocommerce.android.push.getWooType
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.model.notification.NotificationModel.Kind.COMMENT

/**
 * Parse and return the remote order id.
 *
 * Returns null if the notification is not of type new-order or if an ID cannot be parsed.
 */
fun NotificationModel.getRemoteOrderId(): Long? {
    if (this.getWooType() != WooNotificationType.NEW_ORDER) {
        return null
    }

    return this.meta?.ids?.order
}

/**
 * Returns the remote comment_id associated with this notification. Product reviews are comments under the
 * hood so only parse the comment_id if the notification is a product review. This id can be used to fetch
 * the [org.wordpress.android.fluxc.model.CommentModel] from the API which is required for moderating the
 * review.
 */
fun NotificationModel.getCommentId(): Long {
    if (this.type != COMMENT) {
        return 0L
    }

    return this.meta?.ids?.comment ?: 0L
}

class NotificationsUnseenReviewsEvent(var hasUnseen: Boolean)

class NotificationReceivedEvent(var channel: NotificationChannelType)
