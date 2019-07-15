package com.woocommerce.android.ui.dashboard

import com.nhaarman.mockito_kotlin.KArgumentCaptor
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.spy
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_ORDER_STATS_V4
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_TOP_EARNERS_STATS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_VISITOR_STATS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsV4Payload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsV4Changed
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DashboardPresenterTest {
    private val dashboardView: DashboardContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val wcStatsStore: WCStatsStore = mock()
    private val wcOrderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()

    private lateinit var presenter: DashboardPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        presenter = spy(DashboardPresenter(
                dispatcher, wooCommerceStore, wcStatsStore, wcOrderStore, selectedSite, networkStatus))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Requests order stats data correctly`() {
        presenter.takeView(dashboardView)
        presenter.loadStats(StatsGranularity.DAYS)

        // note that we expect two dispatches because there's one to get stats and another to get visitors
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        assertEquals(FETCH_ORDER_STATS_V4, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchOrderStatsV4Payload
        assertEquals(StatsGranularity.DAYS, payload.granularity)
    }

    @Test
    fun `Handles stats OnChanged result correctly`() {
        presenter.takeView(dashboardView)

        // Simulate OnChanged event from FluxC
        val onChanged = OnWCStatsV4Changed(
                1, granularity = DAYS, startDate = "2019-07-15", endDate = "2019-07-15"
        )
        onChanged.causeOfChange = FETCH_ORDER_STATS_V4
        presenter.onWCStatsV4Changed(onChanged)

        verify(dashboardView).showStats(any(), any(), eq(StatsGranularity.DAYS))
    }

    @Test
    fun `Handles stats OnChanged error result correctly`() {
        presenter.takeView(dashboardView)

        // Simulate OnChanged event from FluxC
        val onChanged = OnWCStatsV4Changed(1, granularity = StatsGranularity.DAYS).apply {
            causeOfChange = FETCH_ORDER_STATS_V4
            error = OrderStatsError(OrderStatsErrorType.INVALID_PARAM)
        }
        presenter.onWCStatsV4Changed(onChanged)
        verify(dashboardView, times(1)).showStatsError(StatsGranularity.DAYS)
    }

    @Test
    fun `Requests orders to fulfill count correctly`() {
        presenter.takeView(dashboardView)
        presenter.fetchUnfilledOrderCount(forced = true)

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(WCOrderAction.FETCH_ORDERS_COUNT, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchOrdersCountPayload
        assertEquals(CoreOrderStatus.PROCESSING.value, payload.statusFilter)
    }

    @Test
    fun `Displays orders card correctly`() {
        val totalOrders = 25
        val filter = CoreOrderStatus.PROCESSING.value
        presenter.takeView(dashboardView)
        presenter.fetchUnfilledOrderCount(forced = true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        presenter.onOrderChanged(OnOrderChanged(totalOrders, filter).apply {
            causeOfChange = FETCH_ORDERS_COUNT
            canLoadMore = true
        })

        verify(dashboardView).showUnfilledOrdersCard(totalOrders)
    }

    @Test
    fun `Hides orders card correctly`() {
        val totalOrders = 0
        val filter = CoreOrderStatus.PROCESSING.value
        presenter.takeView(dashboardView)
        presenter.fetchUnfilledOrderCount(forced = true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        presenter.onOrderChanged(OnOrderChanged(totalOrders, filter).apply {
            causeOfChange = FETCH_ORDERS_COUNT
        })

        verify(dashboardView, times(1)).hideUnfilledOrdersCard()
    }

    @Test
    fun `Hides orders card on error correctly`() {
        val totalOrders = 25
        val filter = CoreOrderStatus.PROCESSING.value
        presenter.takeView(dashboardView)
        presenter.fetchUnfilledOrderCount(forced = true)
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        presenter.onOrderChanged(OnOrderChanged(totalOrders, filter).apply {
            causeOfChange = FETCH_ORDERS_COUNT
            error = OrderError()
        })

        verify(dashboardView, times(1)).hideUnfilledOrdersCard()
    }

    @Test
    fun `Handles FETCH-ORDERS order event correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: FETCH-ORDERS - Dashboard should refresh
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDERS })
        verify(dashboardView, times(0)).showUnfilledOrdersCard(any())
        verify(dashboardView, times(0)).hideUnfilledOrdersCard()
        verify(dashboardView, times(0)).refreshDashboard(forced = any())
    }

    @Test
    fun `Handles UPDATE-ORDER-STATUS order event correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: UPDATE-ORDER-STATUS - Dashboard should refresh
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(dashboardView, times(0)).showUnfilledOrdersCard(any())
        verify(dashboardView, times(0)).hideUnfilledOrdersCard()
        verify(dashboardView, times(0)).refreshDashboard(forced = any())
    }

    @Test
    fun `Handles FETCH-ORDER-NOTES order event correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: FETCH-ORDER-NOTES - Dashboard should ignore
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDER_NOTES })
        verify(dashboardView, times(0)).showUnfilledOrdersCard(any())
        verify(dashboardView, times(0)).hideUnfilledOrdersCard()
        verify(dashboardView, times(0)).refreshDashboard(forced = true)
    }

    @Test
    fun `Handles FETCH-ORDERS order event with error correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: FETCH-ORDERS w/error - Dashboard should ignore
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_ORDERS
            error = OrderError()
        })
        verify(dashboardView, times(0)).showUnfilledOrdersCard(any())
        verify(dashboardView, times(0)).hideUnfilledOrdersCard()
        verify(dashboardView, times(0)).refreshDashboard(forced = any())
    }

    @Test
    fun `Refreshes dashboard on network connected event if needed`() {
        presenter.takeView(dashboardView)
        doReturn(true).whenever(dashboardView).isRefreshPending

        // Simulate the network connected event
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(dashboardView, times(1)).refreshDashboard(forced = any())
    }

    @Test
    fun `Does not refresh dashboard on network connected event if not needed`() {
        presenter.takeView(dashboardView)
        doReturn(false).whenever(dashboardView).isRefreshPending

        // Simulate the network connected event
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(dashboardView, times(0)).refreshDashboard(forced = any())
    }

    @Test
    fun `Ignores network disconnected event correctly`() {
        presenter.takeView(dashboardView)

        // Simulate the network disconnected event
        presenter.onEventMainThread(ConnectionChangeEvent(false))
        verify(dashboardView, times(0)).refreshDashboard(forced = any())
    }

    @Test
    fun `Requests top earners stats data correctly - forced`() {
        presenter.takeView(dashboardView)

        presenter.loadTopEarnerStats(StatsGranularity.DAYS, forced = true)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(FETCH_TOP_EARNERS_STATS, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchTopEarnersStatsPayload
        assertEquals(StatsGranularity.DAYS, payload.granularity)
        assertTrue(payload.forced)
    }

    @Test
    fun `Requests top earners stats data correctly - not forced`() {
        presenter.takeView(dashboardView)

        presenter.loadTopEarnerStats(StatsGranularity.DAYS, forced = false)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(FETCH_TOP_EARNERS_STATS, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchTopEarnersStatsPayload
        assertEquals(StatsGranularity.DAYS, payload.granularity)
    }

    @Test
    fun `Handles FETCH_TOP_EARNERS_STATS event correctly`() {
        presenter.takeView(dashboardView)

        val topEarners = ArrayList<WCTopEarnerModel>()
        topEarners.add(WCTopEarnerModel())
        presenter.onWCTopEarnersChanged(OnWCTopEarnersChanged(topEarners, StatsGranularity.DAYS).apply {
            causeOfChange = FETCH_TOP_EARNERS_STATS
        })
        verify(dashboardView, times(1)).showTopEarners(topEarners, StatsGranularity.DAYS)
    }

    @Test
    fun `Handles FETCH_TOP_EARNERS_STATS error event correctly`() {
        presenter.takeView(dashboardView)

        presenter.onWCTopEarnersChanged(OnWCTopEarnersChanged(emptyList(), StatsGranularity.DAYS).apply {
            causeOfChange = FETCH_TOP_EARNERS_STATS
            error = OrderStatsError(OrderStatsErrorType.INVALID_PARAM)
        })
        verify(dashboardView, times(1)).showTopEarnersError(StatsGranularity.DAYS)
    }

    @Test
    fun `Handles FETCH_HAS_ORDERS when there aren't any orders`() {
        presenter.takeView(dashboardView)
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_HAS_ORDERS })
        verify(dashboardView, times(1)).showEmptyView(true)
    }

    @Test
    fun `Handles FETCH_HAS_ORDERS when there are orders`() {
        presenter.takeView(dashboardView)
        presenter.onOrderChanged(OnOrderChanged(1).apply { causeOfChange = FETCH_HAS_ORDERS })
        verify(dashboardView, times(1)).showEmptyView(false)
    }

    @Test
    fun `Handles FETCH_VISITOR_STATS event correctly`() {
        presenter.takeView(dashboardView)

        val onChanged = OnWCStatsChanged(1, granularity = StatsGranularity.DAYS)
        onChanged.causeOfChange = FETCH_VISITOR_STATS

        presenter.onWCStatsChanged(onChanged)
        verify(dashboardView, times(1)).showVisitorStats(1, StatsGranularity.DAYS)
    }

    @Test
    fun `Handles FETCH_VISITOR_STATS error event correctly`() {
        presenter.takeView(dashboardView)

        val onChanged = OnWCStatsChanged(1, granularity = DAYS)
        onChanged.causeOfChange = FETCH_VISITOR_STATS
        onChanged.error = OrderStatsError(OrderStatsErrorType.INVALID_PARAM)

        presenter.onWCStatsChanged(onChanged)
        verify(dashboardView, times(1)).showVisitorStatsError(StatsGranularity.DAYS)
    }

    @Test
    fun `Show and hide stats skeleton correctly`() {
        presenter.takeView(dashboardView)
        presenter.loadStats(StatsGranularity.DAYS, forced = true)
        verify(dashboardView, times(1)).showChartSkeleton(true)

        val onChanged = OnWCStatsV4Changed(
                1, granularity = DAYS, startDate = "2019-07-15", endDate = "2019-07-15"
        )
        onChanged.causeOfChange = FETCH_ORDER_STATS_V4
        presenter.onWCStatsV4Changed(onChanged)
        verify(dashboardView, times(1)).showChartSkeleton(false)
    }

    @Test
    fun `Show and hide unfilled orders skeleton correctly`() {
        presenter.takeView(dashboardView)
        presenter.fetchUnfilledOrderCount(forced = true)
        verify(dashboardView, times(1)).showUnfilledOrdersSkeleton(true)

        val totalOrders = 25
        val filter = CoreOrderStatus.PROCESSING.value
        presenter.onOrderChanged(OnOrderChanged(totalOrders, filter).apply {
            causeOfChange = FETCH_ORDERS_COUNT
        })

        verify(dashboardView, times(1)).showUnfilledOrdersSkeleton(false)
    }

    @Test
    fun `Show and hide top earners skeleton correctly`() {
        presenter.takeView(dashboardView)
        presenter.loadTopEarnerStats(StatsGranularity.DAYS, forced = true)
        verify(dashboardView, times(1)).showTopEarnersSkeleton(true)

        presenter.onWCTopEarnersChanged(OnWCTopEarnersChanged(emptyList(), StatsGranularity.DAYS).apply {
            causeOfChange = FETCH_TOP_EARNERS_STATS
        })
        verify(dashboardView, times(1)).showTopEarnersSkeleton(false)
    }
}
