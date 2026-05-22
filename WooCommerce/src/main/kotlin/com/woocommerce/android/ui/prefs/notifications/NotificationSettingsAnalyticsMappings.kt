package com.woocommerce.android.ui.prefs.notifications

import com.automattic.eventhorizon.NotificationFilterOptionValue
import com.automattic.eventhorizon.NotificationTypeValue
import com.automattic.eventhorizon.StockNotificationOptionValue
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.NewOrderNotificationPreference
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.NewReviewNotificationPreference
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.NotificationType
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.StockNotificationType

fun NotificationType.toEventHorizonValue(): NotificationTypeValue =
    when (this) {
        NotificationType.NEW_ORDERS -> NotificationTypeValue.NewOrder
        NotificationType.NEW_REVIEWS -> NotificationTypeValue.NewReview
        NotificationType.STOCK -> NotificationTypeValue.StockAlert
    }

fun NewOrderNotificationPreference.toEventHorizonValue(): NotificationFilterOptionValue =
    when (this) {
        NewOrderNotificationPreference.AllOrders -> NotificationFilterOptionValue.All
        NewOrderNotificationPreference.HighValueOrders -> NotificationFilterOptionValue.Filtered
    }

fun NewReviewNotificationPreference.toEventHorizonValue(): NotificationFilterOptionValue =
    when (this) {
        NewReviewNotificationPreference.AllReviews -> NotificationFilterOptionValue.All
        NewReviewNotificationPreference.RatingFilteredReviews -> NotificationFilterOptionValue.Filtered
    }

fun StockNotificationType.toEventHorizonValue(): StockNotificationOptionValue =
    when (this) {
        StockNotificationType.LowStock -> StockNotificationOptionValue.LowStock
        StockNotificationType.OutOfStock -> StockNotificationOptionValue.OutOfStock
        StockNotificationType.Backorder -> StockNotificationOptionValue.OnBackorder
    }
