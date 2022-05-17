package com.woocommerce.android.push

import com.woocommerce.android.AppPrefsWrapper
import javax.inject.Inject
import org.wordpress.android.fluxc.store.AccountStore

class RegisterDevice @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val accountStore: AccountStore,
    private val notificationRepository: NotificationRepository,
) {
    suspend operator fun invoke() {
        val token = appPrefsWrapper.getFCMToken()
        if (accountStore.hasAccessToken() && token.isNotEmpty()) {
            notificationRepository.registerDevice(token)
        }
    }
}
