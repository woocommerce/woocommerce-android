package com.woocommerce.android.notifications.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.extensions.orNullIfEmpty
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.network.rest.wpcom.wc.pushnotifications.PushNotificationsStore
import java.util.UUID
import javax.inject.Inject

class PushNotificationRepository @Inject constructor(
    private val pushNotificationsStore: PushNotificationsStore,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    suspend fun registerPushToken(token: String) {
        WooLog.d(
            tag = WooLog.T.NOTIFS,
            message = "Registering FCM token in Woo Core instance${if (BuildConfig.DEBUG) ": $token" else ""}"
        )
        selectedSite.getIfExists()?.let {
            val uuid = appPrefsWrapper.wooCorePushDeviceUUID.orNullIfEmpty() ?: generateAndStoreUUID()
            pushNotificationsStore.registerPushToken(
                site = it,
                token = token,
                deviceUuid = uuid // TODO review we need to add any extra info to the uuid
            )
            //TODO ensure any WP.com token already registered is removed
        }
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            appPrefsWrapper.wooCorePushDeviceUUID = it
        }
    }
}
