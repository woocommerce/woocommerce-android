package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.notifications.push.ShouldShowEnablePushNotificationsUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ObservePushNotificationsWidgetStatus @Inject constructor(
    private val shouldShowEnablePushNotificationsUi: ShouldShowEnablePushNotificationsUi
) {
    operator fun invoke(): Flow<DashboardWidget.Status> =
        shouldShowEnablePushNotificationsUi().map { shouldShow ->
            if (shouldShow) DashboardWidget.Status.Available else DashboardWidget.Status.Hidden
        }
}
