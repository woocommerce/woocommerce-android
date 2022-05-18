package com.woocommerce.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FCMMessageService : FirebaseMessagingService() {
    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var registerDevice: RegisterDevice

    override fun onNewToken(newToken: String) {
        appPrefsWrapper.setFCMToken(newToken)
        CoroutineScope(Dispatchers.IO).launch {
            registerDevice()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        WooLog.v(T.NOTIFS, "Received message from Firebase")
        notificationMessageHandler.onNewMessageReceived(message.data, applicationContext)
    }
}
