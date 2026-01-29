package com.woocommerce.android.notifications.push

import com.woocommerce.android.extensions.isNotNullOrEmpty
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class PushNotificationRegistrationStatus @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    private val pushNotificationRepository: PushNotificationRepository
) {
    suspend operator fun invoke(siteId: Long?): Status {
        val wpComPushServerId = prefsWrapper.getFluxCPreferences()
            .getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        val isWooRegistered = siteId != null && pushNotificationRepository.isWooPushTokenRegisteredForSite(siteId)
        val isWpComRegistered = wpComPushServerId.isNotNullOrEmpty()
        return when {
            isWooRegistered && isWpComRegistered -> Status.REGISTERED_BOTH
            isWooRegistered -> Status.REGISTERED_WOO_ONLY
            isWpComRegistered -> Status.REGISTERED_WPCOM_ONLY
            else -> Status.UNREGISTERED
        }
    }

    enum class Status {
        REGISTERED_WOO_ONLY,
        REGISTERED_WPCOM_ONLY,
        REGISTERED_BOTH, // Registered in both WP.com and Woo Core PN systems
        UNREGISTERED
    }
}
