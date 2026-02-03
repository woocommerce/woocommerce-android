package com.woocommerce.android.notifications.push

import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject

/**
 * Checks whether the "Enable Push Notifications" UI should be shown.
 *
 * This is part of the Woo Core push notifications system for app-password authenticated sites.
 */
class ShouldShowEnablePushNotificationsUi @Inject constructor(
    private val selectedSite: SelectedSite,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus
) {
    suspend operator fun invoke(): Boolean {
        if (!FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM_M2.isEnabled()) return false

        val site = selectedSite.getIfExists() ?: return false
        if (site.connectionType != SiteConnectionType.ApplicationPasswords) return false

        val registrationStatus = pushNotificationRegistrationStatus(site.siteId)
        return registrationStatus != Status.WOO_REGISTERED &&
            registrationStatus != Status.REGISTERED_IN_BOTH
    }
}
