package com.woocommerce.android.notifications.push

import com.woocommerce.android.BuildConfig
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.store.NotificationStore
import javax.inject.Inject

class NotificationRepository @Inject constructor(
    private val notificationStore: NotificationStore,
) {
    suspend fun registerDevice(token: String) {
        WooLog.d(
            tag = WooLog.T.NOTIFS,
            message = "Sending FCM token to our remote services${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        notificationStore.registerDevice(token, NotificationStore.NotificationAppKey.WOOCOMMERCE)
    }
}
