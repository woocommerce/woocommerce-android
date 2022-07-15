package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.R
import com.woocommerce.android.model.Notification
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.WooNotificationBuilder
import com.woocommerce.android.push.WooNotificationType
import com.woocommerce.android.ui.login.localnotifications.LoginFlowUsageTracker.Companion.LOGIN_NOTIFICATION_TYPE_KEY
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class LocalNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooNotificationBuilder: WooNotificationBuilder
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val notificationType = inputData.getString(LOGIN_NOTIFICATION_TYPE_KEY)
        // Do the work here--in this case, upload the images.
        Log.i("LoginNotifications", "NOTIFICATION SENT: $notificationType")

        wooNotificationBuilder.buildAndDisplayWooNotification(
            0,
            0,
            appContext.getString(R.string.notification_channel_general_id),
            notification = Notification(
                noteId = 1,
                uniqueId = 1L,
                remoteNoteId = 1L,
                remoteSiteId = 1L,
                icon = "https://s.wp.com/wp-content/mu-plugins/notes/images/update-payment-2x.png",
                noteTitle = "Test NOTE TITLE",
                noteMessage = "Test NOTE MESSAGE",
                noteType = WooNotificationType.NEW_ORDER,
                channelType = NotificationChannelType.NEW_ORDER
            ),
            addCustomNotificationSound = false,
            isGroupNotification = false
        )

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}
