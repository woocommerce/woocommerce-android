package com.woocommerce.android.push

import android.content.Context
import android.os.Build
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED
import com.woocommerce.android.analytics.AnalyticsEvent.PUSH_NOTIFICATION_TAPPED
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.model.Notification
import com.woocommerce.android.model.isOrderNotification
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.util.NotificationsParser
import com.woocommerce.android.util.WooLog.T.NOTIFS
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.generated.WCOrderActionBuilder
import org.wordpress.android.fluxc.model.WCOrderListDescriptor
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class NotificationMessageHandler @Inject constructor(
    private val accountStore: AccountStore,
    private val wooLogWrapper: WooLogWrapper,
    private val dispatcher: Dispatcher,
    private val siteStore: SiteStore,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val resourceProvider: ResourceProvider,
    private val notificationBuilder: WooNotificationBuilder,
    private val analyticsTracker: NotificationAnalyticsTracker,
    private val zendeskHelper: ZendeskHelper,
    private val notificationsParser: NotificationsParser
) {
    companion object {
        private const val KEY_PUSH_TYPE_ZENDESK = "zendesk"
        private const val KEY_ZENDESK_REQUEST_ID = "zendesk_sdk_request_id"

        // All Zendesk push notifications will show the same notification, so hopefully this will be a unique ID
        private const val ZENDESK_PUSH_NOTIFICATION_ID = 1999999999

        private const val PUSH_NOTIFICATION_ID = 10000

        private const val PUSH_ARG_USER = "user"
        private const val MAX_INBOX_ITEMS = 5

        private val ACTIVE_NOTIFICATIONS_MAP = mutableMapOf<Int, Notification>()
    }

    @Synchronized
    fun onNotificationDismissed(localPushId: Int) {
        removeNotificationByPushIdFromSystemsBar(localPushId)
    }

    @Suppress("ReturnCount", "ComplexMethod")
    fun onNewMessageReceived(messageData: Map<String, String>, appContext: Context) {
        if (!accountStore.hasAccessToken()) {
            wooLogWrapper.e(NOTIFS, "User is not logged in!")
            return
        }

        if (messageData.isEmpty()) {
            wooLogWrapper.e(NOTIFS, "Push notification received without a valid Bundle!")
            return
        }

        if (messageData["type"] == KEY_PUSH_TYPE_ZENDESK) {
            // Make sure the UI gets refreshed so the user can see the reply
            zendeskHelper.refreshRequest(appContext, messageData[KEY_ZENDESK_REQUEST_ID])
            val zendeskNote = NotificationModel(
                noteId = ZENDESK_PUSH_NOTIFICATION_ID,
                remoteNoteId = ZENDESK_PUSH_NOTIFICATION_ID.toLong()
            ).toAppModel(resourceProvider)
            notificationBuilder.buildAndDisplayZendeskNotification(
                channelId = resourceProvider.getString(zendeskNote.channelType.getChannelId()),
                notification = zendeskNote
            )
            return
        }

        val pushUserId = messageData[PUSH_ARG_USER]
        // pushUserId is always set server side, but better to double check it here.
        if (accountStore.account.userId.toString() != pushUserId) {
            wooLogWrapper.e(NOTIFS, "WP.com userId found in the app doesn't match with the ID in the PN. Aborting.")
            return
        }

        val notificationModel = notificationsParser.buildNotificationModelFromPayloadMap(messageData)
        if (notificationModel == null) {
            wooLogWrapper.e(NOTIFS, "Notification data is empty!")
            return
        }

        val notification = notificationModel.toAppModel(resourceProvider)
        if (notification.remoteNoteId == 0L) {
            // At this point 'note_id' is always available in the notification bundle.
            wooLogWrapper.e(NOTIFS, "Push notification received without a valid note_id in the payload!")
            return
        }

        dispatchBackgroundEvents(notificationModel)
        handleWooNotification(notification)
    }

    private fun dispatchBackgroundEvents(notificationModel: NotificationModel) {
        // Save temporary notification to the database.
        dispatcher.dispatch(NotificationActionBuilder.newUpdateNotificationAction(notificationModel))

        // Fire off the event to fetch the actual notification from the api
        dispatcher.dispatch(
            NotificationActionBuilder.newFetchNotificationAction(
                FetchNotificationPayload(notificationModel.remoteNoteId)
            )
        )

        if (notificationModel.isOrderNotification()) {
            siteStore.getSiteBySiteId(notificationModel.remoteSiteId)?.let { site ->
                dispatcher.dispatch(
                    WCOrderActionBuilder.newFetchOrderListAction(
                        FetchOrderListPayload(offset = 0, listDescriptor = WCOrderListDescriptor(site = site))
                    )
                )

                dispatcher.dispatch(
                    WCOrderActionBuilder.newFetchOrderListAction(
                        FetchOrderListPayload(
                            offset = 0,
                            listDescriptor = WCOrderListDescriptor(
                                site = site,
                                statusFilter = CoreOrderStatus.PROCESSING.value
                            )
                        )
                    )
                )
            } ?: wooLogWrapper.e(NOTIFS, "Site not found - can't dispatchNewOrderEvents")
        }
    }

    /**
     * Don't display the notification if user chose to disable this type of notification -
     * note that we skip this for API 26+ since Oreo added per-app notification settings via channels
     */
    private fun isNotificationOptionEnabled(notification: Notification): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            when {
                notification.isOrderNotification -> appPrefsWrapper.isOrderNotificationsEnabled()
                notification.isReviewNotification -> appPrefsWrapper.isReviewNotificationsEnabled()
                else -> true
            }
        } else true
    }

    private fun handleWooNotification(notification: Notification) {
        if (!isNotificationOptionEnabled(notification)) {
            wooLogWrapper.i(NOTIFS, "Skipped ${notification.noteType.name} notification")
            return
        }

        val randomNumber = if (notification.noteType == WooNotificationType.NEW_ORDER) Random.nextInt() else 0
        val localPushId = getLocalPushIdForNoteId(notification.remoteNoteId, randomNumber)
        ACTIVE_NOTIFICATIONS_MAP[getLocalPushId(localPushId, randomNumber)] = notification
        if (notificationBuilder.isNotificationsEnabled()) {
            analyticsTracker.trackNotificationAnalytics(PUSH_NOTIFICATION_RECEIVED, notification)
            analyticsTracker.flush()
        }

        val channelType = notification.channelType
        val defaults = channelType.getDefaults(appPrefsWrapper)
        val channelId = resourceProvider.getString(channelType.getChannelId())
        val isGroupNotification = ACTIVE_NOTIFICATIONS_MAP.size > 1
        with(notificationBuilder) {
            buildAndDisplayWooNotification(
                localPushId, defaults, channelId, notification,
                appPrefsWrapper.isOrderNotificationsChaChingEnabled(), isGroupNotification
            )

            if (isGroupNotification) {
                val notesMap = ACTIVE_NOTIFICATIONS_MAP.toMap()
                val stringBuilder = StringBuilder()
                for (note in notesMap.values.take(MAX_INBOX_ITEMS)) {
                    stringBuilder.appendLine("${note.noteMessage}")
                }

                val subject = String.format(resourceProvider.getString(R.string.new_notifications), notesMap.size)
                val summaryText = String.format(
                    resourceProvider.getString(R.string.more_notifications),
                    notesMap.size - MAX_INBOX_ITEMS
                )
                buildAndDisplayWooGroupNotification(
                    channelId, stringBuilder.toString(), subject, summaryText, notification,
                    notesMap.size > MAX_INBOX_ITEMS
                )
            }
        }

        EventBus.getDefault().post(NotificationReceivedEvent(notification.channelType))
    }

    private fun getLocalPushId(wpComNoteId: Int, randomNumber: Int) = wpComNoteId + randomNumber

    // New order notifications have the same notification_note_id. So if there is a new incoming notification
    // when there is an existing new order notification in the notification tray,
    // the cha ching sound is not played and the new notification replaces the existing notification
    // See issue for more details: https://github.com/woocommerce/woocommerce-android/pull/2546
    // This solution is a temporary HACK to generate a random number for each new order notification
    // and use that number to group notifications along with notification_note_id
    private fun getLocalPushIdForNoteId(noteId: Long, randomNumber: Int): Int {
        for (id in ACTIVE_NOTIFICATIONS_MAP.keys) {
            val notification = ACTIVE_NOTIFICATIONS_MAP[getLocalPushId(id, randomNumber)]
            if (notification?.remoteNoteId == noteId) {
                return id
            }
        }
        return PUSH_NOTIFICATION_ID + ACTIVE_NOTIFICATIONS_MAP.size
    }

    private fun hasNotifications() = ACTIVE_NOTIFICATIONS_MAP.isNotEmpty()
    private fun clearNotifications() = ACTIVE_NOTIFICATIONS_MAP.clear()

    /**
     * Find the matching notification and send a track event for [PUSH_NOTIFICATION_TAPPED].
     */
    fun markNotificationTapped(remoteNoteId: Long) {
        ACTIVE_NOTIFICATIONS_MAP.asSequence()
            .firstOrNull { it.value.remoteNoteId == remoteNoteId }?.let { row ->
                analyticsTracker.trackNotificationAnalytics(PUSH_NOTIFICATION_TAPPED, row.value)
                analyticsTracker.flush()
            }
    }

    /**
     * Loop over all active notifications and send the [PUSH_NOTIFICATION_TAPPED] track event for each one.
     */
    fun markNotificationsOfTypeTapped(type: NotificationChannelType) {
        ACTIVE_NOTIFICATIONS_MAP.asSequence()
            .filter { it.value.channelType == type }
            .forEach { row ->
                analyticsTracker.trackNotificationAnalytics(PUSH_NOTIFICATION_TAPPED, row.value)
                analyticsTracker.flush()
            }
    }

    fun removeAllNotificationsFromSystemsBar() {
        clearNotifications()
        notificationBuilder.cancelAllNotifications()
    }

    @Synchronized
    fun removeNotificationByRemoteIdFromSystemsBar(remoteNoteId: Long) {
        val keptNotifs = HashMap<Int, Notification>()
        ACTIVE_NOTIFICATIONS_MAP.asSequence()
            .forEach { row ->
                if (row.value.remoteNoteId == remoteNoteId) {
                    notificationBuilder.cancelNotification(row.key)
                } else {
                    keptNotifs[row.key] = row.value
                }
            }

        clearNotifications()
        ACTIVE_NOTIFICATIONS_MAP.putAll(keptNotifs)
        updateNotificationsState()
    }

    @Synchronized
    fun removeNotificationByPushIdFromSystemsBar(localPushId: Int) {
        val keptNotifs = HashMap<Int, Notification>()
        ACTIVE_NOTIFICATIONS_MAP.asSequence()
            .forEach { row ->
                if (row.key == localPushId) {
                    notificationBuilder.cancelNotification(row.key)
                } else {
                    keptNotifs[row.key] = row.value
                }
            }

        clearNotifications()
        ACTIVE_NOTIFICATIONS_MAP.putAll(keptNotifs)
        updateNotificationsState()
    }

    @Synchronized
    fun removeNotificationsOfTypeFromSystemsBar(type: NotificationChannelType, remoteSiteId: Long) {
        val keptNotifs = HashMap<Int, Notification>()
        // Using a copy of the map to avoid concurrency problems
        ACTIVE_NOTIFICATIONS_MAP.toMap().asSequence().forEach { row ->
            if (row.value.channelType == type && row.value.remoteSiteId == remoteSiteId) {
                notificationBuilder.cancelNotification(row.key)
            } else {
                keptNotifs[row.key] = row.value
            }
        }
        clearNotifications()
        ACTIVE_NOTIFICATIONS_MAP.putAll(keptNotifs)
        updateNotificationsState()
    }

    private fun updateNotificationsState() {
        if (!hasNotifications()) {
            notificationBuilder.cancelAllNotifications()
        }
    }
}
