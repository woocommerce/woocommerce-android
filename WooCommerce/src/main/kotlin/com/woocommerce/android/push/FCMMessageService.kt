package com.woocommerce.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FCMMessageService : FirebaseMessagingService() {
    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var registerDevice: RegisterDevice
    @AppCoroutineScope lateinit var appCoroutineScope: CoroutineScope

    override fun onNewToken(newToken: String) {
        appPrefsWrapper.setFCMToken(newToken)
        appCoroutineScope.launch {
            registerDevice()
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        WooLog.v(T.NOTIFS, "Received message from Firebase")
        notificationMessageHandler.onNewMessageReceived(message.data, applicationContext)
    }
}
