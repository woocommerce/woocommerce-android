package com.woocommerce.android.notifications

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.notification.NotificationModel

sealed interface WooNotificationType : Parcelable {
    @Parcelize
    data object NewOrder : WooNotificationType

    @Parcelize
    data object ProductReview : WooNotificationType

    @Parcelize
    data object LocalReminder : WooNotificationType

    @Parcelize
    sealed interface BlazeStatusUpdate : WooNotificationType, Parcelable {
        @Parcelize
        data object BlazeApprovedNote : BlazeStatusUpdate

        @Parcelize
        data object BlazeRejectedNote : BlazeStatusUpdate

        @Parcelize
        data object BlazeCancelledNote : BlazeStatusUpdate

        @Parcelize
        data object BlazePerformedNote : BlazeStatusUpdate
    }
}


fun NotificationModel.getWooType(): WooNotificationType {
    return when (this.type) {
        NotificationModel.Kind.STORE_ORDER -> WooNotificationType.NewOrder
        NotificationModel.Kind.COMMENT -> WooNotificationType.ProductReview
        NotificationModel.Kind.BLAZE_APPROVED_NOTE -> WooNotificationType.BlazeStatusUpdate.BlazeApprovedNote
        NotificationModel.Kind.BLAZE_REJECTED_NOTE -> WooNotificationType.BlazeStatusUpdate.BlazeRejectedNote
        NotificationModel.Kind.BLAZE_CANCELLED_NOTE -> WooNotificationType.BlazeStatusUpdate.BlazeCancelledNote
        NotificationModel.Kind.BLAZE_PERFORMED_NOTE -> WooNotificationType.BlazeStatusUpdate.BlazePerformedNote
        else -> WooNotificationType.LocalReminder
    }
}
