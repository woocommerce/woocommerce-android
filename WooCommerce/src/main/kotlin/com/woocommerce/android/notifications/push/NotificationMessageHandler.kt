package com.woocommerce.android.notifications.push

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.LOCAL_NOTIFICATION_DISMISSED
import com.woocommerce.android.analytics.AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED
import com.woocommerce.android.analytics.AnalyticsEvent.PUSH_NOTIFICATION_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.background.WorkManagerScheduler
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.model.Notification
import com.woocommerce.android.model.isOrderNotification
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.WooNotificationType.NEW_ORDER
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.NotificationsParser
import com.woocommerce.android.util.WooLog.T.NOTIFS
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationPayload
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class NotificationMessageHandler @Inject constructor(
    private val notificationBuilder: WooNotificationBuilder,
    private val analyticsTracker: NotificationAnalyticsTracker,
    private val notificationsParser: NotificationsParser,
    private val accountStore: AccountStore,
    private val wooLogWrapper: WooLogWrapper,
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    private val selectedSite: SelectedSite,
    private val workManagerScheduler: WorkManagerScheduler
) {
    companion object {
        private const val PUSH_NOTIFICATION_ID = 10000

        private const val PUSH_ARG_USER = "user"

        @VisibleForTesting
        const val MAX_INBOX_ITEMS = 5

        private val ACTIVE_NOTIFICATIONS_MAP = mutableMapOf<Int, Notification>()
    }

    fun onPushNotificationDismissed(notificationId: Int) {
        removeNotificationByNotificationIdFromSystemsBar(notificationId)
    }

    fun onLocalNotificationDismissed(notificationId: Int, notificationType: String) {
        removeNotificationByNotificationIdFromSystemsBar(notificationId)
        AnalyticsTracker.track(
            stat = LOCAL_NOTIFICATION_DISMISSED,
            properties = mapOf(AnalyticsTracker.KEY_TYPE to notificationType)
        )
    }

    @Suppress("ReturnCount", "ComplexMethod")
    fun onNewMessageReceived(messageData: Map<String, String>) {
        if (!accountStore.hasAccessToken()) {
            wooLogWrapper.e(NOTIFS, "User is not logged in!")
            return
        }

        if (!selectedSite.exists()) {
            wooLogWrapper.e(NOTIFS, "User has no site selected!")
            return
        }

        if (messageData.isEmpty()) {
            wooLogWrapper.e(NOTIFS, "Push notification received without a valid Bundle!")
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
            notificationModel.meta?.ids?.let { ids ->
                val siteId = ids.site
                val orderId = ids.order
                if (siteId != null && orderId != null) {
                    workManagerScheduler.scheduleOrderUpdate(siteId, orderId)
                }
            }
        }
    }

    private fun handleWooNotification(notification: Notification) {
        val randomNumber = if (notification.noteType == NEW_ORDER) Random.nextInt() else 0
        val localPushId = getLocalPushIdForNoteId(notification.remoteNoteId, randomNumber)
        ACTIVE_NOTIFICATIONS_MAP[getLocalPushId(localPushId, randomNumber)] = notification
        if (notificationBuilder.isNotificationsEnabled()) {
            analyticsTracker.trackNotificationAnalytics(PUSH_NOTIFICATION_RECEIVED, notification)
            analyticsTracker.flush()
        }

        val isGroupNotification = ACTIVE_NOTIFICATIONS_MAP.size > 1
        with(notificationBuilder) {
            buildAndDisplayWooNotification(
                pushId = localPushId,
                notification = notification,
                isGroupNotification = isGroupNotification
            )

            if (isGroupNotification) {
                val notesMap = ACTIVE_NOTIFICATIONS_MAP.toMap()
                val message = notesMap.values.take(MAX_INBOX_ITEMS).joinToString("\n") {
                    it.noteMessage.orEmpty()
                }

                val subject = resourceProvider.getString(R.string.new_notifications, notesMap.size)
                val showGroupSummary = notesMap.size > MAX_INBOX_ITEMS
                val summaryText = if (showGroupSummary) {
                    resourceProvider.getString(
                        R.string.more_notifications,
                        notesMap.size - MAX_INBOX_ITEMS
                    )
                } else {
                    null
                }
                buildAndDisplayWooGroupNotification(
                    inboxMessage = message,
                    subject = subject,
                    summaryText = summaryText,
                    notification = notification
                )
            }
        }

        EventBus.getDefault().post(NotificationReceivedEvent(notification.remoteSiteId, notification.channelType))
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

    fun removeNotificationByNotificationIdFromSystemsBar(localPushId: Int) {
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
