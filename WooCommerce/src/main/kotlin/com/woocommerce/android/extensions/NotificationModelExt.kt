package com.woocommerce.android.extensions

import android.annotation.SuppressLint
import android.os.Parcelable
import com.woocommerce.android.extensions.WooNotificationType.NEW_ORDER
import com.woocommerce.android.extensions.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.extensions.WooNotificationType.UNKNOWN
import com.woocommerce.android.ui.notifications.NotificationHelper
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.util.DateTimeUtils

enum class WooNotificationType {
    NEW_ORDER,
    PRODUCT_REVIEW,
    UNKNOWN
}

/**
 * Returns a simplified Woo Notification type.
 *
 * Currently there are only two supported types: NEW_ORDER and PRODUCT_REVIEW.
 */
fun NotificationModel.getWooType(): WooNotificationType {
    return when {
        this.type == NotificationModel.Kind.STORE_ORDER -> NEW_ORDER
        this.subtype == NotificationModel.Subkind.STORE_REVIEW -> PRODUCT_REVIEW
        else -> UNKNOWN
    }
}

/**
 * Parse and return the product rating.
 *
 * Returns null if the notification is not a product review, or if unable to
 * successfully parse a rating from this notification.
 */
fun NotificationModel.getRating(): Float? {
    if (this.getWooType() != PRODUCT_REVIEW) {
        return null
    }

    return this.body?.get(2)?.text?.count { it == '\u2605' }?.takeIf { it > 0.0f }?.toFloat()
}

/**
 * Generate and return the product information.
 *
 * Returns null if the notification is not a product review, or if unable to
 * successfully parse product information from this notification.
 */
fun NotificationModel.getProductInfo(): NotificationProductInfo? {
    if (this.getWooType() != PRODUCT_REVIEW) {
        return null
    }

    val name = NotificationHelper.getProductName(this)
    val url = NotificationHelper.getProductUrl(this)
    return name?.let { n -> url?.let { u -> NotificationProductInfo(n, u) } }
}

/**
 * Generate and return the user information.
 *
 * Returns null if a user name cannot be parsed.
 */
fun NotificationModel.getUserInfo(): NotificationUserInfo? {
    return this.body?.asSequence()?.filter { it.type == "user" }?.first()?.let { block ->
        block.text?.let { name ->
            val url = block.media?.asSequence()?.filter { it.type == "image" }?.first()?.url
            val email = block.meta?.links?.email
            NotificationUserInfo(name, url, email)
        }
    }
}

/**
 * Generate and return the review detail.
 *
 * Returns null if the notification is not of type review.
 */
fun NotificationModel.getReviewDetail(): NotificationReviewDetail? {
    if (this.getWooType() != PRODUCT_REVIEW) {
        return null
    }

    val userInfo = getUserInfo()
    val rating = getRating()
    return NotificationReviewDetail(
            getMessageDetail(),
            getConvertedTimestamp(),
            getRating(),
            getUserInfo(),
            getProductInfo()
    )
}

/**
 * Parse and return the notification detail or an empty string if none found.
 */
fun NotificationModel.getMessageDetail() = this.body?.asSequence()?.filter { it.type == "comment" }?.first()?.text ?: ""

/**
 * Parse and return the notification snippet to be displayed in the notification list.
 */
fun NotificationModel.getTitleSnippet() = this.subject?.get(0)?.text?.split('\n')?.first()

fun NotificationModel.getMessageSnippet() = this.subject?.get(1)?.text?.split('\n')?.first()

/**
 * Parse and return the remote order id.
 *
 * Returns null if the notification is not of type new-order or if an ID cannot be parsed.
 */
fun NotificationModel.getRemoteOrderId(): Long? {
    if (this.getWooType() != NEW_ORDER) {
        return null
    }

    return this.meta?.ids?.order
}

/**
 * Converts the ISO8601 String into a Date() object and returns that date represented as the number of
 * milliseconds since January 1, 1970, 00:00:00 GMT.
 */
fun NotificationModel.getConvertedTimestamp(): Long = DateTimeUtils.timestampFromIso8601(timestamp)

/**
 * Returns the remote comment_id associated with this notification. Product reviews are comments under the
 * hood so only parse the comment_id if the notification is a product review. This id can be used to fetch
 * the [org.wordpress.android.fluxc.model.CommentModel] from the API which is required for moderating the
 * review.
 */
fun NotificationModel.getCommentId(): Long? {
    if (this.getWooType() != PRODUCT_REVIEW) {
        return null
    }

    return this.meta?.ids?.comment
}

// TODO: Temporarily suppress lint errors around ParcelCreator due to this error:
// https://youtrack.jetbrains.com/issue/KT-19300
@SuppressLint("ParcelCreator")
@Parcelize
data class NotificationProductInfo(val name: String, val url: String) : Parcelable

@SuppressLint("ParcelCreator")
@Parcelize
data class NotificationUserInfo(val name: String, val iconUrl: String?, val email: String?) : Parcelable

@SuppressLint("ParcelCreator")
@Parcelize
data class NotificationReviewDetail(
    val msg: String,
    val timestamp: Long,
    val rating: Float?,
    val userInfo: NotificationUserInfo?,
    val productInfo: NotificationProductInfo?
) : Parcelable
