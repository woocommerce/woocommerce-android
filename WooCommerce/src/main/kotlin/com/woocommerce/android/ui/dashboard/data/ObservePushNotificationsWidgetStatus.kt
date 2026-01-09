package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject

class ObservePushNotificationsWidgetStatus @Inject constructor() {
    operator fun invoke(): Flow<DashboardWidget.Status> = flowOf(
        // TODO Add logic to check token registration status
        if (FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM.isEnabled()) {
            DashboardWidget.Status.Available
        } else {
            DashboardWidget.Status.Hidden
        }
    )
}
