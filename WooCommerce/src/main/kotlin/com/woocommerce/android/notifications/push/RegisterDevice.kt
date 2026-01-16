package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.notifications.push.IsDeviceRegisteredForPushNotifications.Status
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.FORCEFULLY
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.IF_NEEDED
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class RegisterDevice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val notificationRepository: NotificationRepository,
    private val isDeviceRegisteredForPushNotifications: IsDeviceRegisteredForPushNotifications,
    private val pushNotificationRepository: PushNotificationRepository
) {
    suspend operator fun invoke(mode: Mode) {
        val pushRegistrationStatus = isDeviceRegisteredForPushNotifications()
        when (mode) {
            IF_NEEDED -> {
                if (pushRegistrationStatus == Status.UNREGISTERED) {
                    sendToken()
                }
            }

            FORCEFULLY -> sendToken()
        }

        WooLog.d(WooLog.T.NOTIFICATIONS, "Push notifications registration status: $pushRegistrationStatus")
        if (BuildConfig.DEBUG) {
            WooLog.d(WooLog.T.UTILS, "Current FCM token: ${appPrefsWrapper.getFCMToken()}")
        }
    }

    private suspend fun sendToken() {
        val token = appPrefsWrapper.getFCMToken()
        if (token.isNotEmpty()) {
            if (FeatureFlag.WOO_PUSH_NOTIFICATIONS_SYSTEM.isEnabled()) {
                pushNotificationRepository.registerPushToken(token)
            } else if (accountStore.hasAccessToken()) {
                notificationRepository.registerDevice(token)
            }
        }
    }

    enum class Mode {
        IF_NEEDED, FORCEFULLY
    }
}
