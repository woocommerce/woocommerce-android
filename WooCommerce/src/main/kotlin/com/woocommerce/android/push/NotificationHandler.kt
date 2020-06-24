package com.woocommerce.android.push

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.ContentResolver.SCHEME_ANDROID_RESOURCE
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.bumptech.glide.Glide
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.push.NotificationHandler.NotificationChannelType.NEW_ORDER
import com.woocommerce.android.push.NotificationHandler.NotificationChannelType.OTHER
import com.woocommerce.android.push.NotificationHandler.NotificationChannelType.REVIEW
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.NotificationsUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.apache.commons.text.StringEscapeUtils
import org.greenrobot.eventbus.EventBus
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.NotificationActionBuilder
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.store.NotificationStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.util.ImageUtils
import org.wordpress.android.util.PhotonUtils
import org.wordpress.android.util.StringUtils
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.util.concurrent.ExecutionException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class NotificationHandler @Inject constructor(
    private val notificationStore: NotificationStore, // Required to ensure instantiated when app started from a push
    private val siteStore: SiteStore,
    private val dispatcher: Dispatcher
) {
    companion object {
        private val ACTIVE_NOTIFICATIONS_MAP = mutableMapOf<Int, Bundle>()

        private const val NOTIFICATION_GROUP_KEY = "notification_group_key"
        private const val PUSH_NOTIFICATION_ID = 10000

        // All Zendesk push notifications will show the same notification, so hopefully this will be a unique ID
        private const val ZENDESK_PUSH_NOTIFICATION_ID = 1999999999

        const val GROUP_NOTIFICATION_ID = 30000
        private const val MAX_INBOX_ITEMS = 5

        const val PUSH_ARG_USER = "user"
        const val PUSH_ARG_TYPE = "type"
        const val PUSH_ARG_TITLE = "title"
        const val PUSH_ARG_MSG = "msg"
        const val PUSH_ARG_NOTE_ID = "note_id"
        const val PUSH_ARG_NOTE_FULL_DATA = "note_full_data"

        const val PUSH_TYPE_COMMENT = "c"
        const val PUSH_TYPE_NEW_ORDER = "store_order"

        @Synchronized fun hasNotifications() = ACTIVE_NOTIFICATIONS_MAP.isNotEmpty()

        @Synchronized fun clearNotifications() {
            ACTIVE_NOTIFICATIONS_MAP.clear()
        }

        @Synchronized fun removeNotification(localPushId: Int) {
            ACTIVE_NOTIFICATIONS_MAP.remove(localPushId)
        }

        /**
         * Find the matching notification and send a track event for [Stat.PUSH_NOTIFICATION_TAPPED].
         */
        @Synchronized fun bumpPushNotificationsTappedAnalytics(context: Context, noteID: String) {
            ACTIVE_NOTIFICATIONS_MAP.asSequence()
                    .firstOrNull { it.value.getString(PUSH_ARG_NOTE_ID, "") == noteID }?.let { row ->
                        bumpPushNotificationsAnalytics(context, Stat.PUSH_NOTIFICATION_TAPPED, row.value)
                        AnalyticsTracker.flush() }
        }

        /**
         * Loop over all active notifications and send the [Stat.PUSH_NOTIFICATION_TAPPED] track event for each one.
         */
        @Synchronized fun bumpPushNotificationsTappedAllAnalytics(context: Context) {
            ACTIVE_NOTIFICATIONS_MAP.asIterable().forEach {
                val noteBundle = it.value
                bumpPushNotificationsAnalytics(context, Stat.PUSH_NOTIFICATION_TAPPED, noteBundle)
            }
            AnalyticsTracker.flush()
        }

        /**
         * Removes all the notifications from the system bar and clears the active map.
         */
        @Synchronized fun removeAllNotificationsFromSystemBar(context: Context) {
            ACTIVE_NOTIFICATIONS_MAP.clear()

            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.cancelAll()

            setHasUnseenReviewNotifs(false)
        }

        /**
         * Removes only a specific type of notification from the system bar
         */
        @SuppressLint("UseSparseArrays")
        @Synchronized private fun removeAllNotifsOfTypeFromSystemBar(context: Context, type: String) {
            val notificationManager = NotificationManagerCompat.from(context)

            val keptNotifs = HashMap<Int, Bundle>()
            ACTIVE_NOTIFICATIONS_MAP.asSequence().forEach { entry ->
                if (entry.value.getString(PUSH_ARG_TYPE) == type) {
                    notificationManager.cancel(entry.key)
                } else {
                    keptNotifs[entry.key] = entry.value
                }
            }
            ACTIVE_NOTIFICATIONS_MAP.clear()
            ACTIVE_NOTIFICATIONS_MAP.putAll(keptNotifs)

            if (!hasNotifications()) {
                notificationManager.cancel(GROUP_NOTIFICATION_ID)
            }

            if (type == PUSH_TYPE_COMMENT) {
                setHasUnseenReviewNotifs(false)
            }
        }

        fun removeAllReviewNotifsFromSystemBar(context: Context) {
            removeAllNotifsOfTypeFromSystemBar(context, PUSH_TYPE_COMMENT)
        }

        fun removeAllOrderNotifsFromSystemBar(context: Context) {
            removeAllNotifsOfTypeFromSystemBar(context, PUSH_TYPE_NEW_ORDER)
        }

        /**
         * Removes a specific notification from the system bar.
         */
        @Synchronized fun removeNotificationWithNoteIdFromSystemBar(context: Context, wpComNoteId: String) {
            if (wpComNoteId.isEmpty() or !hasNotifications()) {
                return
            }

            val notificationManager = NotificationManagerCompat.from(context)

            ACTIVE_NOTIFICATIONS_MAP.asSequence().firstOrNull {
                it.value.getString(PUSH_ARG_NOTE_ID) == wpComNoteId
            }?.key?.let {
                notificationManager.cancel(it)
                ACTIVE_NOTIFICATIONS_MAP.remove(it)
            }

            // If there are no notifications left, cancel the group as well and clear the unseen state
            if (!hasNotifications()) {
                notificationManager.cancel(GROUP_NOTIFICATION_ID)
                setHasUnseenReviewNotifs(false)
            }
        }

        /**
         * Attach default properties and track given analytics for the given notifications-related [stat].
         *
         * Will skip tracking if user has disabled notifications from being shown at the app system settings level.
         */
        private fun bumpPushNotificationsAnalytics(context: Context, stat: Stat, noteBundle: Bundle) {
            if (!NotificationsUtils.isNotificationsEnabled(context)) return

            val wpComNoteId = noteBundle.getString(PUSH_ARG_NOTE_ID, "")
            if (wpComNoteId.isNotEmpty()) {
                val properties = mutableMapOf<String, Any>()
                properties["notification_note_id"] = wpComNoteId

                noteBundle.getString(PUSH_ARG_TYPE)?.takeUnless { it.isEmpty() }?.let { noteType ->
                    // 'comment' types are sent in PN as type = "c"
                    properties["notification_type"] = if (noteType == PUSH_TYPE_COMMENT) "comment" else noteType
                }

                val preferences = PreferenceManager.getDefaultSharedPreferences(context)
                val latestGCMToken = preferences.getString(FCMRegistrationIntentService.WPCOM_PUSH_DEVICE_TOKEN, null)
                properties["push_notification_token"] = latestGCMToken ?: ""
                AnalyticsTracker.track(stat, properties)
            }
        }

        /**
         * Called when we want to update the unseen state of review notifs - changes the related
         * shared preference and posts an EventBus event so main activity can update the badge
         */
        private fun setHasUnseenReviewNotifs(hasUnseen: Boolean) {
            if (hasUnseen != AppPrefs.getHasUnseenReviews()) {
                AppPrefs.setHasUnseenReviews(hasUnseen)
                EventBus.getDefault().post(NotificationsUnseenReviewsEvent(hasUnseen))
            }
        }
    }

    class NotificationsUnseenReviewsEvent(var hasUnseen: Boolean)

    class NotificationReceivedEvent(var channel: NotificationChannelType)

    /**
     * Note that we have separate notification channels for orders with and without the cha-ching sound - this is
     * necessary because once a channel is created we can't change it, and if we delete the channel and re-create
     * it then it will be re-created with the same settings it previously had (ie: we can't simply have a single
     * channel for orders and add/remove the sound from it)
     */
    enum class NotificationChannelType {
        OTHER,
        REVIEW,
        NEW_ORDER,
    }

    @Synchronized fun buildAndShowNotificationFromNoteData(context: Context, data: Bundle, account: AccountModel) {
        if (data.isEmpty) {
            WooLog.e(T.NOTIFS, "Push notification received without a valid Bundle!")
            return
        }

        val wpComNoteId = data.getString(PUSH_ARG_NOTE_ID, "")
        if (wpComNoteId.isNullOrEmpty()) {
            // At this point 'note_id' is always available in the notification bundle.
            WooLog.e(T.NOTIFS, "Push notification received without a valid note_id in the payload!")
            return
        }

        val pushUserId = data.getString(PUSH_ARG_USER)
        // pushUserId is always set server side, but better to double check it here.
        if (account.userId.toString() != pushUserId) {
            WooLog.e(T.NOTIFS, "WP.com userId found in the app doesn't match with the ID in the PN. Aborting.")
            return
        }

        val noteTypeStr = StringUtils.notNullStr(data.getString(PUSH_ARG_TYPE))
        val noteType = when (noteTypeStr) {
            PUSH_TYPE_NEW_ORDER -> {
                NEW_ORDER
            }
            PUSH_TYPE_COMMENT -> {
                REVIEW
            }
            else -> {
                OTHER
            }
        }

        // Build notification from message data, save to the database, and send request to
        // fetch the actual notification from the api.
        NotificationsUtils.buildNotificationModelFromBundle(data)?.let {
            // Save temporary notification to the database.
            dispatcher.dispatch(NotificationActionBuilder.newUpdateNotificationAction(it))

            // Fire off the event to fetch the actual notification from the api
            dispatcher.dispatch(NotificationActionBuilder
                    .newFetchNotificationAction(FetchNotificationPayload(it.remoteNoteId)))
        }

        // don't display the notification if user chose to disable this type of notification - note
        // that we skip this for API 26+ since Oreo added per-app notification settings via channels
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            if ((noteType == NEW_ORDER && !AppPrefs.isOrderNotificationsEnabled()) ||
                    (noteType == REVIEW && !AppPrefs.isReviewNotificationsEnabled())) {
                WooLog.i(T.NOTIFS, "Skipped $noteTypeStr notification")
                return
            }
        }

        val title = if (noteType == NEW_ORDER) {
            // New order notifications have title 'WordPress.com' - just show the app name instead
            // TODO Consider revising this, perhaps use the contents of the note as the title/body of the notification
            context.getString(R.string.app_name)
        } else {
            StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_TITLE))
                    ?: context.getString(R.string.app_name)
        }

        val message = StringEscapeUtils.unescapeHtml4(data.getString(PUSH_ARG_MSG))

        // New order notifications have the same notification_note_id. So if there is a new incoming notification
        // when there is an existing new order notification in the notification tray,
        // the cha ching sound is not played and the new notification replaces the existing notification
        // See issue for more details: https://github.com/woocommerce/woocommerce-android/issues/1076
        // This solution is a temporary HACK to generate a random number for each new order notification
        // and use that number to group notifications along with notification_note_id
        val randomNumber = if (noteType == NEW_ORDER) Random.nextInt() else 0
        val localPushId = getLocalPushIdForWpComNoteId(wpComNoteId, randomNumber)
        ACTIVE_NOTIFICATIONS_MAP[getLocalPushId(localPushId, randomNumber)] = data

        if (NotificationsUtils.isNotificationsEnabled(context)) {
            bumpPushNotificationsAnalytics(context, Stat.PUSH_NOTIFICATION_RECEIVED, data)
            AnalyticsTracker.flush()
        }

        // Build the new notification, add group to support wearable stacking
        val builder = getNotificationBuilder(context, noteType, title, message)
        val largeIconBitmap = getLargeIconBitmap(context, data.getString("icon"),
                shouldCircularizeNoteIcon(noteType))
        largeIconBitmap?.let { builder.setLargeIcon(it) }

        showSingleNotificationForBuilder(context, builder, noteType, wpComNoteId, localPushId)

        // Also add a group summary notification, which is required for non-wearable devices
        // Do not need to play the sound again. We've already played it in the individual builder.
        showGroupNotificationForBuilder(context, builder, noteType, wpComNoteId, message)

        if (noteType == REVIEW) {
            setHasUnseenReviewNotifs(true)
        }

        EventBus.getDefault().post(NotificationReceivedEvent(noteType))
    }

    /**
     * For a given remote note ID, return a unique local ID to track that notification with, or return
     * the existing local ID if a notification matching the remote note ID is already being displayed.
     */
    private fun getLocalPushIdForWpComNoteId(wpComNoteId: String, randomNumber: Int): Int {
        // Update notification content for the same noteId if it is already showing
        for (id in ACTIVE_NOTIFICATIONS_MAP.keys) {
            val noteBundle = ACTIVE_NOTIFICATIONS_MAP[getLocalPushId(id, randomNumber)]
            if (noteBundle?.getString(PUSH_ARG_NOTE_ID, "") == wpComNoteId) {
                return id
            }
        }

        // Notification isn't already showing
        return PUSH_NOTIFICATION_ID + ACTIVE_NOTIFICATIONS_MAP.size
    }

    private fun getLocalPushId(wpComNoteId: Int, randomNumber: Int) = wpComNoteId + randomNumber

    private fun getLargeIconBitmap(context: Context, iconUrl: String?, shouldCircularizeIcon: Boolean): Bitmap? {
        iconUrl?.let {
            try {
                val decodedIconUrl = URLDecoder.decode(iconUrl, "UTF-8")
                val largeIconSize = context.resources.getDimensionPixelSize(
                        android.R.dimen.notification_large_icon_height
                )
                val resizedUrl = PhotonUtils.getPhotonImageUrl(decodedIconUrl, largeIconSize, largeIconSize)

                val largeIconBitmap = Glide.with(context)
                        .asBitmap()
                        .load(resizedUrl)
                        .submit()
                        .get()

                if (largeIconBitmap != null && shouldCircularizeIcon) {
                    return ImageUtils.getCircularBitmap(largeIconBitmap)
                }

                return largeIconBitmap
            } catch (e: UnsupportedEncodingException) {
                WooLog.e(T.NOTIFS, e)
            } catch (e: ExecutionException) {
                // ExecutionException happens when the image fails to load.
                // handling the exception here to gracefully display notification, without icon
                // instead of crashing the app
                WooLog.e(T.NOTIFS, "Failed to load image with url $iconUrl")
            }
        }
        return null
    }

    private fun getChannelIdForNoteType(context: Context, noteType: NotificationChannelType): String {
        return when (noteType) {
            NEW_ORDER -> context.getString(R.string.notification_channel_order_id)
            REVIEW -> context.getString(R.string.notification_channel_review_id)
            else -> context.getString(R.string.notification_channel_general_id)
        }
    }

    private fun getChannelTitleForNoteType(context: Context, noteType: NotificationChannelType): String {
        return when (noteType) {
            NEW_ORDER -> context.getString(R.string.notification_channel_order_title)
            REVIEW -> context.getString(R.string.notification_channel_review_title)
            else -> context.getString(R.string.notification_channel_general_title)
        }
    }

    /**
     * Ensures the desired notification channel is created when on API 26+, does nothing otherwise since notification
     * channels weren't added until API 26
     */
    private fun createNotificationChannel(context: Context, noteType: NotificationChannelType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = getChannelIdForNoteType(context, noteType)

            // check for existing channel first
            manager.getNotificationChannel(channelId)?.let {
                return
            }

            // create the channel since it doesn't already exist
            val channelName = getChannelTitleForNoteType(context, noteType)
            val channel = NotificationChannel(channelId, channelName, IMPORTANCE_DEFAULT)

            // add cha-ching sound to new order notifications
            if (noteType == NEW_ORDER) {
                val attributes = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                channel.setSound(getChaChingUri(context), attributes)
            }

            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Called at startup to ensure we create the notification channels
     */
    fun createNotificationChannels(context: Context) {
        for (noteType in NotificationChannelType.values()) {
            createNotificationChannel(context, noteType)
        }
    }

    /**
     * Returns the URI to use for the cha-ching order notification sound
     */
    private fun getChaChingUri(context: Context): Uri {
        return Uri.parse(SCHEME_ANDROID_RESOURCE + "://" + context.packageName + "/" + R.raw.cha_ching)
    }

    /**
     * Returns true if the note type is known to have a Gravatar.
     */
    private fun shouldCircularizeNoteIcon(noteType: NotificationChannelType): Boolean {
        return when (noteType) {
            REVIEW -> true
            else -> false
        }
    }

    private fun getNotificationBuilder(
        context: Context,
        noteType: NotificationChannelType,
        title: String,
        message: String?
    ): NotificationCompat.Builder {
        val channelId = getChannelIdForNoteType(context, noteType)
        return NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_woo_w_notification)
                .setColor(ContextCompat.getColor(context, R.color.color_primary))
                .setContentTitle(title)
                .setContentText(message)
                .setTicker(message)
                .setOnlyAlertOnce(true)
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setGroup(NOTIFICATION_GROUP_KEY)
    }

    private fun showSingleNotificationForBuilder(
        context: Context,
        builder: NotificationCompat.Builder,
        noteType: NotificationChannelType,
        wpComNoteId: String,
        pushId: Int
    ) {
        when (noteType) {
            NEW_ORDER -> {
                if (AppPrefs.isOrderNotificationsChaChingEnabled()) {
                    builder.setDefaults(NotificationCompat.DEFAULT_LIGHTS or NotificationCompat.DEFAULT_VIBRATE)
                    builder.setSound(getChaChingUri(context))
                } else {
                    builder.setDefaults(NotificationCompat.DEFAULT_ALL)
                }
            }
            REVIEW -> {
                builder.setDefaults(NotificationCompat.DEFAULT_ALL)
                // TODO: Add quick actions for reviews
            }
            else -> {
                builder.setDefaults(NotificationCompat.DEFAULT_ALL)
            }
        }

        showWPComNotificationForBuilder(builder, context, wpComNoteId, pushId, noteType)
    }

    private fun showGroupNotificationForBuilder(
        context: Context,
        builder: NotificationCompat.Builder,
        noteType: NotificationChannelType,
        wpComNoteId: String,
        message: String?
    ) {
        // Using a copy of the map to avoid concurrency problems
        val notesMap = ACTIVE_NOTIFICATIONS_MAP.toMap()
        if (notesMap.size > 1) {
            val inboxStyle = NotificationCompat.InboxStyle()

            var noteCounter = 1
            for (pushBundle in notesMap.values) {
                // InboxStyle notification is limited to 5 lines
                if (noteCounter > MAX_INBOX_ITEMS) break

                // Skip notes with no content from the 5-line inbox
                if (pushBundle.getString(PUSH_ARG_MSG) == null) continue

                if (noteType == REVIEW) {
                    val pnTitle = StringEscapeUtils.unescapeHtml4(pushBundle.getString(PUSH_ARG_TITLE))
                    val pnMessage = StringEscapeUtils.unescapeHtml4(pushBundle.getString(PUSH_ARG_MSG))
                    inboxStyle.addLine("$pnTitle: $pnMessage")
                } else {
                    val pnMessage = StringEscapeUtils.unescapeHtml4(pushBundle.getString(PUSH_ARG_MSG))
                    inboxStyle.addLine(pnMessage)
                }

                noteCounter++
            }

            if (notesMap.size > MAX_INBOX_ITEMS) {
                inboxStyle.setSummaryText(
                        String.format(context.getString(R.string.more_notifications), notesMap.size - MAX_INBOX_ITEMS)
                )
            }

            val subject = String.format(context.getString(R.string.new_notifications), notesMap.size)
            val groupBuilder = NotificationCompat.Builder(context, getChannelIdForNoteType(context, noteType))
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
                    .setSmallIcon(R.drawable.ic_woo_w_notification)
                    .setColor(ContextCompat.getColor(context, R.color.color_primary))
                    .setGroup(NOTIFICATION_GROUP_KEY)
                    .setGroupSummary(true)
                    .setAutoCancel(true)
                    .setTicker(message)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setContentText(subject)
                    .setStyle(inboxStyle)
                    .setSound(null)
                    .setVibrate(null)

            showWPComNotificationForBuilder(groupBuilder, context, wpComNoteId, GROUP_NOTIFICATION_ID, noteType)
        } else {
            // Set the individual notification we've already built as the group summary
            builder.setGroupSummary(true)
                    .setGroupAlertBehavior(NotificationCompat.GROUP_ALERT_CHILDREN)
            showWPComNotificationForBuilder(builder, context, wpComNoteId, GROUP_NOTIFICATION_ID, noteType)
        }
    }

    /**
     * Creates a notification for a WordPress.com note, attaching an intent for the note's tap action.
     */
    private fun showWPComNotificationForBuilder(
        builder: NotificationCompat.Builder,
        context: Context,
        wpComNoteId: String,
        pushId: Int,
        noteType: NotificationChannelType
    ) {
        // Open the app and load the notifications tab
        val resultIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.FIELD_OPENED_FROM_PUSH, true)
            putExtra(MainActivity.FIELD_REMOTE_NOTE_ID, wpComNoteId.toLong())
            putExtra(MainActivity.FIELD_NOTIFICATION_TYPE, noteType.name)
            if (pushId == GROUP_NOTIFICATION_ID) {
                putExtra(MainActivity.FIELD_OPENED_FROM_PUSH_GROUP, true)
            }
        }

        showNotificationForBuilder(builder, context, resultIntent, pushId)
    }

    // Displays a notification to the user
    private fun showNotificationForBuilder(
        builder: NotificationCompat.Builder,
        context: Context,
        resultIntent: Intent,
        pushId: Int
    ) {
        // Call processing service when notification is dismissed
        val pendingDeleteIntent = NotificationsProcessingService.getPendingIntentForNotificationDismiss(context, pushId)
        builder.setDeleteIntent(pendingDeleteIntent)

        builder.setCategory(NotificationCompat.CATEGORY_SOCIAL)

        try {
            val pendingIntent = PendingIntent.getActivity(
                    context, pushId, resultIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setContentIntent(pendingIntent)
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(pushId, builder.build())
        } catch (e: RemoteException) {
            // see https://github.com/woocommerce/woocommerce-android/issues/920
            WooLog.e(T.NOTIFS, e)
        }
    }

    /**
     * Shows a notification stating that the user has a reply pending from Zendesk. Since Zendesk always sends a
     * notification with the same title and message, we use our own localized messaging. For the same reason,
     * we use a static push notification ID. Tapping on the notification will open the `My Tickets` page.
     */
    fun handleZendeskNotification(context: Context) {
        val title = context.getString(R.string.support_push_notification_title)
        val message = context.getString(R.string.support_push_notification_message)

        val resultIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.FIELD_OPENED_FROM_PUSH, true)
            putExtra(MainActivity.FIELD_REMOTE_NOTE_ID, ZENDESK_PUSH_NOTIFICATION_ID)
            putExtra(MainActivity.FIELD_OPENED_FROM_ZENDESK, true)
        }

        // Build the new notification, add group to support wearable stacking
        val builder = getNotificationBuilder(context, OTHER, title, message)
        showNotificationForBuilder(builder, context, resultIntent, ZENDESK_PUSH_NOTIFICATION_ID)
    }
}
