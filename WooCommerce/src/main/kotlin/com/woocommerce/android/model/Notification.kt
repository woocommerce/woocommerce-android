package com.woocommerce.android.model

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.WooNotificationType
import com.woocommerce.android.push.getWooType
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.notification.NotificationModel

@Parcelize
data class Notification(
    val noteId: Int,
    val uniqueId: Long,
    val remoteNoteId: Long,
    val remoteSiteId: Long,
    val icon: String?,
    val noteTitle: String,
    val noteMessage: String?,
    val noteType: WooNotificationType,
    val channelType: NotificationChannelType
): Parcelable {
    @IgnoredOnParcel
    val isOrderNotification = noteType == WooNotificationType.NEW_ORDER

    @IgnoredOnParcel
    val isReviewNotification = noteType == WooNotificationType.PRODUCT_REVIEW
}

fun NotificationModel.toAppModel(resourceProvider: ResourceProvider): Notification {
    return Notification(
        noteId = this.noteId,
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
        else -> NotificationChannelType.OTHER
    }
}

fun NotificationModel.getUniqueId(): Long {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> this.meta?.ids?.order ?: 0L
        NotificationModel.Kind.COMMENT -> this.meta?.ids?.comment ?: 0L
        else -> 0L
    }
}

fun NotificationModel.getNoteTitle(resourceProvider: ResourceProvider): String {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> resourceProvider.getString(R.string.notification_order_title)
        NotificationModel.Kind.COMMENT -> resourceProvider.getString(R.string.notification_review_title)
        else -> resourceProvider.getString(R.string.support_push_notification_title)
    }
}

fun NotificationModel.getNoteMessage(resourceProvider: ResourceProvider): String? {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> this.getMessageSnippet()
        NotificationModel.Kind.COMMENT -> "${this.getTitleSnippet()}: ${this.getMessageSnippet()}"
        else -> resourceProvider.getString(R.string.support_push_notification_message)
    }
}

fun NotificationModel.getTitleSnippet() = this.subject?.get(0)?.text?.split('\n')?.first()

fun NotificationModel.getMessageSnippet() = this.subject?.get(1)?.text?.split('\n')?.first()
