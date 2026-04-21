package com.woocommerce.android.notifications.push

import com.woocommerce.android.extensions.isNotNullOrEmpty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import org.wordpress.android.fluxc.store.WpComPushNotificationStore
import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class PushNotificationRegistrationStatus @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    private val pushNotificationRepository: PushNotificationRepository
) {
    suspend operator fun invoke(siteId: Long?): Status = observe(siteId).first()

    fun observe(siteId: Long?): Flow<Status> {
        val wpComPushServerIdFlow = flowOf(
            prefsWrapper.getFluxCPreferences().getString(WpComPushNotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        )

        val isWooRegisteredFlow = if (siteId != null) {
            pushNotificationRepository.observeWooPushTokenRegisteredForSite(siteId)
        } else {
            flowOf(false)
        }

        return combine(wpComPushServerIdFlow, isWooRegisteredFlow) { wpComPushServerId, isWooRegistered ->
            val isWpComRegistered = wpComPushServerId.isNotNullOrEmpty()
            when {
                isWooRegistered && isWpComRegistered -> Status.REGISTERED_BOTH
                isWooRegistered -> Status.REGISTERED_WOO_ONLY
                isWpComRegistered -> Status.REGISTERED_WPCOM_ONLY
                else -> Status.UNREGISTERED
            }
        }
    }

    enum class Status {
        REGISTERED_WOO_ONLY,
        REGISTERED_WPCOM_ONLY,
        REGISTERED_BOTH, // Registered in both WP.com and Woo Core PN systems
        UNREGISTERED;

        val isWooRegistered: Boolean
            get() = this == REGISTERED_WOO_ONLY || this == REGISTERED_BOTH
    }
}
