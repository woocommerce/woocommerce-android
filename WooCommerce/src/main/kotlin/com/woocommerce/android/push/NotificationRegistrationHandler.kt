package com.woocommerce.android.push

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.PreferencesWrapper
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDevicePayload
import javax.inject.Inject

class NotificationRegistrationHandler @Inject constructor(
    private val dispatcher: Dispatcher,
    private val accountStore: AccountStore,
    @Suppress("UnusedPrivateMember", "Required to ensure the notificationStore is initialized")
    private val notificationStore: NotificationStore,
    private val preferencesWrapper: PreferencesWrapper,
    private val selectedSite: SelectedSite
) {
    fun onNewFCMTokenReceived(token: String) {
        // Register for WordPress.com notifications only if user is logged in & only if atleast one site exists
        if (accountStore.hasAccessToken() && selectedSite.exists()) {
            preferencesWrapper.setFCMToken(token)

            val payload = RegisterDevicePayload(
                gcmToken = token,
                appKey = NotificationStore.NotificationAppKey.WOOCOMMERCE,
                site = null
            )
            dispatcher.dispatch(NotificationActionBuilder.newRegisterDeviceAction(payload))
        }
    }

    fun onEmptyFCMTokenReceived() {
        preferencesWrapper.removeFCMToken()
    }
}
