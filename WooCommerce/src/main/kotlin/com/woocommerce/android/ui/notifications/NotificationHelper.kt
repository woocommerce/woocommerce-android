package com.woocommerce.android.ui.notifications

import com.woocommerce.android.extensions.WooNotificationType
import com.woocommerce.android.extensions.getWooType
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.tools.FormattableContent
import org.wordpress.android.fluxc.tools.FormattableRangeType.POST

/**
 * Helper class for parsing values from the various [FormattableContent] blocks contained inside the
 * [NotificationModel] object.
 */
object NotificationHelper {
    /**
     * Parses the product name from the appropriate subject block of the notification object. This is only
     * relevant for product review notifications. The product name is the name of the product reviewed.
     *
     * @return null if the notification is not a product review, or if the product name
     * is not found.
     */
    fun getProductName(notif: NotificationModel): String? {
        // Exit immediately if not a product review notification
        if (notif.getWooType() != WooNotificationType.PRODUCT_REVIEW) {
            return null
        }

        return notif.subject?.get(0)?.let { block ->
            val fullText = block.text ?: ""
            val post = block.ranges?.asSequence()?.filter { it.rangeType() == POST }?.firstOrNull()
            post?.let {
                val start = it.indices?.get(0) ?: 0
                val end = it.indices?.get(1) ?: 0

                fullText.substring(start, end)
            }
        }
    }

    /**
     * Parses the product URL from the appropriate subject block of the notification object. This is only
     * relevant for product review notifications. The URL opens the detail web page of the product
     * the review was left on.
     *
     * @return null if the notification is not a product review, or if the product URL
     * is not found.
     */
    fun getProductUrl(notif: NotificationModel): String? {
        // Exit immediately if not a product review notification
        if (notif.getWooType() != WooNotificationType.PRODUCT_REVIEW) {
            return null
        }

        return notif.subject?.get(0)?.let { block ->
            block.ranges?.asSequence()?.filter { it.rangeType() == POST }?.firstOrNull()?.url
        }
    }

    /**
     * Parses the product id from the list of meta ids
     *
     * @return zero if the notification is not a product review
     */
    fun getProductRemoteId(notif: NotificationModel): Long? {
        if (notif.getWooType() != WooNotificationType.PRODUCT_REVIEW) {
            return null
        }

        // products are posts
        return notif.meta?.ids?.post
    }

    /**
     * Parses the comment block from the body of a notification object.
     */
    fun getCommentBlockFromBody(notif: NotificationModel) =
            notif.body?.asSequence()?.filter { it.type == "comment" }?.firstOrNull()
}
