package com.woocommerce.android.ui.dashboard.data

import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObservePushNotificationsWidgetStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus
) {
    operator fun invoke(): Flow<DashboardWidget.Status> = flow {
        if (!FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM_M2.isEnabled()) {
            emit(DashboardWidget.Status.Hidden)
            return@flow
        }

        val site = selectedSite.getIfExists()
        if (site == null || site.connectionType != SiteConnectionType.ApplicationPasswords) {
            emit(DashboardWidget.Status.Hidden)
            return@flow
        }

        if (pushNotificationRegistrationStatus() == PushNotificationRegistrationStatus.Status.REGISTERED) {
            emit(DashboardWidget.Status.Hidden)
            return@flow
        }

        emit(DashboardWidget.Status.Available)
    }
}
