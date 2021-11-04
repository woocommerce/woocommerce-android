package com.woocommerce.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class FCMMessageService : FirebaseMessagingService() {
    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler
    @Inject lateinit var notificationRegistrationHandler: NotificationRegistrationHandler

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(newToken: String) {
        WooLog.d(T.NOTIFS, "Sending FCM token to our remote services: $newToken")
        notificationRegistrationHandler.onNewFCMTokenReceived(newToken)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        WooLog.v(T.NOTIFS, "Received message from Firebase")
        notificationMessageHandler.onNewMessageReceived(message.data, applicationContext)
    }
}
