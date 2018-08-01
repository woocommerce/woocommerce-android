package com.woocommerce.android.push

import android.content.Context
import android.content.Intent
import android.os.Build
import android.preference.PreferenceManager
import android.support.v4.app.JobIntentService
import com.google.firebase.iid.FirebaseInstanceId
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.android.AndroidInjection
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDevicePayload
import org.wordpress.android.util.DeviceUtils
import org.wordpress.android.util.PackageUtils
import java.util.UUID
import javax.inject.Inject

class FCMRegistrationIntentService : JobIntentService() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var notificationStore: NotificationStore

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    companion object {
        private const val JOB_FCM_REGISTRATION_SERVICE_ID = 1000

        const val WPCOM_PUSH_DEVICE_UUID = "WC_PREF_NOTIFICATIONS_UUID"
        const val WPCOM_PUSH_DEVICE_TOKEN = "WC_PREF_NOTIFICATIONS_TOKEN"
        const val WPCOM_PUSH_DEVICE_SERVER_ID = "WC_PREF_NOTIFICATIONS_SERVER_ID"

        fun enqueueWork(context: Context, work: Intent) {
            JobIntentService.enqueueWork(context, FCMRegistrationIntentService::class.java,
                    JOB_FCM_REGISTRATION_SERVICE_ID, work)
        }
    }

    override fun onCreate() {
        AndroidInjection.inject(this)
        super.onCreate()
    }

    override fun onHandleWork(intent: Intent) {
        try {
            val token = FirebaseInstanceId.getInstance().token
            token?.takeIf { it.isNotEmpty() }?.let {
                sendRegistrationToken(it)
            } ?: run {
                WooLog.w(T.NOTIFS, "Empty GCM token, can't register the id on remote services")
                sharedPreferences.edit().remove(WPCOM_PUSH_DEVICE_TOKEN).apply()
            }
        } catch (e: Exception) {
            // SecurityException can happen on some devices without Google services (these devices probably strip
            // the AndroidManifest.xml and remove unsupported permissions).
            WooLog.e(T.NOTIFS, "Google Play Services unavailable: ", e)
        }
    }

    override fun onStopCurrentWork(): Boolean {
        // Ensure that the job is rescheduled if stopped
        return true
    }

    private fun sendRegistrationToken(gcmToken: String) {
        WooLog.i(T.NOTIFS, "Sending GCM token to our remote services: $gcmToken")
        // Register to WordPress.com notifications
        if (accountStore.hasAccessToken()) {
            // Get or create UUID for WP.com notes API
            val uuid = sharedPreferences.getString(WPCOM_PUSH_DEVICE_UUID, null) ?: generateAndStoreUUID()

            sharedPreferences.edit().putString(WPCOM_PUSH_DEVICE_TOKEN, gcmToken).apply()

            val payload = RegisterDevicePayload(createDeviceRegistrationStruct(uuid, gcmToken))
            dispatcher.dispatch(NotificationActionBuilder.newRegisterDeviceAction(payload))
        }
    }

    private fun generateAndStoreUUID(): String {
        return UUID.randomUUID().toString().also {
            sharedPreferences.edit().putString(WPCOM_PUSH_DEVICE_UUID, it).apply()
        }
    }

    private fun createDeviceRegistrationStruct(uuid: String, gcmToken: String): Map<String, String> {
        val deviceName = DeviceUtils.getInstance().getDeviceName(this)
        // TODO: Replace hardcoded values with RegisterDevicePayload properties
        return mapOf(
                "device_token" to gcmToken,
                "device_family" to "android",
                "device_name" to deviceName,
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "app_version" to PackageUtils.getVersionName(this),
                "version_code" to PackageUtils.getVersionCode(this).toString(),
                "os_version" to Build.VERSION.RELEASE,
                "device_uuid" to uuid
        )
    }
}
