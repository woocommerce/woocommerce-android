package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.FORCEFULLY
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.IF_NEEDED
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class RegisterDevice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus,
    private val pushNotificationRepository: PushNotificationRepository
) {
    suspend operator fun invoke(mode: Mode) {
        val pushRegistrationStatus = pushNotificationRegistrationStatus()
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
                pushNotificationRepository.registerPushTokenInWooCoreSystem(token)
            } else if (accountStore.hasAccessToken()) {
                pushNotificationRepository.registerPushTokenInWpComSystem(token)
            }
        }
    }

    enum class Mode {
        IF_NEEDED, FORCEFULLY
    }
}
