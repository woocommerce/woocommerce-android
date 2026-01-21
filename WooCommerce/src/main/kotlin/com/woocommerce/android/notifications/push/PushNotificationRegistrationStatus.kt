package com.woocommerce.android.notifications.push

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class PushNotificationRegistrationStatus @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    private val pushNotificationRepository: PushNotificationRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(): Status =
        if (FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM.isEnabled()) {
            val siteId = selectedSite.getIfExists()?.siteId
            if (siteId != null && pushNotificationRepository.isWooPushTokenRegisteredForSite(siteId)) {
                Status.REGISTERED
            } else {
                Status.UNREGISTERED
            }
        } else {
            val deviceId = prefsWrapper.getFluxCPreferences()
                .getString(NotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
            when (deviceId.isNullOrEmpty()) {
                true -> Status.UNREGISTERED
                false -> Status.REGISTERED
            }
        }

    enum class Status {
        REGISTERED, UNREGISTERED
    }
}
