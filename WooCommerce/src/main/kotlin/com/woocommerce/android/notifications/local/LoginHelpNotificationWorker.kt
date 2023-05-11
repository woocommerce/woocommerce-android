package com.woocommerce.android.notifications.local

import android.content.Context
import android.content.Intent
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_LOCAL_NOTIFICATION_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.support.help.HelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.notifications.local.LoginNotificationScheduler.Companion.LOGIN_NOTIFICATION_TYPE_KEY
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
@Suppress("UnusedPrivateMember")
class LoginHelpNotificationWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val wooNotificationBuilder: WooNotificationBuilder,
    private val resourceProvider: ResourceProvider,
    private val prefsWrapper: AppPrefsWrapper
) : Worker(appContext, workerParams) {
    override fun doWork(): Result {
        val notificationType = LoginHelpNotificationType.fromString(
            inputData.getString(LOGIN_NOTIFICATION_TYPE_KEY)
        )
        AnalyticsTracker.track(
            LOGIN_LOCAL_NOTIFICATION_DISPLAYED,
            mapOf(AnalyticsTracker.KEY_TYPE to notificationType.toString())
        )
        prefsWrapper.setPreLoginNotificationDisplayed(displayed = true)
        prefsWrapper.setPreLoginNotificationDisplayedType(notificationType.toString())
        return Result.success()
    }

//    private fun defaultLoginSupportNotification(
//        notificationType: LoginHelpNotificationType = DEFAULT_HELP,
//        actions: List<Pair<String, Intent>> = emptyList()
//    ) {
//        wooNotificationBuilder.buildAndDisplayLoginHelpNotification(
//            notificationLocalId = LOGIN_HELP_NOTIFICATION_ID,
//            appContext.getString(R.string.notification_channel_pre_login_id),
//            notification = buildLoginNotification(
//                title = R.string.login_help_notification_default_title,
//                description = R.string.login_help_notification_no_interaction_default_description
//            ),
//            notificationTappedIntent = buildOpenSupportScreenIntent(notificationType),
//            actions = actions
//        )
//    }
//
//    private fun buildLoginNotification(
//        @StringRes title: Int,
//        @StringRes description: Int
//    ) = Notification(
//        noteId = LOGIN_HELP_NOTIFICATION_ID,
//        uniqueId = 0,
//        remoteNoteId = 0,
//        remoteSiteId = 0,
//        icon = null,
//        noteTitle = resourceProvider.getString(title),
//        noteMessage = resourceProvider.getString(description),
//        noteType = WooNotificationType.PRE_LOGIN,
//        channelType = NotificationChannelType.PRE_LOGIN
//    )

    private fun buildOpenSupportScreenIntent(notificationType: LoginHelpNotificationType): Intent =
        HelpActivity.createIntent(
            appContext,
            HelpOrigin.LOGIN_HELP_NOTIFICATION,
            arrayListOf(notificationType.toString())
        )
}
