package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.FORCEFULLY
import com.woocommerce.android.notifications.push.RegisterDevice.Mode.IF_NEEDED
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class RegisterDevice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val pushNotificationRegistrationStatus: PushNotificationRegistrationStatus,
    private val pushNotificationRepository: PushNotificationRepository,
    private val featureFlagRepository: FeatureFlagRepository,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(mode: Mode) {
        val pushRegistrationStatus = pushNotificationRegistrationStatus(selectedSite.getIfExists()?.siteId)
        WooLog.d(WooLog.T.NOTIFICATIONS, "Current PN registration status: $pushRegistrationStatus")
        when (mode) {
            IF_NEEDED -> {
                when (pushRegistrationStatus) {
                    Status.UNREGISTERED -> sendToken()
                    Status.REGISTERED_WPCOM_ONLY -> {
                        if (featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1)) {
                            sendToken()
                        }
                    }

                    Status.REGISTERED_WOO_ONLY,
                    Status.REGISTERED_BOTH -> {
                    }
                }
            }

            FORCEFULLY -> sendToken()
        }
    }

    private suspend fun sendToken() {
        val token = appPrefsWrapper.getFCMToken()
        if (token.isNotEmpty()) {
            if (featureFlagRepository.isEnabled(FeatureFlag.WOO_SELF_DRIVEN_PUSH_NOTIFICATIONS_M1)) {
                selectedSite.getIfExists()?.let { site ->
                    pushNotificationRepository.registerPushTokenInWooCoreSystem(token, site)
                }
            } else if (accountStore.hasAccessToken()) {
                pushNotificationRepository.registerPushTokenInWpComSystem(token)
            }
            WooLog.d(
                WooLog.T.NOTIFICATIONS,
                "Updated push notification registration status: " +
                    "${pushNotificationRegistrationStatus(selectedSite.getIfExists()?.siteId)}"
            )
        }
    }

    enum class Mode {
        IF_NEEDED, FORCEFULLY
    }
}
