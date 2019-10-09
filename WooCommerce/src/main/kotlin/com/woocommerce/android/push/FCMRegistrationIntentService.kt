package com.woocommerce.android.push

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.preference.PreferenceManager
import com.google.firebase.iid.FirebaseInstanceId
import com.woocommerce.android.JobServiceIds.JOB_FCM_REGISTRATION_SERVICE_ID
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import dagger.android.AndroidInjection
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.NotificationAppKey.WOOCOMMERCE
import org.wordpress.android.fluxc.store.NotificationStore.RegisterDevicePayload
import javax.inject.Inject

class FCMRegistrationIntentService : JobIntentService() {
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var accountStore: AccountStore
    @Inject internal lateinit var notificationStore: NotificationStore

    @Inject internal lateinit var selectedSite: SelectedSite

    private val sharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(this) }

    companion object {
        const val WPCOM_PUSH_DEVICE_TOKEN = "WC_PREF_NOTIFICATIONS_TOKEN"

        fun enqueueWork(context: Context) {
            val work = Intent(context, FCMRegistrationIntentService::class.java)
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
                WooLog.w(T.NOTIFS, "Empty FCM token, can't register the id on remote services")
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

    private fun sendRegistrationToken(fcmToken: String) {
        if (accountStore.hasAccessToken() && selectedSite.exists()) {
            // Register for WordPress.com notifications
            WooLog.i(T.NOTIFS, "Sending FCM token to our remote services: $fcmToken")

            sharedPreferences.edit().putString(WPCOM_PUSH_DEVICE_TOKEN, fcmToken).apply()

            val payload = RegisterDevicePayload(fcmToken, WOOCOMMERCE, selectedSite.get())
            dispatcher.dispatch(NotificationActionBuilder.newRegisterDeviceAction(payload))
        }
    }
}
