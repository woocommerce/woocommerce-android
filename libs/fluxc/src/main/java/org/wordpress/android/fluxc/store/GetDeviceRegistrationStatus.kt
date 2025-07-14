package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.utils.PreferenceUtils
import javax.inject.Inject

class GetDeviceRegistrationStatus @Inject constructor(
    private val prefsWrapper: PreferenceUtils.PreferenceUtilsWrapper
) {
    operator fun invoke(): Status {
        val deviceId = prefsWrapper.getFluxCPreferences().getString(NotificationStore.WPCOM_PUSH_DEVICE_SERVER_ID, null)
        return if (deviceId.isNullOrEmpty()) {
            Status.UNREGISTERED
        } else {
            Status.REGISTERED
        }
    }

    enum class Status {
        REGISTERED, UNREGISTERED
    }
}
