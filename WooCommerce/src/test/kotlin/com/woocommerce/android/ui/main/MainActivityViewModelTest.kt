package com.woocommerce.android.ui.main

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.push.NotificationMessageHandler
import com.woocommerce.android.push.NotificationTestUtils
import com.woocommerce.android.push.WooNotificationType
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewZendeskTickets
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewMyStoreStats
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewList
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MainActivityViewModelTest : BaseUnitTest() {
    companion object {
        private const val TEST_NEW_ORDER_REMOTE_NOTE_ID = 5473011602
        private const val TEST_NEW_ORDER_ID_1 = 1915L

        private const val TEST_NEW_REVIEW_REMOTE_NOTE_ID = 5604993863
        private const val TEST_NEW_REVIEW_ID_1 = 4418L

        private const val TEST_ZENDESK_PUSH_NOTIFICATION_ID = 1999999999
    }

    private lateinit var viewModel: MainActivityViewModel
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    private val siteStore: SiteStore = mock()
    private val siteModel: SiteModel = SiteModel().apply {
        id = 1
        siteId = 123456789
    }

    private val notificationMessageHandler: NotificationMessageHandler = mock()
    private val testOrderNotification = NotificationTestUtils.generateTestNotification(
        remoteNoteId = TEST_NEW_ORDER_REMOTE_NOTE_ID,
        remoteSiteId = siteModel.siteId,
        uniqueId = TEST_NEW_ORDER_ID_1,
        channelType = NotificationChannelType.NEW_ORDER,
        noteType = WooNotificationType.NEW_ORDER
    )

    private val testReviewNotification = NotificationTestUtils.generateTestNotification(
        remoteNoteId = TEST_NEW_REVIEW_REMOTE_NOTE_ID,
        remoteSiteId = siteModel.siteId,
        uniqueId = TEST_NEW_REVIEW_ID_1,
        channelType = NotificationChannelType.REVIEW,
        noteType = WooNotificationType.PRODUCT_REVIEW
    )

    private val testZendeskNotification = NotificationTestUtils.generateTestNotification(
        noteId = TEST_ZENDESK_PUSH_NOTIFICATION_ID,
        remoteNoteId = TEST_ZENDESK_PUSH_NOTIFICATION_ID.toLong(),
        remoteSiteId = siteModel.siteId,
        uniqueId = 0,
        channelType = NotificationChannelType.OTHER,
        noteType = WooNotificationType.ZENDESK
    )

    @Before
    fun setup() {
        viewModel = spy(
            MainActivityViewModel(
                savedStateHandle,
                siteStore,
                notificationMessageHandler
            )
        )

        clearInvocations(
            viewModel,
            siteStore,
            notificationMessageHandler
        )

        doReturn(siteModel).whenever(siteStore).getSiteBySiteId(any())
    }

    @Test
    fun `blank notification to open my store`() {
        val localPushId = 1000
        var event: ViewMyStoreStats? = null
        viewModel.event.observeForever {
            if (it is ViewMyStoreStats) event = it
        }

        viewModel.handleIncomingNotification(localPushId, null)
        assertThat(event).isEqualTo(ViewMyStoreStats)
    }

    @Test
    fun `incoming new order notification to open order detail`() {
        val localPushId = 1000
        var event: ViewOrderDetail? = null
        viewModel.event.observeForever {
            if (it is ViewOrderDetail) event = it
        }

        viewModel.handleIncomingNotification(localPushId, testOrderNotification)

        verify(notificationMessageHandler, atLeastOnce()).markNotificationTapped(eq(testOrderNotification.remoteNoteId))
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationByPushIdFromSystemsBar(eq(localPushId))
        assertThat(event).isEqualTo(
            ViewOrderDetail(
                testOrderNotification.uniqueId,
                siteModel.id,
                testOrderNotification.remoteNoteId
            )
        )
    }

    @Test
    fun `incoming new order notification for non existent site to open my store`() {
        doReturn(null).whenever(siteStore).getSiteBySiteId(any())

        val localPushId = 1000
        var event: ViewOrderList? = null
        viewModel.event.observeForever {
            if (it is ViewOrderList) event = it
        }

        viewModel.handleIncomingNotification(localPushId, testOrderNotification)

        verify(notificationMessageHandler, atLeastOnce()).markNotificationTapped(eq(testOrderNotification.remoteNoteId))
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationByPushIdFromSystemsBar(eq(localPushId))
        assertThat(event).isEqualTo(ViewOrderList)
    }

    @Test
    fun `incoming new review notification to open review detail`() {
        val localPushId = 1001
        var event: ViewReviewDetail? = null
        viewModel.event.observeForever {
            if (it is ViewReviewDetail) event = it
        }

        viewModel.handleIncomingNotification(localPushId, testReviewNotification)

        verify(notificationMessageHandler, atLeastOnce())
            .markNotificationTapped(eq(testReviewNotification.remoteNoteId))
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationByPushIdFromSystemsBar(eq(localPushId))
        assertThat(event).isEqualTo(ViewReviewDetail(testReviewNotification.uniqueId))
    }

    @Test
    fun `incoming new zendesk notification to open zendesk tickets`() {
        var event1: ViewZendeskTickets? = null
        viewModel.event.observeForever {
            if (it is ViewZendeskTickets) event1 = it
        }

        viewModel.handleIncomingNotification(TEST_ZENDESK_PUSH_NOTIFICATION_ID, testZendeskNotification)

        verify(notificationMessageHandler, atLeastOnce()).markNotificationTapped(
            eq(testZendeskNotification.remoteNoteId)
        )
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationByPushIdFromSystemsBar(
            eq(TEST_ZENDESK_PUSH_NOTIFICATION_ID)
        )
        assertThat(event1).isEqualTo(ViewZendeskTickets)
    }

    @Test
    fun `incoming multiple order notifications to open order list`() {
        val orderPushId = 30001
        var event: ViewOrderList? = null
        viewModel.event.observeForever {
            if (it is ViewOrderList) event = it
        }

        viewModel.handleIncomingNotification(orderPushId, testOrderNotification)
        verify(notificationMessageHandler, atLeastOnce()).markNotificationsOfTypeTapped(
            eq(testOrderNotification.channelType)
        )
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationsOfTypeFromSystemsBar(
            eq(testOrderNotification.channelType)
        )
        assertThat(event).isEqualTo(ViewOrderList)
    }

    @Test
    fun `incoming multiple review notifications to open review list`() {
        val reviewPushId = 30002
        var event: ViewReviewList? = null
        viewModel.event.observeForever {
            if (it is ViewReviewList) event = it
        }

        viewModel.handleIncomingNotification(reviewPushId, testReviewNotification)
        verify(notificationMessageHandler, atLeastOnce()).markNotificationsOfTypeTapped(
            eq(testReviewNotification.channelType)
        )
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationsOfTypeFromSystemsBar(
            eq(testReviewNotification.channelType)
        )
        assertThat(event).isEqualTo(ViewReviewList)
    }

    @Test
    fun `incoming multiple zendesk notifications to open my store`() {
        val localPushId = 30003
        var event: ViewMyStoreStats? = null
        viewModel.event.observeForever {
            if (it is ViewMyStoreStats) event = it
        }

        viewModel.handleIncomingNotification(localPushId, testZendeskNotification)
        verify(notificationMessageHandler, atLeastOnce()).markNotificationsOfTypeTapped(
            eq(testZendeskNotification.channelType)
        )
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationsOfTypeFromSystemsBar(
            eq(testZendeskNotification.channelType)
        )
        assertThat(event).isEqualTo(ViewMyStoreStats)
    }
}
