package com.woocommerce.android.notifications.push

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class PushNotificationRegistrationStatus @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    private val pushNotificationRepository: PushNotificationRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(): Status {
        val wpComPushServerId = prefsWrapper.getFluxCPreferences()
            .getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        val siteId = selectedSite.getIfExists()?.siteId
        val isWooRegistered = siteId != null && pushNotificationRepository.isWooPushTokenRegisteredForSite(siteId)
        val isWpComRegistered = wpComPushServerId.isNotNullOrEmpty()
        return when {
            isWooRegistered && isWpComRegistered -> Status.REGISTERED_IN_BOTH
            isWooRegistered -> Status.WOO_REGISTERED
            isWpComRegistered -> Status.WPCOM_REGISTERED
            else -> Status.UNREGISTERED
        }
    }

    enum class Status {
        WOO_REGISTERED,
        WPCOM_REGISTERED,
        REGISTERED_IN_BOTH, // Registered in both WP.com and Woo Core PN systems
        UNREGISTERED
    }
}
