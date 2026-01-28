package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.notifications.push.ShouldShowEnablePushNotificationsUi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObservePushNotificationsWidgetStatus @Inject constructor(
    private val shouldShowEnablePushNotificationsUi: ShouldShowEnablePushNotificationsUi
) {
    operator fun invoke(): Flow<DashboardWidget.Status> = flow {
        if (shouldShowEnablePushNotificationsUi()) {
            emit(DashboardWidget.Status.Available)
        } else {
            emit(DashboardWidget.Status.Hidden)
        }
    }
}
