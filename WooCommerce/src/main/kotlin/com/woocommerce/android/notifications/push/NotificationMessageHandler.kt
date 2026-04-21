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
import com.woocommerce.android.notifications.ActiveNotificationData
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.NotificationSource
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.WooNotificationType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.sitevisibility.GetWooVisibleSites
import com.woocommerce.android.util.NotificationsParser
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.NOTIFICATIONS
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.runBlocking
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationMessageHandler @Inject constructor(
    private val notificationBuilder: WooNotificationBuilder,
    private val analyticsTracker: NotificationAnalyticsTracker,
    private val notificationsParser: NotificationsParser,
    private val accountStore: AccountStore,
    private val registrationStatus: PushNotificationRegistrationStatus,
    private val wooLog: WooLog,
    private val dispatcher: Dispatcher,
    private val resourceProvider: ResourceProvider,
    private val getWooVisibleSites: GetWooVisibleSites,
    private val selectedSite: SelectedSite,
    private val workManagerScheduler: WorkManagerScheduler
) {
    companion object {
        private const val PUSH_NOTIFICATION_ID = 10000

        private const val PUSH_ARG_USER = "user"

        @VisibleForTesting
        const val MAX_INBOX_ITEMS = 5
    }

    @Synchronized
    fun onPushNotificationDismissed(notificationId: Int) {
        removeNotificationByNotificationIdFromSystemsBar(notificationId)
    }

    @Synchronized
    fun onLocalNotificationDismissed(notificationId: Int, notificationType: String) {
        removeNotificationByNotificationIdFromSystemsBar(notificationId)
        AnalyticsTracker.track(
            stat = LOCAL_NOTIFICATION_DISMISSED,
            properties = mapOf(AnalyticsTracker.KEY_TYPE to notificationType)
        )
    }

    @Suppress("ReturnCount", "ComplexMethod")
    fun onNewMessageReceived(messageData: Map<String, String>) {
        if (!selectedSite.exists()) {
            wooLog.e(NOTIFICATIONS, "User has no site selected!")
            return
        }

        if (messageData.isEmpty()) {
            wooLog.e(NOTIFICATIONS, "Push notification received without a valid Bundle!")
            return
        }

        val notificationModel = notificationsParser.buildNotificationModelFromPayloadMap(messageData)
        if (notificationModel == null) {
            wooLog.e(NOTIFICATIONS, "Notification data is empty!")
            return
        }

        val notification = notificationModel.toAppModel(resourceProvider)
        val notificationSource = messageData.detectNotificationSource(notification.remoteNoteId)
        val pushUserId = messageData[PUSH_ARG_USER]
        val hasWpComAccessToken = accountStore.hasAccessToken()

        if (hasWpComAccessToken && !isWooSiteVisible(notification.remoteSiteId)) {
            wooLog.w(NOTIFICATIONS, "Skipping notification, site ${notification.remoteSiteId} is not visible")
            return
        }

        if (notificationSource == NotificationSource.WPCOM) {
            if (!hasWpComAccessToken) {
                wooLog.e(NOTIFICATIONS, "User is not logged in!")
                return
            }

            if (notification.remoteNoteId == 0L) {
                wooLog.e(NOTIFICATIONS, "Push notification received without a valid note_id in the payload!")
                return
            }

            // At this point, pushUserId is always set server side, but better to double check it here.
            if (accountStore.account.userId.toString() != pushUserId) {
                wooLog.e(NOTIFICATIONS, "WP.com userId found in the app doesn't match with the ID in the PN. Aborting.")
                return
            }

            val registrationStatusResult = runBlocking { registrationStatus(notification.remoteSiteId) }
            if (registrationStatusResult.isWooRegistered) {
                wooLog.d(NOTIFICATIONS, "Skipping WPCOM notification, already registered with Woo Core")
                return
            }
        }

        dispatchBackgroundEvents(notificationModel)
        handleWooNotification(notification, notificationSource)
    }

    private fun isWooSiteVisible(siteId: Long): Boolean = runBlocking {
        getWooVisibleSites().any { it.siteId == siteId }
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

    private fun handleWooNotification(notification: Notification, source: NotificationSource) {
        val localPushId = getLocalPushIdForNoteId(notification.remoteNoteId)
        val analyticsId = notification.buildAnalyticsId(source)
        with(notificationBuilder) {
            if (isNotificationsEnabled()) {
                analyticsTracker.trackNotificationAnalytics(
                    stat = PUSH_NOTIFICATION_RECEIVED,
                    siteId = notification.remoteSiteId,
                    notificationId = analyticsId,
                    noteTypeTrackingValue = notification.noteType.trackingValue,
                    source = source
                )
                analyticsTracker.flush()
            }

            val activeNotifications = getActiveNotifications()
            val otherNotifications = activeNotifications.filter { it.id != localPushId }
            val isGroupNotification = otherNotifications.isNotEmpty()
            buildAndDisplayWooNotification(
                pushId = localPushId,
                notification = notification,
                source = source,
                analyticsId = analyticsId,
                isGroupNotification = isGroupNotification
            )

            if (isGroupNotification) {
                val existingMessages = otherNotifications
                    .take(MAX_INBOX_ITEMS - 1)
                    .mapNotNull { it.noteMessage }
                val message = (listOfNotNull(notification.noteMessage) + existingMessages).joinToString("\n")

                val totalCount = otherNotifications.size + 1
                val subject = resourceProvider.getString(R.string.new_notifications, totalCount)
                buildAndDisplayWooGroupNotification(
                    inboxMessage = message,
                    subject = subject,
                    notification = notification,
                    source = source,
                    analyticsId = analyticsId
                )
            }
        }

        EventBus.getDefault().post(NotificationReceivedEvent(notification.remoteSiteId, notification.channelType))
    }

    private fun getLocalPushIdForNoteId(noteId: Long): Int {
        with(notificationBuilder) {
            val activeNotifications = getActiveNotifications()

            // Return existing ID if this noteId already has an active notification
            // New order notifications have the same notification_note_id. So if there is a new incoming notification
            // when there is an existing new order notification in the notification tray,
            // the cha ching sound is not played and the new notification replaces the existing notification
            // See issue for more details: https://github.com/woocommerce/woocommerce-android/pull/2546
            // We always generate new id for NewOrder.
            activeNotifications
                .firstOrNull {
                    noteId != 0L && it.remoteNoteId == noteId &&
                        it.noteTypeTrackingValue != WooNotificationType.NewOrder.trackingValue
                }
                ?.let { return it.id }

            // Generate a unique ID by incrementing until we find one not in use
            val activeIds = activeNotifications.map { it.id }.toSet()
            return generateSequence(PUSH_NOTIFICATION_ID) { it + 1 }.first { it !in activeIds }
        }
    }

    /**
     * Find the matching notification and send a track event for [PUSH_NOTIFICATION_TAPPED].
     */
    fun markNotificationTapped(localPushId: Int) {
        with(notificationBuilder) {
            getActiveNotifications()
                .firstOrNull { it.id == localPushId }
                ?.let { it.trackTapped() }
        }
    }

    /**
     * Loop over all active notifications and send the [PUSH_NOTIFICATION_TAPPED] track event for each one.
     */
    fun markNotificationsOfTypeTapped(type: NotificationChannelType) {
        notificationBuilder.getActiveNotifications()
            .filter { it.channelType == type.name && !it.isGroupSummary }
            .forEach { it.trackTapped() }
    }

    fun removeAllNotificationsFromSystemsBar() {
        notificationBuilder.cancelAllNotifications()
    }

    @Synchronized
    fun removeNotificationByRemoteIdFromSystemsBar(remoteNoteId: Long) {
        with(notificationBuilder) {
            getActiveNotifications()
                .filter { it.remoteNoteId == remoteNoteId }
                .forEach { cancelNotification(it.id) }
        }
    }

    @Synchronized
    fun removeNotificationByNotificationIdFromSystemsBar(localPushId: Int) {
        notificationBuilder.cancelNotification(localPushId)
    }

    @Synchronized
    fun removeTappedNotificationAndSummaryIfNeeded(localPushId: Int, notification: Notification) {
        with(notificationBuilder) {
            cancelNotification(localPushId)

            val hasRemainingChildrenInGroup = getActiveNotifications().any {
                !it.isGroupSummary &&
                    it.id != localPushId &&
                    it.channelType == notification.channelType.name &&
                    it.remoteSiteId == notification.remoteSiteId
            }

            if (!hasRemainingChildrenInGroup) {
                cancelNotification(notification.getGroupPushId())
            }
        }
    }

    @Synchronized
    fun removeNotificationsOfTypeFromSystemsBar(type: NotificationChannelType, remoteSiteId: Long) {
        with(notificationBuilder) {
            getActiveNotifications()
                .filter { it.channelType == type.name && it.remoteSiteId == remoteSiteId }
                .forEach { cancelNotification(it.id) }
        }
    }

    private fun Map<String, String>.detectNotificationSource(remoteNoteId: Long): NotificationSource =
        when {
            this[PUSH_ARG_USER] != null || remoteNoteId != 0L -> NotificationSource.WPCOM
            else -> NotificationSource.WOO_DRIVEN
        }

    private fun ActiveNotificationData.trackTapped() {
        analyticsTracker.trackNotificationAnalytics(
            stat = PUSH_NOTIFICATION_TAPPED,
            siteId = remoteSiteId,
            notificationId = analyticsId,
            noteTypeTrackingValue = noteTypeTrackingValue.orEmpty(),
            source = source
        )
        analyticsTracker.flush()
    }

    private fun Notification.buildAnalyticsId(source: NotificationSource): String? = when (source) {
        NotificationSource.WPCOM -> remoteNoteId.takeIf { it != 0L }?.toString()
        NotificationSource.WOO_DRIVEN -> buildWooDrivenAnalyticsId()
    }

    /**
     * Builds the stable `<siteId>:<type>:<entity-id>` analytics id for a Woo-driven notification,
     * or `null` when the type has no segment (Blaze, local reminder) or the entity id is zero.
     */
    private fun Notification.buildWooDrivenAnalyticsId(): String? {
        val wooTypeSegment = when (noteType) {
            is WooNotificationType.NewOrder -> NotificationModel.Kind.STORE_ORDER.name
            is WooNotificationType.ProductReview -> NotificationModel.Kind.COMMENT.name
            else -> null
        }
        return when {
            wooTypeSegment == null || uniqueId == 0L -> null
            else -> "$remoteSiteId:$wooTypeSegment:$uniqueId"
        }
    }
}
