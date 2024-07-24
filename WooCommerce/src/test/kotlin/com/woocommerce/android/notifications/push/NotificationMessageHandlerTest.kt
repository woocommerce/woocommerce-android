package com.woocommerce.android.notifications.push

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.background.WorkManagerScheduler
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.Base64Decoder
import com.woocommerce.android.util.NotificationsParser
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLogWrapper
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.only
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationPayload
import org.wordpress.android.fluxc.store.SiteStore

class NotificationMessageHandlerTest {
    private lateinit var notificationMessageHandler: NotificationMessageHandler

    private val accountModel = AccountModel().apply { userId = 12345 }
    private val accountStore: AccountStore = mock {
        on { account } doReturn accountModel
    }
    private val siteStore: SiteStore = mock {
        on { getSiteBySiteId(any()) } doReturn SiteModel()
    }
    private val dispatcher: Dispatcher = mock()
    private val actionCaptor: KArgumentCaptor<Action<*>> = argumentCaptor()
    private val wooLogWrapper: WooLogWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invocationOnMock ->
            "${invocationOnMock.arguments[0]}-${invocationOnMock.arguments[1]}"
        }
    }
    private val notificationBuilder: WooNotificationBuilder = mock()
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker = mock()
    private val jvmBase64Decoder: Base64Decoder = mock {
        on { decode(any<String>(), any()) } doAnswer {
            java.util.Base64.getDecoder().decode(it.arguments.first() as String)
        }
    }
    private val notificationsParser: NotificationsParser = NotificationsParser(jvmBase64Decoder)
    private val selectedSite: SelectedSite = mock {
        on { exists() }.thenReturn(true)
    }

    private val orderNotificationPayload = NotificationTestUtils.generateTestNewOrderNotificationPayload(
        userId = accountModel.userId
    )
    private val orderNotification = notificationsParser
        .buildNotificationModelFromPayloadMap(orderNotificationPayload)!!.toAppModel(resourceProvider)

    private val reviewNotificationPayload = NotificationTestUtils.generateTestNewReviewNotificationPayload(
        userId = accountModel.userId
    )

    private val reviewNotification = notificationsParser
        .buildNotificationModelFromPayloadMap(reviewNotificationPayload)!!.toAppModel(resourceProvider)

    private val workManagerScheduler: WorkManagerScheduler = mock()

    @Before
    fun setUp() {
        notificationMessageHandler = NotificationMessageHandler(
            accountStore = accountStore,
            wooLogWrapper = wooLogWrapper,
            dispatcher = dispatcher,
            resourceProvider = resourceProvider,
            notificationBuilder = notificationBuilder,
            analyticsTracker = notificationAnalyticsTracker,
            notificationsParser = notificationsParser,
            selectedSite = selectedSite,
            workManagerScheduler = workManagerScheduler,
        )

        doReturn(true).whenever(accountStore).hasAccessToken()
        doReturn(accountModel).whenever(accountStore).account
        doReturn(true).whenever(notificationBuilder).isNotificationsEnabled()
    }

    @Test
    fun `when the user is not logged in, then do not process the incoming notification`() {
        doReturn(false).whenever(accountStore).hasAccessToken()

        notificationMessageHandler.onNewMessageReceived(emptyMap())
        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(wooLogWrapper, only()).e(eq(WooLog.T.NOTIFS), eq("User is not logged in!"))
    }

    @Test
    fun `given site is not selected, when new message received, then do not process the incoming notification`() {
        doReturn(false).whenever(accountStore).hasAccessToken()
        doReturn(false).whenever(selectedSite).exists()

        notificationMessageHandler.onNewMessageReceived(emptyMap())

        verifyNoInteractions(notificationBuilder)
    }

    @Test
    fun `when the notification payload data is empty, then do not process the notification`() {
        notificationMessageHandler.onNewMessageReceived(emptyMap())
        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(wooLogWrapper, only()).e(
            eq(WooLog.T.NOTIFS),
            eq("Push notification received without a valid Bundle!")
        )
    }

    @Test
    fun `when the user id does not match, then do not process the notification`() {
        notificationMessageHandler.onNewMessageReceived(mapOf("type" to "new_order", "user" to "67890"))
        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(wooLogWrapper, only()).e(
            eq(WooLog.T.NOTIFS),
            eq("WP.com userId found in the app doesn't match with the ID in the PN. Aborting.")
        )
    }

    @Test
    fun `when the notification payload is empty then do not process the notification`() {
        notificationMessageHandler.onNewMessageReceived(
            mapOf(
                "type" to "new_order",
                "user" to accountModel.userId.toString()
            )
        )

        verify(wooLogWrapper, only()).e(eq(WooLog.T.NOTIFS), eq("Notification data is empty!"))
    }

    @Test
    fun `when an incoming notification is received, then we should update that notification to local cache`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(NotificationModel::class.java)
            assertThat(it as NotificationModel).isNotNull()
        }
    }

    @Test
    fun `when an incoming notification is received, then we should request the notification fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(FetchNotificationPayload::class.java)
            assertThat((it as FetchNotificationPayload).remoteNoteId).isNotNull()
        }
    }

    @Test
    fun `when review notifications are received, then do not request all orders diff fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload)

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isNotInstanceOf(FetchNotificationPayload::class.java)
        }
        verify(workManagerScheduler, never()).scheduleOrderUpdate(any())
    }

    @Test
    fun `when order notifications are received, then we should request all orders diff fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)
        verify(workManagerScheduler).scheduleOrderUpdate(any())
    }

    @Test
    fun `when order and review notifications are received together, then display notification details correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(),
            any(),
            any(),
            any()
        )

        // new incoming review notification
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload)

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val subject = resourceProvider.getString(R.string.new_notifications, 2)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = eq("${orderNotification.noteMessage!!}\n${reviewNotification.noteMessage!!}"),
            subject = eq(subject),
            summaryText = eq(null),
            notification = eq(reviewNotification)
        )
    }

    @Test
    fun `when two new order notifications are received for the same store, then display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(),
            any(),
            any(),
            any()
        )

        // new incoming order notification
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_ORDER_NOTE_FULL_DATA_2
        )
        val orderNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            orderNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload2)

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification2),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val subject = resourceProvider.getString(R.string.new_notifications, 2)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = eq("${orderNotification.noteMessage!!}\n${orderNotification2.noteMessage!!}"),
            subject = eq(subject),
            summaryText = eq(null),
            notification = eq(orderNotification2)
        )
    }

    @Test
    fun `when two new review notifications are received for the same store, then display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload)

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(),
            any(),
            any(),
            any()
        )

        // new incoming review notification
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_REVIEW_NOTE_FULL_DATA_2
        )
        val reviewNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            reviewNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload2)

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification2),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val subject = resourceProvider.getString(R.string.new_notifications, 2)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = eq("${reviewNotification.noteMessage!!}\n${reviewNotification2.noteMessage!!}"),
            subject = eq(subject),
            summaryText = eq(null),
            notification = eq(reviewNotification2)
        )
    }

    @Test
    fun `when two new order notifications are received for different stores display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(),
            any(),
            any(),
            any()
        )

        // new incoming order notification for different store
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_ORDER_NOTE_FULL_DATA_SITE_2
        )
        val orderNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            orderNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload2)

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification2),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val subject = resourceProvider.getString(R.string.new_notifications, 2)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = eq("${orderNotification.noteMessage!!}\n${orderNotification2.noteMessage!!}"),
            subject = eq(subject),
            summaryText = eq(null),
            notification = eq(orderNotification2)
        )
    }

    @Test
    fun `when two new review notifications are received for different stores, display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload)

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(),
            any(),
            any(),
            any()
        )

        // new incoming review notification
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_REVIEW_NOTE_FULL_DATA_SITE_2
        )
        val reviewNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            reviewNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload2)

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification2),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val subject = resourceProvider.getString(R.string.new_notifications, 2)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = eq("${reviewNotification.noteMessage!!}\n${reviewNotification2.noteMessage!!}"),
            subject = eq(subject),
            summaryText = eq(null),
            notification = eq(reviewNotification2)
        )
    }

    @Test
    fun `when more than 5 notifications are received for same store, display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()
        val notificationsCount = NotificationMessageHandler.MAX_INBOX_ITEMS + 1
        val notifications = List(notificationsCount) { orderNotification }

        repeat(notificationsCount) {
            notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)
        }

        // verify that the contents for the group notification is correct
        val subject = resourceProvider.getString(
            R.string.new_notifications,
            notificationsCount
        )
        val summary = resourceProvider.getString(
            R.string.more_notifications,
            notificationsCount - NotificationMessageHandler.MAX_INBOX_ITEMS
        )
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = eq(
                notifications.take(NotificationMessageHandler.MAX_INBOX_ITEMS)
                    .joinToString(separator = "\n") { it.noteMessage.orEmpty() }
            ),
            subject = eq(subject),
            summaryText = eq(summary),
            notification = eq(orderNotification)
        )
    }

    @Test
    fun `when notification is clicked, then mark new notification as tapped correctly`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)
        notificationMessageHandler.markNotificationTapped(orderNotification.remoteNoteId)

        verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
            eq(AnalyticsEvent.PUSH_NOTIFICATION_TAPPED),
            eq(orderNotification)
        )
    }

    @Test
    fun `when new order notification is clicked, then mark only new order notification as tapped correctly`() {
        doReturn(true).whenever(notificationBuilder).isNotificationsEnabled()
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)
        notificationMessageHandler.markNotificationsOfTypeTapped(orderNotification.channelType)

        verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
            eq(AnalyticsEvent.PUSH_NOTIFICATION_TAPPED),
            eq(orderNotification)
        )
    }
}
