package com.woocommerce.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.push.RegisterDevice.Mode.FORCEFULLY
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.InvalidateDeviceRegistration
import javax.inject.Inject

@AndroidEntryPoint
class FCMMessageService : FirebaseMessagingService() {
    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler
    @Inject lateinit var appPrefsWrapper: AppPrefsWrapper
    @Inject lateinit var registerDevice: RegisterDevice
    @Inject lateinit var invalidateDeviceRegistration: InvalidateDeviceRegistration

    private val job = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(newToken: String) {
        appPrefsWrapper.setFCMToken(newToken)
        serviceScope.launch {
            invalidateDeviceRegistration()
            registerDevice(FORCEFULLY)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        WooLog.v(T.NOTIFS, "Received message from Firebase")
        notificationMessageHandler.onNewMessageReceived(message.data, applicationContext)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
