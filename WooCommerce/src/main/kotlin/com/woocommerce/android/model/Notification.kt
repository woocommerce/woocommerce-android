package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.WooNotificationType
import com.woocommerce.android.notifications.getGroupId
import com.woocommerce.android.notifications.getWooType
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.notification.NotificationModel

/**
 * Represents a notification to be displayed to the user.
 *
 * This class is used for both remote (push) notifications and local notifications.
 * For remote notifications, all ID-related operations use [remoteNoteId].
 * For local notifications, the notification ID is passed separately to display methods.
 */
@Parcelize
data class Notification(
    val uniqueId: Long,
    val remoteNoteId: Long,
    val remoteSiteId: Long,
    val icon: String?,
    val noteTitle: String,
    val noteMessage: String?,
    val noteType: WooNotificationType,
    val channelType: NotificationChannelType,
    val tag: String? = null,
    val data: String? = null
) : Parcelable {
    @IgnoredOnParcel
    val isOrderNotification = noteType is WooNotificationType.NewOrder

    @IgnoredOnParcel
    val isReviewNotification = noteType is WooNotificationType.ProductReview

    @IgnoredOnParcel
    val isBlazeNotification = noteType is WooNotificationType.BlazeStatusUpdate

    /**
     * Notifications are grouped based on the notification type and the store the notification belongs to.
     *
     * For instance: a new order notification from Store 1 with remoteSiteId = 12345, would return:
     * "NEW_ORDER 12345"
     */
    fun getGroup(): String = "${channelType.name} $remoteSiteId"

    /**
     * This method returns a group notification id based on the notification type
     * and the store the notification belongs to
     */
    fun getGroupPushId(): Int = channelType.getGroupId() + remoteSiteId.toInt()
}

fun NotificationModel.toAppModel(resourceProvider: ResourceProvider): Notification {
    return Notification(
        remoteNoteId = this.remoteNoteId,
        remoteSiteId = this.remoteSiteId,
        icon = this.icon,
        noteTitle = getNoteTitle(resourceProvider),
        noteMessage = getNoteMessage(resourceProvider),
        noteType = getWooType(),
        channelType = getChannelType(),
        uniqueId = getUniqueId()
    )
}

fun NotificationModel.isOrderNotification() = this.type == NotificationModel.Kind.STORE_ORDER

fun NotificationModel.getChannelType(): NotificationChannelType {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> NotificationChannelType.NEW_ORDER
        NotificationModel.Kind.COMMENT -> NotificationChannelType.REVIEW
        NotificationModel.Kind.STORE_STOCK -> NotificationChannelType.STOCK
        else -> NotificationChannelType.OTHER
    }
}

fun NotificationModel.getUniqueId(): Long {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> this.meta?.ids?.order ?: 0L
        NotificationModel.Kind.COMMENT -> this.meta?.ids?.comment ?: 0L
        NotificationModel.Kind.STORE_STOCK -> this.meta?.ids?.product ?: 0L
        NotificationModel.Kind.BLAZE_APPROVED_NOTE,
        NotificationModel.Kind.BLAZE_REJECTED_NOTE,
        NotificationModel.Kind.BLAZE_CANCELLED_NOTE,
        NotificationModel.Kind.BLAZE_PERFORMED_NOTE -> this.meta?.ids?.campaignId ?: 0L

        else -> 0L
    }
}

fun NotificationModel.getNoteTitle(resourceProvider: ResourceProvider): String {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> resourceProvider.getString(R.string.notification_order_title)
        NotificationModel.Kind.COMMENT -> resourceProvider.getString(R.string.notification_review_title)
        NotificationModel.Kind.STORE_STOCK ->
            title
                ?: getTitleSnippet()
                ?: resourceProvider.getString(R.string.settings_notifs_stock)
        else -> resourceProvider.getString(R.string.support_push_notification_title)
    }
}

fun NotificationModel.getNoteMessage(resourceProvider: ResourceProvider): String? {
    return when (type) {
        NotificationModel.Kind.STORE_ORDER, NotificationModel.Kind.STORE_STOCK -> getMessageSnippet()
        NotificationModel.Kind.COMMENT -> "${getTitleSnippet()}: ${getMessageSnippet()}"
        NotificationModel.Kind.BLAZE_APPROVED_NOTE,
        NotificationModel.Kind.BLAZE_REJECTED_NOTE,
        NotificationModel.Kind.BLAZE_CANCELLED_NOTE,
        NotificationModel.Kind.BLAZE_PERFORMED_NOTE -> getTitleSnippet()

        else -> resourceProvider.getString(R.string.support_push_notification_message)
    }
}

fun NotificationModel.getTitleSnippet() = this.subject?.getOrNull(0)?.text?.split('\n')?.first()

fun NotificationModel.getMessageSnippet() = this.subject?.getOrNull(1)?.text?.split('\n')?.first()
