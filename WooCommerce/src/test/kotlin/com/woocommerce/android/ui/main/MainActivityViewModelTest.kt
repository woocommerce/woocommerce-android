package com.woocommerce.android.ui.main

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.REVIEW_OPEN
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.FeatureAnnouncement
import com.woocommerce.android.model.FeatureAnnouncementItem
import com.woocommerce.android.notifications.NotificationChannelType
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.notifications.WooNotificationType
import com.woocommerce.android.notifications.push.NotificationMessageHandler
import com.woocommerce.android.notifications.push.NotificationTestUtils
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.Hidden
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.UnseenReviews
import com.woocommerce.android.ui.main.MainActivityViewModel.RestartActivityForPushNotification
import com.woocommerce.android.ui.main.MainActivityViewModel.ShortcutOpenOrderCreation
import com.woocommerce.android.ui.main.MainActivityViewModel.ShortcutOpenPayments
import com.woocommerce.android.ui.main.MainActivityViewModel.ShowFeatureAnnouncement
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewMyStoreStats
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewTapToPay
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewUrlInWebView
import com.woocommerce.android.ui.moremenu.MoreMenuNewFeatureHandler
import com.woocommerce.android.ui.whatsnew.FeatureAnnouncementRepository
import com.woocommerce.android.util.BuildConfigWrapper
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore

@ExperimentalCoroutinesApi
class MainActivityViewModelTest : BaseUnitTest() {
    companion object {
        private const val TEST_REMOTE_SITE_ID_1 = 1023456789L
        private const val TEST_REMOTE_SITE_ID_2 = 9876543210L

        private const val TEST_NEW_ORDER_REMOTE_NOTE_ID = 5473011602
        private const val TEST_NEW_ORDER_ID_1 = 1915L
        private const val TEST_NEW_ORDER_ID_2 = 1915L

        private const val TEST_NEW_REVIEW_REMOTE_NOTE_ID = 5604993863
        private const val TEST_NEW_REVIEW_ID_1 = 4418L
        private const val TEST_NEW_REVIEW_ID_2 = 4418L
    }

    private lateinit var viewModel: MainActivityViewModel
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val selectedSite: SelectedSite = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val siteStore: SiteStore = mock()
    private val siteModel: SiteModel = SiteModel().apply {
        id = 1
        siteId = TEST_REMOTE_SITE_ID_1
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

    private val featureAnnouncementRepository: FeatureAnnouncementRepository = mock()
    private val buildConfigWrapper: BuildConfigWrapper = mock()
    private val prefs: AppPrefs = mock()
    private val moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler = mock()
    private val unseenReviewsCountHandler: UnseenReviewsCountHandler = mock {
        on { observeUnseenCount() } doReturn MutableStateFlow(1)
    }

    private val testAnnouncement = FeatureAnnouncement(
        appVersionName = "14.2",
        announcementVersion = 1337,
        minimumAppVersion = "14.2",
        maximumAppVersion = "14.3",
        appVersionTargets = listOf("alpha-centauri-1", "alpha-centauri-2"),
        detailsUrl = "https://woocommerce.com/",
        isLocalized = true,
        features = listOf(
            FeatureAnnouncementItem(
                title = "Super Publishing",
                subtitle = "Super Publishing is here! Publish using the power of your mind.",
                iconBase64 = "",
                iconUrl = "https://s0.wordpress.com/i/store/mobile/plans-personal.png"
            ),
            FeatureAnnouncementItem(
                title = "Amazing Feature",
                subtitle = "That's right! They are right in the app! They require pets right now.",
                iconBase64 = "",
                iconUrl = "https://s0.wordpress.com/i/store/mobile/plans-premium.png"
            ),
            FeatureAnnouncementItem(
                title = "Third feature",
                subtitle = "Sorry we forgot to include an image here!",
                iconBase64 = "",
                iconUrl = ""
            )
        )
    )

    private val resolveAppLink: ResolveAppLink = mock()

    @Before
    fun setup() {
        createViewModel()

        clearInvocations(
            viewModel,
            siteStore,
            selectedSite,
            notificationMessageHandler
        )

        doReturn(siteModel).whenever(siteStore).getSiteBySiteId(any())
        doReturn(siteModel).whenever(selectedSite).get()
    }

    @Test
    fun `when a blank notification is clicked, then the my store tab is opened`() {
        val localPushId = 1000
        var event: ViewMyStoreStats? = null
        viewModel.event.observeForever {
            if (it is ViewMyStoreStats) event = it
        }

        viewModel.handleIncomingNotification(localPushId, null)
        assertThat(event).isEqualTo(ViewMyStoreStats)
    }

    @Test
    fun `when a new order notification is clicked, then the order detail screen for that order is opened`() {
        val localPushId = 1000
        var event: ViewOrderDetail? = null
        viewModel.event.observeForever {
            if (it is ViewOrderDetail) event = it
        }

        viewModel.handleIncomingNotification(localPushId, testOrderNotification)

        verify(notificationMessageHandler, atLeastOnce()).markNotificationTapped(eq(testOrderNotification.remoteNoteId))
        verify(notificationMessageHandler, atLeastOnce())
            .removeNotificationByNotificationIdFromSystemsBar(eq(localPushId))
        assertThat(event).isEqualTo(
            ViewOrderDetail(
                testOrderNotification.uniqueId,
                testOrderNotification.remoteNoteId
            )
        )
    }

    @Test
    fun `when a new order notification for non existent site is clicked, then the my store tab is opened`() {
        doReturn(null).whenever(siteStore).getSiteBySiteId(any())

        val localPushId = 1000
        var event: ViewOrderList? = null
        viewModel.event.observeForever {
            if (it is ViewOrderList) event = it
        }

        viewModel.handleIncomingNotification(localPushId, testOrderNotification)

        verify(notificationMessageHandler, atLeastOnce()).markNotificationTapped(eq(testOrderNotification.remoteNoteId))
        verify(notificationMessageHandler, atLeastOnce())
            .removeNotificationByNotificationIdFromSystemsBar(eq(localPushId))
        assertThat(event).isEqualTo(ViewOrderList)
    }

    @Test
    fun `when a new review notification is clicked, then the review detail screen for that review is opened`() {
        val localPushId = 1001
        var event: ViewReviewDetail? = null
        viewModel.event.observeForever {
            if (it is ViewReviewDetail) event = it
        }

        viewModel.handleIncomingNotification(localPushId, testReviewNotification)

        verify(notificationMessageHandler, atLeastOnce())
            .markNotificationTapped(eq(testReviewNotification.remoteNoteId))
        verify(notificationMessageHandler, atLeastOnce())
            .removeNotificationByNotificationIdFromSystemsBar(eq(localPushId))
        assertThat(event).isEqualTo(ViewReviewDetail(testReviewNotification.uniqueId))
    }

    @Test
    fun `when a new review notification is clicked, then review open even tracked`() {
        val localPushId = 1001

        viewModel.handleIncomingNotification(localPushId, testReviewNotification)

        verify(analyticsTrackerWrapper).track(REVIEW_OPEN)
    }

    @Test
    fun `when multiple order notifications for the same store is clicked, then the order list screen is opened`() {
        val groupOrderPushId = testOrderNotification.getGroupPushId()
        var event: ViewOrderList? = null
        viewModel.event.observeForever {
            if (it is ViewOrderList) event = it
        }

        viewModel.handleIncomingNotification(groupOrderPushId, testOrderNotification)

        verify(selectedSite, never()).set(any())
        verify(notificationMessageHandler, atLeastOnce()).markNotificationsOfTypeTapped(
            eq(testOrderNotification.channelType)
        )
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationsOfTypeFromSystemsBar(
            eq(testOrderNotification.channelType),
            eq(testOrderNotification.remoteSiteId)
        )
        assertThat(event).isEqualTo(ViewOrderList)
    }

    @Test
    fun `when multiple review notifications for the same store is clicked, then the review list screen is opened`() {
        val reviewPushId = testReviewNotification.getGroupPushId()
        var event: ViewReviewList? = null
        viewModel.event.observeForever {
            if (it is ViewReviewList) event = it
        }

        viewModel.handleIncomingNotification(reviewPushId, testReviewNotification)

        verify(selectedSite, never()).set(any())
        verify(notificationMessageHandler, atLeastOnce()).markNotificationsOfTypeTapped(
            eq(testReviewNotification.channelType)
        )
        verify(notificationMessageHandler, atLeastOnce()).removeNotificationsOfTypeFromSystemsBar(
            eq(testReviewNotification.channelType),
            eq(testReviewNotification.remoteSiteId)
        )
        assertThat(event).isEqualTo(ViewReviewList)
    }

    @Test
    fun `when order notifications for a second store is clicked then switch to the this store and restart activity`() {
        val orderNotification2 = testOrderNotification.copy(
            remoteSiteId = TEST_REMOTE_SITE_ID_2,
            uniqueId = TEST_NEW_ORDER_ID_2
        )
        val groupOrderPushId = orderNotification2.getGroupPushId()

        viewModel.handleIncomingNotification(groupOrderPushId, orderNotification2)

        verify(selectedSite, atLeastOnce()).set(any())
        assertThat(viewModel.event.value)
            .isEqualTo(RestartActivityForPushNotification(groupOrderPushId, orderNotification2))
    }

    @Test
    fun `when review notifications for second store is clicked then switch to the this store and restart activity`() {
        val reviewNotification2 = testReviewNotification.copy(
            remoteSiteId = TEST_REMOTE_SITE_ID_2,
            uniqueId = TEST_NEW_REVIEW_ID_2
        )
        val reviewPushId = reviewNotification2.getGroupPushId()

        viewModel.handleIncomingNotification(reviewPushId, reviewNotification2)

        verify(selectedSite, atLeastOnce()).set(any())
        assertThat(viewModel.event.value).isEqualTo(
            RestartActivityForPushNotification(
                reviewPushId,
                reviewNotification2
            )
        )
    }

    @Test
    fun `when notification of non existing store is clicked, then show default screen`() {
        doReturn(null).whenever(siteStore).getSiteBySiteId(any())
        val notification = testOrderNotification.copy(remoteSiteId = TEST_REMOTE_SITE_ID_2)

        viewModel.handleIncomingNotification(1000, notification)

        assertThat(viewModel.event.value).isEqualTo(ViewMyStoreStats)
    }

    @Test
    fun `given existing announcement cache, when app is upgraded and announcement is valid, then show announcement`() =
        testBlocking {
            doReturn(testAnnouncement).whenever(featureAnnouncementRepository).getLatestFeatureAnnouncement(true)
            doReturn("14.0").whenever(prefs).getLastVersionWithAnnouncement()
            doReturn("14.2").whenever(buildConfigWrapper).versionName

            viewModel.showFeatureAnnouncementIfNeeded()
            assertThat(viewModel.event.value).isEqualTo(ShowFeatureAnnouncement(testAnnouncement))
        }

    @Test
    fun `given existing announcement cache, when app is upgraded and announcement is valid, track event is tracked`() =
        testBlocking {
            doReturn(testAnnouncement).whenever(featureAnnouncementRepository).getLatestFeatureAnnouncement(true)
            doReturn("14.0").whenever(prefs).getLastVersionWithAnnouncement()
            doReturn("14.2").whenever(buildConfigWrapper).versionName

            viewModel.showFeatureAnnouncementIfNeeded()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.FEATURE_ANNOUNCEMENT_SHOWN,
                mapOf(
                    AnalyticsTracker.KEY_ANNOUNCEMENT_VIEW_SOURCE to
                        AnalyticsTracker.VALUE_ANNOUNCEMENT_SOURCE_UPGRADE
                )
            )
        }

    @Test
    fun `given zero unseen reviews and no new features, when listening badge state, then hidden returned`() =
        testBlocking {
            // GIVEN
            whenever(unseenReviewsCountHandler.observeUnseenCount()).thenReturn(flowOf(0))
            whenever(moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable).thenReturn(MutableStateFlow(emptyList()))
            createViewModel()

            // WHEN
            viewModel.moreMenuBadgeState.observeForever { }

            // THEN
            assertThat(viewModel.moreMenuBadgeState.value).isEqualTo(Hidden)
        }

    @Test
    fun `given unseen reviews and no new features, when listening badge state, then unseen reviews returned`() =
        testBlocking {
            // GIVEN
            whenever(unseenReviewsCountHandler.observeUnseenCount()).thenReturn(flowOf(1))
            whenever(moreMenuNewFeatureHandler.moreMenuNewFeaturesAvailable).thenReturn(
                MutableStateFlow(emptyList())
            )
            createViewModel()

            // WHEN
            viewModel.moreMenuBadgeState.observeForever {}

            // THEN
            assertThat(viewModel.moreMenuBadgeState.value).isEqualTo(UnseenReviews(1))
        }

    @Test
    fun `given tap to pay url, when app opened, then trigger ViewTapToPay event`() {
        testBlocking {
            // GIVEN
            whenever(resolveAppLink.invoke(any())).thenReturn(ResolveAppLink.Action.ViewTapToPay)
            createViewModel()

            // WHEN
            viewModel.handleIncomingAppLink(mock())

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ViewTapToPay::class.java)
        }
    }

    @Test
    fun `given view url in web view, when app opened, then trigger ViewUrlInWebView event`() {
        testBlocking {
            // GIVEN
            val url = "https://woocommerce.com"
            whenever(resolveAppLink.invoke(any())).thenReturn(ResolveAppLink.Action.ViewUrlInWebView(url))
            createViewModel()

            // WHEN
            viewModel.handleIncomingAppLink(mock())

            // THEN
            assertThat(viewModel.event.value).isEqualTo(ViewUrlInWebView(url))
        }
    }

    @Test
    fun `given payments shortcut, when app opened, then trigger ViewPayments event`() {
        testBlocking {
            // GIVEN
            whenever(selectedSite.exists()).thenReturn(true)
            createViewModel()

            // WHEN
            viewModel.handleShortcutAction("com.woocommerce.android.payments")

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ShortcutOpenPayments::class.java)
        }
    }

    @Test
    fun `given order creation shortcut, when app opened, then trigger OpenOrderCreation event`() {
        testBlocking {
            // GIVEN
            whenever(selectedSite.exists()).thenReturn(true)
            createViewModel()

            // WHEN
            viewModel.handleShortcutAction("com.woocommerce.android.ordercreation")

            // THEN
            assertThat(viewModel.event.value).isInstanceOf(ShortcutOpenOrderCreation::class.java)
        }
    }

    @Test
    fun `given irrelevant shortcut, when app opened, then do not trigger any event`() {
        testBlocking {
            // GIVEN
            whenever(selectedSite.exists()).thenReturn(true)
            createViewModel()

            // WHEN
            viewModel.handleShortcutAction("com.woocommerce.android.irrelevant")

            // THEN
            assertThat(viewModel.event.value).isNull()
        }
    }

    @Test
    fun `given payments shortcut, when app opened, then track payments opened event`() {
        testBlocking {
            // GIVEN
            whenever(selectedSite.exists()).thenReturn(true)
            createViewModel()

            // WHEN
            viewModel.handleShortcutAction("com.woocommerce.android.payments")

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.SHORTCUT_PAYMENTS_TAPPED
            )
        }
    }

    @Test
    fun `given order creation shortcut, when app opened, then track orders add new event`() {
        testBlocking {
            // GIVEN
            whenever(selectedSite.exists()).thenReturn(true)
            createViewModel()

            // WHEN
            viewModel.handleShortcutAction("com.woocommerce.android.ordercreation")

            // THEN
            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.SHORTCUT_ORDERS_ADD_NEW
            )
        }
    }

    @Test
    fun `given no selected site, when shortcut used, then skip it`() {
        testBlocking {
            // GIVEN
            whenever(selectedSite.exists()).thenReturn(false)
            createViewModel()

            // WHEN
            viewModel.handleShortcutAction("com.woocommerce.android.ordercreation")

            // THEN
            assertThat(viewModel.event.value).isNull()
        }
    }

    @Test
    fun `given image uris when app opened, then a product creation is triggered using the images`() = testBlocking {
        // GIVEN
        createViewModel()

        var event: MainActivityViewModel.CreateNewProductUsingImages? = null
        viewModel.event.observeForever {
            if (it is MainActivityViewModel.CreateNewProductUsingImages) event = it
        }

        val uri = "content://com.woocommerce.android.fileprovider/woocommerce/woocommerce_1.jpg"

        // WHEN
        viewModel.handleIncomingImages(listOf(uri))

        // THEN
        assertThat(event).isEqualTo(MainActivityViewModel.CreateNewProductUsingImages(listOf(uri)))
    }

    private fun createViewModel() {
        viewModel = spy(
            MainActivityViewModel(
                savedState = savedStateHandle,
                siteStore = siteStore,
                selectedSite = selectedSite,
                notificationHandler = notificationMessageHandler,
                featureAnnouncementRepository = featureAnnouncementRepository,
                buildConfigWrapper = buildConfigWrapper,
                prefs = prefs,
                analyticsTrackerWrapper = analyticsTrackerWrapper,
                resolveAppLink = resolveAppLink,
                privacyRepository = mock(),
                moreMenuNewFeatureHandler = moreMenuNewFeatureHandler,
                unseenReviewsCountHandler = unseenReviewsCountHandler,
                determineTrialStatusBarState = mock {
                    onBlocking { invoke(any()) } doReturn emptyFlow()
                },
            )
        )
    }
}
