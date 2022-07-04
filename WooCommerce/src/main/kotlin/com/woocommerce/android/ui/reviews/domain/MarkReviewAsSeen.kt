package com.woocommerce.android.ui.reviews.domain

import com.woocommerce.android.push.NotificationMessageHandler
import com.woocommerce.android.ui.reviews.ReviewDetailRepository
import org.wordpress.android.fluxc.model.notification.NotificationModel
import javax.inject.Inject

open class MarkReviewAsSeen @Inject constructor(
    private val repository: ReviewDetailRepository,
    private val notificationHandler: NotificationMessageHandler
) {
    suspend operator fun invoke(remoteReviewId: Long, notification: NotificationModel) {
        notificationHandler.removeNotificationByRemoteIdFromSystemsBar(notification.remoteNoteId)
        repository.markNotificationAsRead(notification, remoteReviewId)
    }
}
