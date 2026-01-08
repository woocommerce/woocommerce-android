package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.dispatchAndAwait
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsStore
import org.wordpress.android.fluxc.store.NotificationStore.OnDeviceUnregistered
import java.util.UUID
import javax.inject.Inject

class PushNotificationRepository @Inject constructor(
    private val pushNotificationsStore: PushNotificationsStore,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatcher: Dispatcher
) {
    suspend fun registerPushToken(token: String) {
        WooLog.d(
            tag = WooLog.T.NOTIFS,
            message = "Registering FCM token in Woo Core instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        selectedSite.getIfExists()?.let {
            val uuid = appPrefsWrapper.wooCorePushDeviceUUID.orNullIfEmpty() ?: generateAndStoreUUID()
            val result = pushNotificationsStore.registerPushToken(
                site = it,
                token = token,
                deviceUuid = uuid
            )
            if (!result.isError) {
                dispatcher.dispatchAndAwait<Void, OnDeviceUnregistered>(
                    NotificationActionBuilder.newUnregisterDeviceAction()
                )
            }
        } ?: run { WooLog.w(WooLog.T.NOTIFS, "No selected site, skipping PN registration") }
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            appPrefsWrapper.wooCorePushDeviceUUID = it
        }
    }
}
