package com.woocommerce.android.push

import com.woocommerce.android.util.WooLog
import javax.inject.Inject
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore

class NotificationRepository @Inject constructor(
    private val accountStore: AccountStore,
    private val notificationStore: NotificationStore,
) {
    suspend fun registerDevice(token: String) {
        if (accountStore.hasAccessToken()) {
            WooLog.d(WooLog.T.NOTIFS, "Sending FCM token to our remote services: $token")
            notificationStore.registerDevice(token, NotificationStore.NotificationAppKey.WOOCOMMERCE)
        }
    }
}
