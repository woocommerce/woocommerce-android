package com.woocommerce.android.push

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.android.AndroidInjection
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

class FCMMessageService : FirebaseMessagingService() {
    @Inject lateinit var accountStore: AccountStore

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onMessageReceived(message: RemoteMessage?) {
        WooLog.v(T.NOTIFS, "Received message from Firebase")

        if (!accountStore.hasAccessToken()) return

        message?.data?.let {
            // TODO Build and show notification
        } ?: WooLog.d(T.NOTIFS, "No notification message content received. Aborting.")
    }
}
