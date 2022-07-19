package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.model.Notification
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.WooNotificationBuilder
import com.woocommerce.android.push.WooNotificationType
import com.woocommerce.android.ui.login.localnotifications.LoginFlowUsageTracker.Companion.LOGIN_NOTIFICATION_TYPE_KEY
import com.woocommerce.android.ui.login.localnotifications.LoginFlowUsageTracker.LoginSupportNotificationType
import com.woocommerce.android.ui.login.localnotifications.LoginFlowUsageTracker.LoginSupportNotificationType.DEFAULT_SUPPORT
import com.woocommerce.android.ui.login.localnotifications.LoginFlowUsageTracker.LoginSupportNotificationType.LOGIN_ERROR_WRONG_EMAIL
import com.woocommerce.android.ui.login.localnotifications.LoginFlowUsageTracker.LoginSupportNotificationType.NO_LOGIN_INTERACTION
import com.woocommerce.android.ui.login.localnotifications.LoginFlowUsageTracker.LoginSupportNotificationType.valueOf
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocalNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooNotificationBuilder: WooNotificationBuilder,
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        when (getNotificationType()) {
            NO_LOGIN_INTERACTION -> noInteractionNotification()
            LOGIN_ERROR_WRONG_EMAIL -> wrongEmailNotification()
            DEFAULT_SUPPORT -> defaultLoginSupportNotification()
        }
        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

    private fun defaultLoginSupportNotification() {
        wooNotificationBuilder.buildAndDisplayWooNotification(
            0,
            0,
            appContext.getString(R.string.notification_channel_pre_login_id),
            notification = Notification(
                noteId = 1,
                uniqueId = 1L,
                remoteNoteId = 1L,
                remoteSiteId = 1L,
                icon = "https://s.wp.com/wp-content/mu-plugins/notes/images/update-payment-2x.png",
                noteTitle = "Trouble login into WooCommerce?",
                noteMessage = "If you are having issues login into your store from the app, " +
                    "please reach support so we can help you. ",
                noteType = WooNotificationType.PRE_LOGIN,
                channelType = NotificationChannelType.PRE_LOGIN
            ),
            addCustomNotificationSound = false,
            isGroupNotification = false
        )
    }

    private fun wrongEmailNotification() {
        TODO("Not yet implemented")
    }

    private fun noInteractionNotification() {
        TODO("Not yet implemented")
    }

    private fun getNotificationType(): LoginSupportNotificationType = runCatching {
        valueOf(inputData.getString(LOGIN_NOTIFICATION_TYPE_KEY).orEmpty())
    }.getOrDefault(DEFAULT_SUPPORT)
}
