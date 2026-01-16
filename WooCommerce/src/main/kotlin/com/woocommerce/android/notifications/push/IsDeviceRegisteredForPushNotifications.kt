package com.woocommerce.android.notifications.push

import com.woocommerce.android.util.FeatureFlag
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class IsDeviceRegisteredForPushNotifications @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper,
    private val pushNotificationsStore: PushNotificationsStore
) {
    operator fun invoke(): Status =
        if (FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM.isEnabled()) {
            when (pushNotificationsStore.hasPushToken()) {
                true -> Status.REGISTERED
                false -> Status.UNREGISTERED
            }
        } else {
            val deviceId = prefsWrapper.getFluxCPreferences()
                .getString(NotificationStore.Companion.WPCOM_PUSH_DEVICE_SERVER_ID, null)
            when (deviceId.isNullOrEmpty()) {
                true -> Status.UNREGISTERED
                false -> Status.REGISTERED
            }
        }

    enum class Status {
        REGISTERED, UNREGISTERED
    }
}
