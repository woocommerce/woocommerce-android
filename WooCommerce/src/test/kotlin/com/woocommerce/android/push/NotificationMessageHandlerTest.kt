package com.woocommerce.android.push

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_2
import com.woocommerce.android.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_2
import com.woocommerce.android.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.support.ZendeskHelper
import com.woocommerce.android.util.*
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.notification.NotificationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.NotificationStore.FetchNotificationPayload
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrderListPayload

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
    private val zendeskHelper: ZendeskHelper = mock()
    private val actionCaptor: KArgumentCaptor<Action<*>> = argumentCaptor()
    private val wooLogWrapper: WooLogWrapper = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
        on { getString(any(), any()) } doAnswer { invocationOnMock -> invocationOnMock.arguments[0].toString() }
    }
    private val notificationBuilder: WooNotificationBuilder = mock()
    private val notificationAnalyticsTracker: NotificationAnalyticsTracker = mock()
    private val jvmBase64Decoder: Base64Decoder = mock {
        on { decode(any<String>(), any()) } doAnswer {
            java.util.Base64.getDecoder().decode(it.arguments.first() as String)
        }
    }
    private val notificationsParser: NotificationsParser = NotificationsParser(jvmBase64Decoder)

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

    @Before
    fun setUp() {
        notificationMessageHandler = NotificationMessageHandler(
            accountStore = accountStore,
            wooLogWrapper = wooLogWrapper,
            dispatcher = dispatcher,
            siteStore = siteStore,
            appPrefsWrapper = appPrefsWrapper,
            resourceProvider = resourceProvider,
            notificationBuilder = notificationBuilder,
            analyticsTracker = notificationAnalyticsTracker,
            zendeskHelper = zendeskHelper,
            notificationsParser = notificationsParser,
        )

        doReturn(true).whenever(accountStore).hasAccessToken()
        doReturn(accountModel).whenever(accountStore).account
        doReturn(true).whenever(appPrefsWrapper).isOrderNotificationsEnabled()
        doReturn(true).whenever(appPrefsWrapper).isOrderNotificationsEnabled()
        doReturn(true).whenever(appPrefsWrapper).isReviewNotificationsEnabled()
        doReturn(true).whenever(appPrefsWrapper).isOrderNotificationsChaChingEnabled()
        doReturn(true).whenever(notificationBuilder).isNotificationsEnabled()
    }

    @Test
    fun `when the user is not logged in, then do not process the incoming notification`() {
        doReturn(false).whenever(accountStore).hasAccessToken()

        notificationMessageHandler.onNewMessageReceived(emptyMap(), mock())
        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(wooLogWrapper, only()).e(eq(WooLog.T.NOTIFS), eq("User is not logged in!"))
    }

    @Test
    fun `when the notification payload data is empty, then do not process the notification`() {
        notificationMessageHandler.onNewMessageReceived(emptyMap(), mock())
        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(wooLogWrapper, only()).e(
            eq(WooLog.T.NOTIFS),
            eq("Push notification received without a valid Bundle!")
        )
    }

    @Test
    fun `when the user id does not match, then do not process the notification`() {
        notificationMessageHandler.onNewMessageReceived(mapOf("type" to "new_order", "user" to "67890"), mock())
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
            ),
            mock()
        )

        verify(wooLogWrapper, only()).e(eq(WooLog.T.NOTIFS), eq("Notification data is empty!"))
    }

    @Test
    fun `when an incoming notification is received, then we should update that notification to local cache`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(NotificationModel::class.java)
            assertThat(it as NotificationModel).isNotNull()
        }
    }

    @Test
    fun `when an incoming notification is received, then we should request the notification fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(FetchNotificationPayload::class.java)
            assertThat((it as FetchNotificationPayload).remoteNoteId).isNotNull()
        }
    }

    @Test
    fun `when review notifications are received, then do not request all orders diff fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload, mock())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isNotInstanceOf(FetchNotificationPayload::class.java)
        }
    }

    @Test
    fun `when order notification is received for a non existent site, then do not request all orders diff fetch`() {
        doReturn(null).whenever(siteStore).getSiteBySiteId(any())
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isNotInstanceOf(FetchNotificationPayload::class.java)
        }
        verify(wooLogWrapper, only()).e(
            eq(WooLog.T.NOTIFS),
            eq("Site not found - can't dispatchNewOrderEvents")
        )
    }

    @Test
    fun `when order notifications are received, then we should request all orders diff fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(FetchOrderListPayload::class.java)
            assertThat((it as FetchOrderListPayload).listDescriptor.statusFilter).isNull()
        }
    }

    @Test
    fun `when order notifications are received, then we should request processing orders diff fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        verify(dispatcher, atLeastOnce()).dispatch(actionCaptor.capture())

        assertThat(actionCaptor.allValues.map { it.payload }).anySatisfy {
            assertThat(it).isInstanceOf(FetchOrderListPayload::class.java)
            assertThat((it as FetchOrderListPayload).listDescriptor.statusFilter)
                .isEqualTo(CoreOrderStatus.PROCESSING.value)
        }
    }

    @Test
    fun `when order notification is received but not enabled, then do not display notification`() {
        doReturn(false).whenever(appPrefsWrapper).isOrderNotificationsEnabled()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())
        verify(wooLogWrapper, only()).i(
            eq(WooLog.T.NOTIFS),
            eq("Skipped ${orderNotification.noteType.name} notification")
        )
    }

    @Test
    fun `when review notification is received but not enabled, then do not display notification`() {
        doReturn(false).whenever(appPrefsWrapper).isReviewNotificationsEnabled()

        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload, mock())
        verify(wooLogWrapper, only()).i(
            eq(WooLog.T.NOTIFS),
            eq("Skipped ${reviewNotification.noteType.name} notification")
        )
    }

    @Test
    fun `when zendesk notification is received, then display notification details correctly`() {
        val notificationPayload = mapOf("type" to "zendesk")
        val zendeskNote = NotificationModel(noteId = 1999999999, remoteNoteId = 1999999999).toAppModel(resourceProvider)

        notificationMessageHandler.onNewMessageReceived(notificationPayload, mock())

        verify(notificationBuilder, atLeastOnce()).buildAndDisplayZendeskNotification(
            channelId = eq(resourceProvider.getString(NotificationChannelType.OTHER.getChannelId())),
            notification = eq(zendeskNote)
        )
    }

    @Test
    fun `when zendesk notification is received, then refresh zendesk UI`() {
        val dummyRequestId = "dummy"
        val notificationPayload = mapOf("type" to "zendesk", "zendesk_sdk_request_id" to dummyRequestId)

        notificationMessageHandler.onNewMessageReceived(notificationPayload, mock())

        verify(zendeskHelper).refreshRequest(any(), eq(dummyRequestId))
    }

    @Test
    fun `when order and review notifications are received together, then display notification details correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        val orderDefaults = orderNotification.channelType.getDefaults(appPrefsWrapper)
        val orderChannelId = resourceProvider.getString(orderNotification.channelType.getChannelId())

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(orderDefaults),
            channelId = eq(orderChannelId),
            notification = eq(orderNotification),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(), any(), any(), any(), any(), any()
        )

        // new incoming review notification
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload, mock())

        val reviewDefaults = reviewNotification.channelType.getDefaults(appPrefsWrapper)
        val reviewChannelId = resourceProvider.getString(reviewNotification.channelType.getChannelId())

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(reviewDefaults),
            channelId = eq(reviewChannelId),
            notification = eq(reviewNotification),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val groupChannelId = resourceProvider.getString(reviewNotification.channelType.getChannelId())
        val subject = resourceProvider.getString(R.string.new_notifications, 1)
        val summaryText = resourceProvider.getString(R.string.more_notifications, 1)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            channelId = eq(groupChannelId),
            inboxMessage = eq("${orderNotification.noteMessage!!}\n${reviewNotification.noteMessage!!}\n"),
            subject = eq(subject),
            summaryText = eq(summaryText),
            notification = eq(reviewNotification),
            shouldDisplaySummaryText = eq(false)
        )
    }

    @Test
    fun `when two new order notifications are received for the same store, then display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        val orderDefaults = orderNotification.channelType.getDefaults(appPrefsWrapper)
        val orderChannelId = resourceProvider.getString(orderNotification.channelType.getChannelId())

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(orderDefaults),
            channelId = eq(orderChannelId),
            notification = eq(orderNotification),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(), any(), any(), any(), any(), any()
        )

        // new incoming order notification
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId, noteData = TEST_ORDER_NOTE_FULL_DATA_2
        )
        val orderNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            orderNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload2, mock())

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(orderDefaults),
            channelId = eq(orderChannelId),
            notification = eq(orderNotification2),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val groupChannelId = resourceProvider.getString(orderNotification2.channelType.getChannelId())
        val subject = resourceProvider.getString(R.string.new_notifications, 1)
        val summaryText = resourceProvider.getString(R.string.more_notifications, 1)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            channelId = eq(groupChannelId),
            inboxMessage = eq("${orderNotification.noteMessage!!}\n${orderNotification2.noteMessage!!}\n"),
            subject = eq(subject),
            summaryText = eq(summaryText),
            notification = eq(orderNotification2),
            shouldDisplaySummaryText = eq(false)
        )
    }

    @Test
    fun `when two new review notifications are received for the same store, then display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload, mock())

        val reviewDefaults = reviewNotification.channelType.getDefaults(appPrefsWrapper)
        val reviewChannelId = resourceProvider.getString(reviewNotification.channelType.getChannelId())

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(reviewDefaults),
            channelId = eq(reviewChannelId),
            notification = eq(reviewNotification),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(), any(), any(), any(), any(), any()
        )

        // new incoming review notification
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId, noteData = TEST_REVIEW_NOTE_FULL_DATA_2
        )
        val reviewNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            reviewNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload2, mock())

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(reviewDefaults),
            channelId = eq(reviewChannelId),
            notification = eq(reviewNotification2),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val groupChannelId = resourceProvider.getString(reviewNotification2.channelType.getChannelId())
        val subject = resourceProvider.getString(R.string.new_notifications, 1)
        val summaryText = resourceProvider.getString(R.string.more_notifications, 1)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            channelId = eq(groupChannelId),
            inboxMessage = eq("${reviewNotification.noteMessage!!}\n${reviewNotification2.noteMessage!!}\n"),
            subject = eq(subject),
            summaryText = eq(summaryText),
            notification = eq(reviewNotification2),
            shouldDisplaySummaryText = eq(false)
        )
    }

    @Test
    fun `when two new order notifications are received for different stores display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())

        val orderDefaults = orderNotification.channelType.getDefaults(appPrefsWrapper)
        val orderChannelId = resourceProvider.getString(orderNotification.channelType.getChannelId())

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(orderDefaults),
            channelId = eq(orderChannelId),
            notification = eq(orderNotification),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(), any(), any(), any(), any(), any()
        )

        // new incoming order notification for different store
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId, noteData = TEST_ORDER_NOTE_FULL_DATA_SITE_2
        )
        val orderNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            orderNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload2, mock())

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(orderDefaults),
            channelId = eq(orderChannelId),
            notification = eq(orderNotification2),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val groupChannelId = resourceProvider.getString(orderNotification2.channelType.getChannelId())
        val subject = resourceProvider.getString(R.string.new_notifications, 1)
        val summaryText = resourceProvider.getString(R.string.more_notifications, 1)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            channelId = eq(groupChannelId),
            inboxMessage = eq("${orderNotification.noteMessage!!}\n${orderNotification2.noteMessage!!}\n"),
            subject = eq(subject),
            summaryText = eq(summaryText),
            notification = eq(orderNotification2),
            shouldDisplaySummaryText = eq(false)
        )
    }

    @Test
    fun `when two new review notifications are received for different stores, display the notification correctly`() {
        // clear all notifications
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload, mock())

        val reviewDefaults = reviewNotification.channelType.getDefaults(appPrefsWrapper)
        val reviewChannelId = resourceProvider.getString(reviewNotification.channelType.getChannelId())

        // verify that the contents for a new order notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(reviewDefaults),
            channelId = eq(reviewChannelId),
            notification = eq(reviewNotification),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(
            any(), any(), any(), any(), any(), any()
        )

        // new incoming review notification
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId, noteData = TEST_REVIEW_NOTE_FULL_DATA_SITE_2
        )
        val reviewNotification2 = notificationsParser.buildNotificationModelFromPayloadMap(
            reviewNotificationPayload2
        )!!.toAppModel(resourceProvider)
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload2, mock())

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            defaults = eq(reviewDefaults),
            channelId = eq(reviewChannelId),
            notification = eq(reviewNotification2),
            addCustomNotificationSound = eq(true),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val groupChannelId = resourceProvider.getString(reviewNotification2.channelType.getChannelId())
        val subject = resourceProvider.getString(R.string.new_notifications, 1)
        val summaryText = resourceProvider.getString(R.string.more_notifications, 1)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            channelId = eq(groupChannelId),
            inboxMessage = eq("${reviewNotification.noteMessage!!}\n${reviewNotification2.noteMessage!!}\n"),
            subject = eq(subject),
            summaryText = eq(summaryText),
            notification = eq(reviewNotification2),
            shouldDisplaySummaryText = eq(false)
        )
    }

    @Test
    fun `when notification is clicked, then mark new notification as tapped correctly`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())
        notificationMessageHandler.markNotificationTapped(orderNotification.remoteNoteId)

        verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
            eq(AnalyticsEvent.PUSH_NOTIFICATION_TAPPED), eq(orderNotification)
        )
    }

    @Test
    fun `when new order notification is clicked, then mark only new order notification as tapped correctly`() {
        doReturn(true).whenever(notificationBuilder).isNotificationsEnabled()
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload, mock())
        notificationMessageHandler.markNotificationsOfTypeTapped(orderNotification.channelType)

        verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
            eq(AnalyticsEvent.PUSH_NOTIFICATION_TAPPED), eq(orderNotification)
        )
    }
}
