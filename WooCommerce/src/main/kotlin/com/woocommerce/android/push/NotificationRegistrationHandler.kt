package com.woocommerce.android.push

import com.woocommerce.android.util.PreferencesWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDevicePayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRegistrationHandler @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    private val notificationStore: NotificationStore, // Required to ensure the notificationStore is initialized
    private val preferencesWrapper: PreferencesWrapper
) {
    fun onNewFCMTokenReceived(token: String) {
        // Register for WordPress.com notifications only if user is logged in
        if (accountStore.hasAccessToken()) {
            preferencesWrapper.setFCMToken(token)

            // The site is set to null to ensure we get notifications for all stores the user is logged in
            val payload = RegisterDevicePayload(
                gcmToken = token,
                appKey = NotificationStore.NotificationAppKey.WOOCOMMERCE,
                site = null
            )
            dispatcher.dispatch(NotificationActionBuilder.newRegisterDeviceAction(payload))
        }
    }
}
