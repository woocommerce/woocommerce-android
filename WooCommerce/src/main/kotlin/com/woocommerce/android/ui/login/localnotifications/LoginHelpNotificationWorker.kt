package com.woocommerce.android.ui.login.localnotifications

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.hilt.work.HiltWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_LOCAL_NOTIFICATION_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Notification
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.WooNotificationBuilder
import com.woocommerce.android.push.WooNotificationType
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.localnotifications.LoginHelpNotificationType.DEFAULT_HELP
import com.woocommerce.android.ui.login.localnotifications.LoginHelpNotificationType.LOGIN_SITE_ADDRESS_EMAIL_ERROR
import com.woocommerce.android.ui.login.localnotifications.LoginHelpNotificationType.LOGIN_SITE_ADDRESS_ERROR
import com.woocommerce.android.ui.login.localnotifications.LoginHelpNotificationType.LOGIN_SITE_ADDRESS_PASSWORD_ERROR
import com.woocommerce.android.ui.login.localnotifications.LoginHelpNotificationType.LOGIN_WPCOM_EMAIL_ERROR
import com.woocommerce.android.ui.login.localnotifications.LoginHelpNotificationType.LOGIN_WPCOM_PASSWORD_ERROR
import com.woocommerce.android.ui.login.localnotifications.LoginNotificationScheduler.Companion.LOGIN_HELP_NOTIFICATION_ID
import com.woocommerce.android.ui.login.localnotifications.LoginNotificationScheduler.Companion.LOGIN_NOTIFICATION_TYPE_KEY
import com.woocommerce.android.viewmodel.ResourceProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
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
        when (notificationType) {
            LOGIN_SITE_ADDRESS_ERROR -> siteAddressErrorNotification(notificationType)
            LOGIN_SITE_ADDRESS_EMAIL_ERROR,
            LOGIN_WPCOM_EMAIL_ERROR -> invalidEmailErrorNotification(notificationType)
            LOGIN_SITE_ADDRESS_PASSWORD_ERROR,
            LOGIN_WPCOM_PASSWORD_ERROR -> invalidPasswordErrorNotification(notificationType)
            DEFAULT_HELP -> defaultLoginSupportNotification()
        }
        AnalyticsTracker.track(
            LOGIN_LOCAL_NOTIFICATION_DISPLAYED,
            mapOf(AnalyticsTracker.KEY_TYPE to notificationType.toString())
        )
        prefsWrapper.setPreLoginNotificationDisplayed(displayed = true)
        prefsWrapper.setPreLoginNotificationDisplayedType(notificationType.toString())
        return Result.success()
    }

    private fun defaultLoginSupportNotification(
        notificationType: LoginHelpNotificationType = DEFAULT_HELP,
        actions: List<Pair<String, Intent>> = emptyList()
    ) {
        wooNotificationBuilder.buildAndDisplayLoginHelpNotification(
            notificationLocalId = LOGIN_HELP_NOTIFICATION_ID,
            appContext.getString(R.string.notification_channel_pre_login_id),
            notification = buildLoginNotification(
                title = R.string.login_help_notification_default_title,
                description = R.string.login_help_notification_no_interaction_default_description
            ),
            notificationTappedIntent = buildOpenSupportScreenIntent(notificationType),
            actions = actions
        )
    }

    private fun siteAddressErrorNotification(notificationType: LoginHelpNotificationType) {
        wooNotificationBuilder.buildAndDisplayLoginHelpNotification(
            notificationLocalId = LOGIN_HELP_NOTIFICATION_ID,
            appContext.getString(R.string.notification_channel_pre_login_id),
            notification = buildLoginNotification(
                title = R.string.login_help_notification_default_title,
                description = R.string.login_help_notification_site_error_description
            ),
            notificationTappedIntent = buildOpenLoginFlowForNotificationType(notificationType),
            actions = getActionsForSiteAddressErrorNotification(notificationType)
        )
    }

    private fun invalidEmailErrorNotification(notificationType: LoginHelpNotificationType) {
        defaultLoginSupportNotification(
            notificationType, getActionsForInvalidEmailErrorNotification(notificationType)
        )
    }

    private fun invalidPasswordErrorNotification(notificationType: LoginHelpNotificationType) {
        wooNotificationBuilder.buildAndDisplayLoginHelpNotification(
            notificationLocalId = LOGIN_HELP_NOTIFICATION_ID,
            appContext.getString(R.string.notification_channel_pre_login_id),
            notification = buildLoginNotification(
                title = R.string.login_help_notification_invalid_password_title,
                description = R.string.login_help_notification_no_interaction_default_description
            ),
            notificationTappedIntent = buildOpenSupportScreenIntent(notificationType),
            actions = getActionsForInvalidPasswordErrorNotification(notificationType)
        )
    }

    private fun buildLoginNotification(
        @StringRes title: Int,
        @StringRes description: Int
    ) = Notification(
        noteId = LOGIN_HELP_NOTIFICATION_ID,
        uniqueId = 0,
        remoteNoteId = 0,
        remoteSiteId = 0,
        icon = null,
        noteTitle = resourceProvider.getString(title),
        noteMessage = resourceProvider.getString(description),
        noteType = WooNotificationType.PRE_LOGIN,
        channelType = NotificationChannelType.PRE_LOGIN
    )

    private fun buildOpenSupportScreenIntent(notificationType: LoginHelpNotificationType): Intent =
        HelpActivity.createIntent(
            appContext,
            HelpActivity.Origin.LOGIN_HELP_NOTIFICATION,
            arrayListOf(notificationType.toString())
        )

    private fun buildOpenLoginFlowForNotificationType(notificationType: LoginHelpNotificationType): Intent =
        LoginActivity.createIntent(appContext, notificationType)

    private fun getActionsForSiteAddressErrorNotification(
        notificationType: LoginHelpNotificationType
    ): List<Pair<String, Intent>> =
        listOf(
            resourceProvider.getString(R.string.login_help_notification_wordpress_login_button)
                to buildOpenLoginFlowForNotificationType(notificationType),
            resourceProvider.getString(R.string.login_help_notification_contact_support_button)
                to buildOpenSupportScreenIntent(notificationType),
        )

    private fun getActionsForInvalidEmailErrorNotification(
        notificationType: LoginHelpNotificationType
    ): List<Pair<String, Intent>> =
        listOf(
            resourceProvider.getString(R.string.login_help_notification_contact_support_button)
                to buildOpenSupportScreenIntent(notificationType),
        )

    private fun getActionsForInvalidPasswordErrorNotification(
        notificationType: LoginHelpNotificationType
    ): List<Pair<String, Intent>> {
        val actionsList = mutableListOf(
            resourceProvider.getString(R.string.login_help_notification_contact_support_button)
                to buildOpenSupportScreenIntent(notificationType)
        )
        if (prefsWrapper.getLoginEmail().isNotBlank()) {
            actionsList.add(
                index = 0,
                element = resourceProvider.getString(R.string.login_help_notification_get_link_by_email_button)
                    to buildOpenLoginFlowForNotificationType(notificationType),
            )
        }
        return actionsList
    }
}
