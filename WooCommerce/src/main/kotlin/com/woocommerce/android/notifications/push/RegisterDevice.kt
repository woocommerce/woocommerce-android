package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.FORCEFULLY
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.IF_NEEDED
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.GetDeviceRegistrationStatus
import javax.inject.Inject

class RegisterDevice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val notificationRepository: NotificationRepository,
    private val getDeviceRegistrationStatus: GetDeviceRegistrationStatus,
) {
    suspend operator fun invoke(mode: Mode) {
        when (mode) {
            IF_NEEDED -> {
                if (getDeviceRegistrationStatus() == GetDeviceRegistrationStatus.Status.UNREGISTERED) {
                    sendToken()
                }
            }
            FORCEFULLY -> sendToken()
        }

        WooLog.d(WooLog.T.NOTIFICATIONS, "Push notifications registration status: ${getDeviceRegistrationStatus()}")
        if (BuildConfig.DEBUG) {
            WooLog.d(WooLog.T.UTILS, "Current FCM token: ${appPrefsWrapper.getFCMToken()}")
        }
    }

    private suspend fun sendToken() {
        val token = appPrefsWrapper.getFCMToken()
        if (accountStore.hasAccessToken() && token.isNotEmpty()) {
            notificationRepository.registerDevice(token)
        }
    }

    enum class Mode {
        IF_NEEDED, FORCEFULLY
    }
}
