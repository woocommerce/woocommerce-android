package com.woocommerce.android.push

import com.woocommerce.android.AppPrefsWrapper
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
            Mode.IF_NEEDED -> {
                if (getDeviceRegistrationStatus() == GetDeviceRegistrationStatus.Status.UNREGISTERED) {
                    sendToken()
                }
            }
            Mode.FORCEFULLY -> sendToken()
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
