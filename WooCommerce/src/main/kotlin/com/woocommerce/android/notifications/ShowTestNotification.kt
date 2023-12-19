package com.woocommerce.android.notifications

import android.Manifest.permission
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.woocommerce.android.R
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.random.Random
import kotlin.time.Duration

class ShowTestNotification @Inject constructor(
    private val context: Context,
    private val notificationChannelsHandler: NotificationChannelsHandler
) {
    suspend operator fun invoke(
        title: String,
        message: String,
        channelType: NotificationChannelType,
        dismissDelay: Duration? = null
    ) {
        if (context.checkSelfPermission(permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        val notificationId = Random.nextInt()

        val notification = NotificationCompat.Builder(
            context,
            with(notificationChannelsHandler) { channelType.getChannelId() }
        )
            .setSmallIcon(R.drawable.ic_woo_w_notification)
            .setContentTitle(title)
            .setContentText(message)
            .build()

        notificationManager.notify(notificationId, notification)

        dismissDelay?.let {
            withContext(NonCancellable) {
                kotlinx.coroutines.delay(it)
                notificationManager.cancel(notificationId)
            }
        }
    }
}
