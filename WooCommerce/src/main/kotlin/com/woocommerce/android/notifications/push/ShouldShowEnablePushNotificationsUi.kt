package com.woocommerce.android.notifications.push

import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.util.FeatureFlag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Checks whether the "Enable Push Notifications" UI should be shown.
 *
 * This is part of the Woo Core push notifications system for non-Jetpack sites
 * (app-password authenticated sites and Jetpack Connection Package sites).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShouldShowEnablePushNotificationsUi @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus
) {
    operator fun invoke(): Flow<Boolean> {
        if (!FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM_M2.isEnabled()) return flowOf(false)
        return selectedSite.observe()
            .flatMapLatest { site ->
                if (site == null || site.connectionType == SiteConnectionType.Jetpack) {
                    flowOf(false)
                } else {
                    pushNotificationRegistrationStatus.observe(site.siteId).map { registrationStatus ->
                        registrationStatus != Status.REGISTERED_WOO_ONLY && registrationStatus != Status.REGISTERED_BOTH
                    }
                }
            }
    }
}
