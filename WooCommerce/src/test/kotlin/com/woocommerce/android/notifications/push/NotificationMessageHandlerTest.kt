package com.woocommerce.android.notifications.push

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.background.WorkManagerScheduler
import com.woocommerce.android.model.Notification
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.notifications.ActiveNotificationData
import com.woocommerce.android.notifications.NotificationSource
import com.woocommerce.android.notifications.WooNotificationBuilder
import com.woocommerce.android.notifications.WooNotificationType
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_ORDER_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_2
import com.woocommerce.android.notifications.push.NotificationTestUtils.TEST_REVIEW_NOTE_FULL_DATA_SITE_2
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status.REGISTERED_BOTH
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status.REGISTERED_WOO_ONLY
import com.woocommerce.android.notifications.push.PushNotificationRegistrationStatus.Status.UNREGISTERED
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
import org.wordpress.android.fluxc.tools.FormattableMeta

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationMessageHandlerTest {
    private lateinit var notificationMessageHandler: NotificationMessageHandler

    private val accountModel = AccountModel().apply { userId = 12345 }
    private val accountStore: AccountStore = mock {
        on { account } doReturn accountModel
    }
    private val registrationStatus: PushNotificationRegistrationStatus = mock {
        on { invoke(any()) } doReturn UNREGISTERED
    }
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
    private val wooNotificationPayload = mapOf("type" to "new_order")
    private val wooNotificationModel
        get() = NotificationModel(
            remoteNoteId = 0L,
            remoteSiteId = orderNotification.remoteSiteId,
            type = NotificationModel.Kind.STORE_ORDER
        )

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

    private fun createWooNotificationMessageHandler() {
        val mockNotificationsParser: NotificationsParser = mock {
            on { buildNotificationModelFromPayloadMap(any()) } doReturn wooNotificationModel
        }
        createNotificationMessageHandler(mockNotificationsParser)
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
            on { invoke() } doReturn visibleSites
        }
        createNotificationMessageHandler()

        doReturn(true).whenever(accountStore).hasAccessToken()
        doReturn(accountModel).whenever(accountStore).account
        doReturn(true).whenever(notificationBuilder).isNotificationsEnabled()
        doReturn(emptyList<ActiveNotificationData>()).whenever(notificationBuilder).getActiveNotifications()
    }

    @Test
    fun `when the user is not logged in, then do not process the incoming notification`() = runTest {
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
        val payload = NotificationTestUtils.generateTestNewOrderNotificationPayload(userId = 67890)

        notificationMessageHandler.onNewMessageReceived(payload)
        verify(accountStore, atLeastOnce()).hasAccessToken()
        verify(wooLog).e(
            eq(WooLog.T.NOTIFICATIONS),
            eq("WP.com userId found in the app doesn't match with the ID in the PN. Aborting.")
        )
    }

    @Test
    fun `given wpcom payload is missing note id, when notification received, then keep legacy note id validation`() =
        runTest {
            val payload = NotificationTestUtils.generateTestNewOrderNotificationPayload().minus("note_id")
            createWooNotificationMessageHandler()

            notificationMessageHandler.onNewMessageReceived(payload)

            verify(wooLog).e(
                eq(WooLog.T.NOTIFICATIONS),
                eq("Push notification received without a valid note_id in the payload!")
            )
            verifyNoInteractions(dispatcher)
        }

    @Test
    fun `given payload has no wpcom user and parsed note id is zero, when notification received, then treat it as Woo`() =
        runTest {
            val payload = NotificationTestUtils.generateTestNewOrderNotificationPayload().minus("user")
            val wooDrivenOrderId = 4321L
            val wooDrivenModel = NotificationModel(
                remoteNoteId = 0L,
                remoteSiteId = orderNotification.remoteSiteId,
                type = NotificationModel.Kind.STORE_ORDER,
                meta = FormattableMeta(
                    ids = FormattableMeta.Ids(site = orderNotification.remoteSiteId, order = wooDrivenOrderId)
                )
            )
            val mockParser: NotificationsParser = mock {
                on { buildNotificationModelFromPayloadMap(any()) } doReturn wooDrivenModel
            }
            createNotificationMessageHandler(mockParser)

            notificationMessageHandler.onNewMessageReceived(payload)

            verify(dispatcher, atLeastOnce()).dispatch(any())
            verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
                stat = eq(AnalyticsEvent.PUSH_NOTIFICATION_RECEIVED),
                siteId = eq(orderNotification.remoteSiteId),
                notificationId = eq("${orderNotification.remoteSiteId}:order:$wooDrivenOrderId"),
                noteTypeTrackingValue = eq(WooNotificationType.NewOrder.trackingValue),
                source = eq(NotificationSource.WOO_DRIVEN)
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
    fun `given site registered in both systems, when wpcom notification received, then skip notification`() = runTest {
        whenever(registrationStatus.invoke(any()))
            .thenReturn(REGISTERED_BOTH)

        // WPCOM notifications have remoteNoteId > 0
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(wooLog).d(
            eq(WooLog.T.NOTIFICATIONS),
            eq("Skipping WPCOM notification, already registered with Woo Core")
        )
        verifyNoInteractions(dispatcher)
    }

    @Test
    fun `given site registered in both systems and user mismatch, when wpcom notification received, then keep legacy validation`() =
        runTest {
            whenever(registrationStatus.invoke(any()))
                .thenReturn(REGISTERED_BOTH)
            val payload = NotificationTestUtils.generateTestNewOrderNotificationPayload(userId = 67890)

            notificationMessageHandler.onNewMessageReceived(payload)

            verify(wooLog).e(
                eq(WooLog.T.NOTIFICATIONS),
                eq("WP.com userId found in the app doesn't match with the ID in the PN. Aborting.")
            )
            verifyNoInteractions(dispatcher)
        }

    @Test
    fun `given site registered only in Woo, when wpcom notification received, then skip notification`() = runTest {
        whenever(registrationStatus.invoke(any()))
            .thenReturn(REGISTERED_WOO_ONLY)

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(dispatcher, never()).dispatch(any())
    }

    @Test
    fun `given woo push registered and site is visible, when woo notification received, then process it`() = runTest {
        whenever(registrationStatus.invoke(any()))
            .thenReturn(REGISTERED_WOO_ONLY)
        createWooNotificationMessageHandler()

        notificationMessageHandler.onNewMessageReceived(wooNotificationPayload)

        verify(dispatcher, atLeastOnce()).dispatch(any())
    }

    @Test
    fun `given site registered in both systems, when woo notification received, then process it`() = runTest {
        whenever(registrationStatus.invoke(any()))
            .thenReturn(REGISTERED_BOTH)
        createWooNotificationMessageHandler()

        notificationMessageHandler.onNewMessageReceived(wooNotificationPayload)

        verify(dispatcher, atLeastOnce()).dispatch(any())
    }

    @Test
    fun `given user has access token and site is not marked as Woo registered, when woo notification received, then process it`() =
        runTest {
            createWooNotificationMessageHandler()

            notificationMessageHandler.onNewMessageReceived(wooNotificationPayload)

            verify(dispatcher, atLeastOnce()).dispatch(any())
        }

    @Test
    fun `given user is not logged in, when woo notification received, then process it`() = runTest {
        doReturn(false).whenever(accountStore).hasAccessToken()
        createWooNotificationMessageHandler()

        notificationMessageHandler.onNewMessageReceived(wooNotificationPayload)

        verify(dispatcher, atLeastOnce()).dispatch(any())
    }

    @Test
    fun `given site is hidden, when notification received, then skip it`() = runTest {
        createWooNotificationMessageHandler()
        whenever(getWooVisibleSites.invoke()).thenReturn(emptyList())

        notificationMessageHandler.onNewMessageReceived(wooNotificationPayload)

        verify(wooLog).w(
            eq(WooLog.T.NOTIFICATIONS),
            eq("Skipping notification, site ${orderNotification.remoteSiteId} is not visible")
        )
        verifyNoInteractions(dispatcher)
    }

    @Test
    fun `given wpcom notification site is hidden, when user has access token, then skip it`() = runTest {
        whenever(getWooVisibleSites.invoke()).thenReturn(emptyList())

        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        verify(wooLog).w(
            eq(WooLog.T.NOTIFICATIONS),
            eq("Skipping notification, site ${orderNotification.remoteSiteId} is not visible")
        )
        verifyNoInteractions(dispatcher)
    }

    @Test
    fun `given site is hidden and user has no access token, when woo notification received, then process it`() =
        runTest {
            doReturn(false).whenever(accountStore).hasAccessToken()
            createWooNotificationMessageHandler()
            whenever(getWooVisibleSites.invoke()).thenReturn(emptyList())

            notificationMessageHandler.onNewMessageReceived(wooNotificationPayload)

            verify(dispatcher, atLeastOnce()).dispatch(any())
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
            source = any(),
            analyticsId = any(),
            isGroupNotification = eq(false)
        )

        verify(notificationBuilder, never()).buildAndDisplayWooGroupNotification(any(), any(), any(), any(), any())

        // Second notification - simulate first notification being active
        val firstNotificationData = orderNotification.toActiveNotificationData(10000)
        doReturn(listOf(firstNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        notificationMessageHandler.onNewMessageReceived(reviewNotificationPayload)

        // verify that the contents for a new review notification is correct
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(reviewNotification),
            source = any(),
            analyticsId = any(),
            isGroupNotification = eq(true)
        )

        // verify that the contents for the group notification is correct
        val subject = resourceProvider.getString(R.string.new_notifications, 2)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = eq("${reviewNotification.noteMessage!!}\n${orderNotification.noteMessage!!}"),
            subject = eq(subject),
            notification = eq(reviewNotification),
            source = any(),
            analyticsId = any()
        )
    }

    private fun verifyGroupedNotificationDisplay(
        firstPayload: Map<String, String>,
        firstNotification: Notification,
        secondPayload: Map<String, String>,
        simulatedRemoteNoteId: Long
    ) {
        // First notification
        notificationMessageHandler.onNewMessageReceived(firstPayload)

        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = eq(firstNotification),
            source = any(),
            analyticsId = any(),
            isGroupNotification = eq(false)
        )

        // Simulate first notification being active before second arrives
        val firstNotificationData = firstNotification
            .copy(remoteNoteId = simulatedRemoteNoteId)
            .toActiveNotificationData(10000)
        doReturn(listOf(firstNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        // Second notification
        notificationMessageHandler.onNewMessageReceived(secondPayload)

        // Verify second notification is displayed as group notification
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = any(),
            source = any(),
            analyticsId = any(),
            isGroupNotification = eq(true)
        )

        // Verify group notification is displayed
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = any(),
            subject = any(),
            notification = any(),
            source = any(),
            analyticsId = any()
        )
    }

    @Test
    fun `when two new order notifications are received for the same store, then display correctly`() {
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_ORDER_NOTE_FULL_DATA_2
        )
        verifyGroupedNotificationDisplay(
            firstPayload = orderNotificationPayload,
            firstNotification = orderNotification,
            secondPayload = orderNotificationPayload2,
            simulatedRemoteNoteId = 999999L
        )
    }

    @Test
    fun `when two new review notifications are received for the same store, then display correctly`() {
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_REVIEW_NOTE_FULL_DATA_2
        )
        verifyGroupedNotificationDisplay(
            firstPayload = reviewNotificationPayload,
            firstNotification = reviewNotification,
            secondPayload = reviewNotificationPayload2,
            simulatedRemoteNoteId = 888888L
        )
    }

    @Test
    fun `when two new order notifications are received for different stores, then display correctly`() {
        val orderNotificationPayload2 = NotificationTestUtils.generateTestNewOrderNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_ORDER_NOTE_FULL_DATA_SITE_2
        )
        verifyGroupedNotificationDisplay(
            firstPayload = orderNotificationPayload,
            firstNotification = orderNotification,
            secondPayload = orderNotificationPayload2,
            simulatedRemoteNoteId = 777777L
        )
    }

    @Test
    fun `when two new review notifications are received for different stores, then display correctly`() {
        val reviewNotificationPayload2 = NotificationTestUtils.generateTestNewReviewNotificationPayload(
            userId = accountModel.userId,
            noteData = TEST_REVIEW_NOTE_FULL_DATA_SITE_2
        )
        verifyGroupedNotificationDisplay(
            firstPayload = reviewNotificationPayload,
            firstNotification = reviewNotification,
            secondPayload = reviewNotificationPayload2,
            simulatedRemoteNoteId = 666666L
        )
    }

    @Test
    fun `when more than 5 notifications are received for same store, then display correctly`() {
        // Simulate 5 existing notifications
        val existingNotifications = (0 until 5).map { index ->
            orderNotification
                .copy(
                    remoteNoteId = (100000 + index).toLong(),
                    noteMessage = "Message $index"
                )
                .toActiveNotificationData(id = 10000 + index)
        }
        doReturn(existingNotifications).whenever(notificationBuilder).getActiveNotifications()

        // 6th notification arrives
        notificationMessageHandler.onNewMessageReceived(orderNotificationPayload)

        // Verify notification is displayed as group notification
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooNotification(
            pushId = any(),
            notification = any(),
            source = any(),
            analyticsId = any(),
            isGroupNotification = eq(true)
        )

        // Verify group notification is displayed with correct count
        val subject = resourceProvider.getString(R.string.new_notifications, 6)
        verify(notificationBuilder, atLeastOnce()).buildAndDisplayWooGroupNotification(
            inboxMessage = any(),
            subject = eq(subject),
            notification = any(),
            source = any(),
            analyticsId = any()
        )
    }

    @Test
    fun `when notification is clicked, then mark new notification as tapped correctly`() {
        val mockNotificationData = orderNotification.toActiveNotificationData(10000)
        doReturn(listOf(mockNotificationData)).whenever(notificationBuilder).getActiveNotifications()

        notificationMessageHandler.markNotificationTapped(mockNotificationData.id)

        verify(notificationAnalyticsTracker, atLeastOnce()).trackNotificationAnalytics(
            stat = eq(AnalyticsEvent.PUSH_NOTIFICATION_TAPPED),
            siteId = eq(orderNotification.remoteSiteId),
            notificationId = eq(orderNotification.remoteNoteId.toString()),
            noteTypeTrackingValue = eq(orderNotification.noteType.trackingValue),
            source = eq(NotificationSource.WPCOM)
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
            notificationId = eq(orderNotification.remoteNoteId.toString()),
            noteTypeTrackingValue = eq(orderNotification.noteType.trackingValue),
            source = eq(NotificationSource.WPCOM)
        )
    }

    @Test
    fun `when tapped notification is the last child in its group, then remove child and summary`() {
        val childNotificationId = 10000
        val summaryNotificationId = orderNotification.getGroupPushId()
        doReturn(
            listOf(
                orderNotification.toActiveNotificationData(id = childNotificationId),
                orderNotification.toActiveNotificationData(id = summaryNotificationId, isGroupSummary = true)
            )
        ).whenever(notificationBuilder).getActiveNotifications()

        notificationMessageHandler.removeTappedNotificationAndSummaryIfNeeded(
            localPushId = childNotificationId,
            notification = orderNotification
        )

        verify(notificationBuilder).cancelNotification(childNotificationId)
        verify(notificationBuilder).cancelNotification(summaryNotificationId)
    }

    @Test
    fun `when another child in the same group remains, then keep summary notification`() {
        val tappedNotificationId = 10000
        val remainingChildId = 10001
        val summaryNotificationId = orderNotification.getGroupPushId()
        doReturn(
            listOf(
                orderNotification.toActiveNotificationData(id = tappedNotificationId),
                orderNotification.toActiveNotificationData(id = remainingChildId),
                orderNotification.toActiveNotificationData(id = summaryNotificationId, isGroupSummary = true)
            )
        ).whenever(notificationBuilder).getActiveNotifications()

        notificationMessageHandler.removeTappedNotificationAndSummaryIfNeeded(
            localPushId = tappedNotificationId,
            notification = orderNotification
        )

        verify(notificationBuilder).cancelNotification(tappedNotificationId)
        verify(notificationBuilder, never()).cancelNotification(summaryNotificationId)
    }

    @Test
    fun `when only notifications from another store remain, then remove tapped group summary`() {
        val tappedNotificationId = 10000
        val otherStoreChildId = 10001
        val summaryNotificationId = orderNotification.getGroupPushId()
        doReturn(
            listOf(
                orderNotification.toActiveNotificationData(id = tappedNotificationId),
                orderNotificationSite2.toActiveNotificationData(id = otherStoreChildId),
                orderNotification.toActiveNotificationData(id = summaryNotificationId, isGroupSummary = true)
            )
        ).whenever(notificationBuilder).getActiveNotifications()

        notificationMessageHandler.removeTappedNotificationAndSummaryIfNeeded(
            localPushId = tappedNotificationId,
            notification = orderNotification
        )

        verify(notificationBuilder).cancelNotification(tappedNotificationId)
        verify(notificationBuilder).cancelNotification(summaryNotificationId)
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

    private fun Notification.toActiveNotificationData(
        id: Int,
        isGroupSummary: Boolean = false
    ) = ActiveNotificationData(
        id = id,
        remoteNoteId = remoteNoteId,
        remoteSiteId = remoteSiteId,
        channelType = channelType.name,
        noteMessage = noteMessage,
        noteTypeTrackingValue = noteType.trackingValue,
        source = NotificationSource.WPCOM,
        analyticsId = remoteNoteId.takeIf { it != 0L }?.toString(),
        isGroupSummary = isGroupSummary
    )
}
