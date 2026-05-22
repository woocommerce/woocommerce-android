package com.woocommerce.android.ui.prefs.notifications

import com.automattic.eventhorizon.NotificationTypeValue
import com.automattic.eventhorizon.StockNotificationOptionValue
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.NotificationType
import com.woocommerce.android.ui.prefs.notifications.NotificationSettingsSharedViewModel.StockNotificationType

fun NotificationType.toEventHorizonValue(): NotificationTypeValue =
    when (this) {
        NotificationType.NEW_ORDERS -> NotificationTypeValue.NewOrder
        NotificationType.NEW_REVIEWS -> NotificationTypeValue.NewReview
        NotificationType.STOCK -> NotificationTypeValue.StockAlert
    }

fun StockNotificationType.toEventHorizonValue(): StockNotificationOptionValue =
    when (this) {
        StockNotificationType.LowStock -> StockNotificationOptionValue.LowStock
        StockNotificationType.OutOfStock -> StockNotificationOptionValue.OutOfStock
        StockNotificationType.Backorder -> StockNotificationOptionValue.OnBackorder
    }
