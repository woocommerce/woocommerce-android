package com.woocommerce.android.notifications.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.woocommerce.android.notifications.push.RegisterDevice.Trigger.TOKEN_REFRESH
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.store.InvalidateDeviceRegistration
import javax.inject.Inject

@AndroidEntryPoint
class FCMMessageService : FirebaseMessagingService() {
    private val job = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + job)

    @Inject lateinit var notificationMessageHandler: NotificationMessageHandler

    @Inject lateinit var identityStore: WooPushIdentityStore

    @Inject lateinit var registerDevice: RegisterDevice

    @Inject lateinit var invalidateDeviceRegistration: InvalidateDeviceRegistration

    override fun onNewToken(newToken: String) {
        // Firebase invokes this callback on a worker thread; persist the token before returning.
        runBlocking {
            identityStore.onNewFcmToken(newToken)
        }
        invalidateDeviceRegistration()
        serviceScope.launch {
            registerDevice(TOKEN_REFRESH)
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        WooLog.v(T.NOTIFICATIONS, "Received message from Firebase")
        notificationMessageHandler.onNewMessageReceived(message.data)
    }
}
