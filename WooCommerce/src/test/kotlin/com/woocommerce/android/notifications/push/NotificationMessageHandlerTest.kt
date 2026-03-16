package com.woocommerce.android.notifications.push

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.background.WorkManagerScheduler
import com.woocommerce.android.model.Notification
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.notifications.ActiveNotificationData
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.sitevisibility.GetWooVisibleSites
import com.woocommerce.android.util.Base64Decoder
import com.woocommerce.android.util.NotificationsParser
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
import org.wordpress.android.fluxc.store.WpComPushNotificationStore.FetchNotificationPayload

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationMessageHandlerTest {
    private lateinit var notificationMessageHandler: NotificationMessageHandler

    private val accountModel = AccountModel().apply { userId = 12345 }
    private val accountStore: AccountStore = mock {
        on { account } doReturn accountModel
    }
    private val registrationStatus: PushNotificationRegistrationStatus = mock()
    private val dispatcher: Dispatcher = mock()
    private val actionCaptor: KArgumentCaptor<Action<*>> = argumentCaptor()
    private val wooLog: WooLog = mock()
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
    private lateinit var getWooVisibleSites: GetWooVisibleSites
    private val selectedSite: SelectedSite = mock {
        on { exists() }.thenReturn(true)
    }

    private val orderNotificationPayload = NotificationTestUtils.generateTestNewOrderNotificationPayload(
        userId = accountModel.userId
    )
    private val orderNotification = notificationsParser
        .buildNotificationModelFromPayloadMap(orderNotificationPayload)!!.toAppModel(resourceProvider)
    private val orderNotificationSite2Payload = NotificationTestUtils.generateTestNewOrderNotificationPayload(
        userId = accountModel.userId,
        noteData = TEST_ORDER_NOTE_FULL_DATA_SITE_2
    )
    private val orderNotificationSite2 = notificationsParser
        .buildNotificationModelFromPayloadMap(orderNotificationSite2Payload)!!.toAppModel(resourceProvider)

    private val reviewNotificationPayload = NotificationTestUtils.generateTestNewReviewNotificationPayload(
        userId = accountModel.userId
    )

    private val reviewNotification = notificationsParser
        .buildNotificationModelFromPayloadMap(reviewNotificationPayload)!!.toAppModel(resourceProvider)
    private val reviewNotificationSite2Payload = NotificationTestUtils.generateTestNewReviewNotificationPayload(
        userId = accountModel.userId,
        noteData = TEST_REVIEW_NOTE_FULL_DATA_SITE_2
    )
    private val reviewNotificationSite2 = notificationsParser
        .buildNotificationModelFromPayloadMap(reviewNotificationSite2Payload)!!.toAppModel(resourceProvider)

    private val workManagerScheduler: WorkManagerScheduler = mock()

    private fun createNotificationMessageHandler(
        notificationsParser: NotificationsParser = this.notificationsParser
    ) {
        notificationMessageHandler = NotificationMessageHandler(
            notificationBuilder = notificationBuilder,
            analyticsTracker = notificationAnalyticsTracker,
            notificationsParser = notificationsParser,
            registrationStatus = registrationStatus,
            accountStore = accountStore,
            wooLog = wooLog,
            dispatcher = dispatcher,
            resourceProvider = resourceProvider,
            getWooVisibleSites = getWooVisibleSites,
            selectedSite = selectedSite,
            workManagerScheduler = workManagerScheduler,
        )
    }

    @Before
    fun setUp() {
        val visibleSites = listOf(
            orderNotification.remoteSiteId,
            reviewNotification.remoteSiteId,
            orderNotificationSite2.remoteSiteId,
            reviewNotificationSite2.remoteSiteId
        ).distinct().map { visibleSiteId ->
            mock<SiteModel> {
                on { siteId } doReturn visibleSiteId
            }
        }
        getWooVisibleSites = mock {
            onBlocking { invoke() } doReturn visibleSites
        }
        createNotificationMessageHandler()

        doReturn(true).whenever(accountStore).hasAccessToken()
        doReturn(accountModel).whenever(accountStore).account
        doReturn(true).whenever(notificationBuilder).isNotificationsEnabled()
        doReturn(emptyList<ActiveNotificationData>()).whenever(notificationBuilder).getActiveNotifications()
    }

    @Test
    fun `when the user is not logged in, then do not process the incoming notification`() = runTest {
        whenever(registrationStatus.invoke(any())).thenReturn(PushNotificationRegistrationStatus.Status.UNREGISTERED)
        doReturn(false).whenever(accountStore).hasAccessToken()

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(wooLog).e(eq(WooLog.T.NOTIFICATIONS), eq("User is not logged in!"))
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

        verify(wooLog).e(
            eq(WooLog.T.NOTIFICATIONS),
            eq("Push notification received without a valid Bundle!")
        )
    }

    @Test
    fun `when the user id does not match, then do not process the notification`() = runTest {
        whenever(registrationStatus.invoke(any())).thenReturn(PushNotificationRegistrationStatus.Status.UNREGISTERED)
        val payload = NotificationTestUtils.generateTestNewOrderNotificationPayload(userId = 67890)

        notificationMessageHandler.onNewMessageReceived(payload)
        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(wooLog).e(
            eq(WooLog.T.NOTIFICATIONS),
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

        verify(wooLog, only()).e(eq(WooLog.T.NOTIFICATIONS), eq("Notification data is empty!"))
    }

    @Test
    fun `given woo push registered, when wpcom notification received, then skip notification`() = runTest {
        whenever(registrationStatus.invoke(any()))
            .thenReturn(PushNotificationRegistrationStatus.Status.REGISTERED_WOO_ONLY)

        // WPCOM notifications have remoteNoteId > 0
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(wooLog).d(
            eq(WooLog.T.NOTIFICATIONS),
            eq("Skipping WPCOM notification, already registered with Woo Core")
        )
        verifyNoInteractions(dispatcher)
    }

    @Test
    fun `given woo push registered and site is visible, when woo notification received, then process it`() = runTest {
        whenever(registrationStatus.invoke(any()))
            .thenReturn(PushNotificationRegistrationStatus.Status.REGISTERED_WOO_ONLY)
        val mockNotificationsParser: NotificationsParser = mock {
            on { buildNotificationModelFromPayloadMap(any()) } doReturn NotificationModel(
                remoteNoteId = 0L,
                remoteSiteId = orderNotification.remoteSiteId,
                type = NotificationModel.Kind.STORE_ORDER
            )
        }
        createNotificationMessageHandler(mockNotificationsParser)

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(dispatcher, atLeastOnce()).dispatch(any())
    }

    @Test
    fun `given site is hidden, when notification received, then skip it`() = runTest {
        whenever(registrationStatus.invoke(any()))
            .thenReturn(PushNotificationRegistrationStatus.Status.UNREGISTERED)
        val mockNotificationsParser: NotificationsParser = mock {
            on { buildNotificationModelFromPayloadMap(any()) } doReturn NotificationModel(
                remoteNoteId = 0L,
                remoteSiteId = orderNotification.remoteSiteId,
                type = NotificationModel.Kind.STORE_ORDER
            )
        }
        createNotificationMessageHandler(mockNotificationsParser)
        whenever(getWooVisibleSites.invoke()).thenReturn(emptyList())

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(wooLog).w(
            eq(WooLog.T.NOTIFICATIONS),
            eq("Skipping notification, site ${orderNotification.remoteSiteId} is not visible")
        )
        verifyNoInteractions(dispatcher)
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
        verify(workManagerScheduler, never()).scheduleOrderUpdate(any(), any())
    }

    @Test
    fun `when order notifications are received, then we should request all orders diff fetch from api`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)
        verify(workManagerScheduler).scheduleOrderUpdate(any(), any())
    }

    @Test
    fun `when order and review notifications are received together, then display notification details correctly`() {
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(any(), any(), any())

        // Second notification - simulate first notification being active
        val firstNotificationData = orderNotification.toActiveNotificationData(10000)
        doReturn(listOf(firstNotificationData)).whenever(notificationBuilder).getActiveNotifications()

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
            inboxMessage = eq("${reviewNotification.noteMessage!!}\n${orderNotification.noteMessage!!}"),
            subject = eq(subject),
            notification = eq(reviewNotification)
        )
    }

    @Test
    fun `when two new order notifications are received for the same store, then display correctly`() {
        // First notification
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification),
            isGroupNotification = eq(false)
        )

        // Simulate first notification being active before second arrives
        // Use a DIFFERENT remoteNoteId to ensure second notification is treated as new
        val firstNotificationData = ActiveNotificationData(
            id = 10000,
            remoteNoteId = 999999L, // Different from orderNotification2.remoteNoteId
            remoteSiteId = orderNotification.remoteSiteId,
            channelType = orderNotification.channelType.name,
            noteMessage = orderNotification.noteMessage,
            noteTypeTrackingValue = orderNotification.noteType.trackingValue,
            isGroupSummary = false
        )
        doReturn(listOf(firstNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        // Second notification
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_ORDER_NOTE_FULL_DATA_2
        )
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload2)

        // Verify second notification is displayed as group notification
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = any(),
            isGroupNotification = eq(true)
        )

        // Verify group notification is displayed
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = any(),
            subject = any(),
            notification = any()
        )
    }

    @Test
    fun `when two new review notifications are received for the same store, then display correctly`() {
        // First notification
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload)

        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification),
            isGroupNotification = eq(false)
        )

        // Simulate first notification being active before second arrives
        val firstNotificationData = ActiveNotificationData(
            id = 10000,
            remoteNoteId = 888888L,
            remoteSiteId = reviewNotification.remoteSiteId,
            channelType = reviewNotification.channelType.name,
            noteMessage = reviewNotification.noteMessage,
            noteTypeTrackingValue = reviewNotification.noteType.trackingValue,
            isGroupSummary = false
        )
        doReturn(listOf(firstNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        // Second notification
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_REVIEW_NOTE_FULL_DATA_2
        )
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload2)

        // Verify second notification is displayed as group notification
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = any(),
            isGroupNotification = eq(true)
        )

        // Verify group notification is displayed
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = any(),
            subject = any(),
            notification = any()
        )
    }

    @Test
    fun `when two new order notifications are received for different stores, then display correctly`() {
        // First notification
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(orderNotification),
            isGroupNotification = eq(false)
        )

        // Simulate first notification being active before second arrives
        val firstNotificationData = ActiveNotificationData(
            id = 10000,
            remoteNoteId = 777777L,
            remoteSiteId = orderNotification.remoteSiteId,
            channelType = orderNotification.channelType.name,
            noteMessage = orderNotification.noteMessage,
            noteTypeTrackingValue = orderNotification.noteType.trackingValue,
            isGroupSummary = false
        )
        doReturn(listOf(firstNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        // Second notification for different store
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_ORDER_NOTE_FULL_DATA_SITE_2
        )
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload2)

        // Verify second notification is displayed as group notification
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = any(),
            isGroupNotification = eq(true)
        )

        // Verify group notification is displayed
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = any(),
            subject = any(),
            notification = any()
        )
    }

    @Test
    fun `when two new review notifications are received for different stores, then display correctly`() {
        // First notification
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload)

        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification),
            isGroupNotification = eq(false)
        )

        // Simulate first notification being active before second arrives
        val firstNotificationData = ActiveNotificationData(
            id = 10000,
            remoteNoteId = 666666L,
            remoteSiteId = reviewNotification.remoteSiteId,
            channelType = reviewNotification.channelType.name,
            noteMessage = reviewNotification.noteMessage,
            noteTypeTrackingValue = reviewNotification.noteType.trackingValue,
            isGroupSummary = false
        )
        doReturn(listOf(firstNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        // Second notification for different store
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_REVIEW_NOTE_FULL_DATA_SITE_2
        )
        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload2)

        // Verify second notification is displayed as group notification
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = any(),
            isGroupNotification = eq(true)
        )

        // Verify group notification is displayed
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = any(),
            subject = any(),
            notification = any()
        )
    }

    @Test
    fun `when more than 5 notifications are received for same store, then display correctly`() {
        // Simulate 5 existing notifications
        val existingNotifications = (0 until 5).map { index ->
            ActiveNotificationData(
                id = 10000 + index,
                remoteNoteId = (100000 + index).toLong(),
                remoteSiteId = orderNotification.remoteSiteId,
                channelType = orderNotification.channelType.name,
                noteMessage = "Message $index",
                noteTypeTrackingValue = orderNotification.noteType.trackingValue,
                isGroupSummary = false
            )
        }
        doReturn(existingNotifications).whenever(notificationBuilder).getActiveNotifications()

        // 6th notification arrives
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        // Verify notification is displayed as group notification
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = any(),
            isGroupNotification = eq(true)
        )

        // Verify group notification is displayed with correct count
        val subject = resourceProvider.getString(R.string.new_notifications, 6)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = any(),
            subject = eq(subject),
            notification = any()
        )
    }

    @Test
    fun `when notification is clicked, then mark new notification as tapped correctly`() {
        val mockNotificationData = orderNotification.toActiveNotificationData(10000)
        doReturn(listOf(mockNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        notificationMessageHandler.markNotificationTapped(orderNotification.remoteNoteId)

        verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
            stat = eq(AnalyticsEvent.PUSH_NOTIFICATION_TAPPED),
            siteId = eq(orderNotification.remoteSiteId),
            remoteNoteId = eq(orderNotification.remoteNoteId),
            noteTypeTrackingValue = eq(orderNotification.noteType.trackingValue)
        )
    }

    @Test
    fun `when new order notification is clicked, then mark only new order notification as tapped correctly`() {
        val mockNotificationData = orderNotification.toActiveNotificationData(10000)
        doReturn(listOf(mockNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        notificationMessageHandler.markNotificationsOfTypeTapped(orderNotification.channelType)

        verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
            stat = eq(AnalyticsEvent.PUSH_NOTIFICATION_TAPPED),
            siteId = eq(orderNotification.remoteSiteId),
            remoteNoteId = eq(orderNotification.remoteNoteId),
            noteTypeTrackingValue = eq(orderNotification.noteType.trackingValue)
        )
    }

    @Test
    fun `remove notifications concurrently without throwing ConcurrentModificationException`() {
        notificationMessageHandler.removeAllNotificationsFromSystemsBar()

        runTest {
            repeat(50) {
                launch(Dispatchers.Default) {
                    notificationMessageHandler.removeNotificationByNotificationIdFromSystemsBar(0)
                }
            }

            repeat(50) {
                launch(Dispatchers.Default) {
                    notificationMessageHandler.removeNotificationByNotificationIdFromSystemsBar(0)
                }
            }

            advanceUntilIdle()
        }
    }

    private fun Notification.toActiveNotificationData(id: Int) = ActiveNotificationData(
        id = id,
        remoteNoteId = remoteNoteId,
        remoteSiteId = remoteSiteId,
        channelType = channelType.name,
        noteMessage = noteMessage,
        noteTypeTrackingValue = noteType.trackingValue,
        isGroupSummary = false
    )
}
