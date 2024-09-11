package com.woocommerce.android.notifications

import android.os.Parcelable
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.notification.NotificationModel

sealed interface WooNotificationType : Parcelable {
    val trackingValue: String

    @Parcelize
    data object NewOrder : WooNotificationType {
        @IgnoredOnParcel override val trackingValue: String = "NEW_ORDER"
    }

    @Parcelize
    data object ProductReview : WooNotificationType {
        @IgnoredOnParcel override val trackingValue: String = "PRODUCT_REVIEW"
    }

    @Parcelize
    data object LocalReminder : WooNotificationType {
        @IgnoredOnParcel override val trackingValue: String = "LOCAL_REMINDER"
    }

    @Parcelize
    sealed interface BlazeStatusUpdate : WooNotificationType, Parcelable {
        @Parcelize
        data object BlazeApprovedNote : BlazeStatusUpdate {
            @IgnoredOnParcel override val trackingValue: String = "blaze_approved_note"
        }

        @Parcelize
        data object BlazeRejectedNote : BlazeStatusUpdate {
            @IgnoredOnParcel override val trackingValue: String = "blaze_rejected_note"
        }

        @Parcelize
        data object BlazeCancelledNote : BlazeStatusUpdate {
            @IgnoredOnParcel override val trackingValue: String = "blaze_cancelled_note"
        }

        @Parcelize
        data object BlazePerformedNote : BlazeStatusUpdate {
            @IgnoredOnParcel override val trackingValue: String = "blaze_performed_note"
        }
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
