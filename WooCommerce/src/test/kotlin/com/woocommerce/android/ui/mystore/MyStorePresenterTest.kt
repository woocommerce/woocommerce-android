package com.woocommerce.android.ui.mystore

import com.nhaarman.mockitokotlin2.KArgumentCaptor
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.network.ConnectionChangeReceiver.ConnectionChangeEvent
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.widgets.WidgetUpdater.StatsWidgetUpdaters
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_HAS_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_REVENUE_STATS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_TOP_EARNERS_STATS
import org.wordpress.android.fluxc.action.WCStatsAction.FETCH_NEW_VISITOR_STATS
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchRevenueStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.FetchTopEarnersStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCRevenueStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCTopEarnersChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity.DAYS
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MyStorePresenterTest {
    private val myStoreView: MyStoreContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val wcStatsStore: WCStatsStore = mock()
    private val wcOrderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val networkStatus: NetworkStatus = mock()
    private val statsWidgetUpdaters: StatsWidgetUpdaters = mock()

    private lateinit var presenter: MyStorePresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        presenter = spy(MyStorePresenter(
                dispatcher, wooCommerceStore, wcStatsStore,
                wcOrderStore, selectedSite, networkStatus, statsWidgetUpdaters
        ))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()
        doReturn(true).whenever(networkStatus).isConnected()
        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Requests revenue stats data correctly`() {
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS)

        // note that we expect two dispatches because there's one to get stats and another to get visitors
        verify(dispatcher, times(2)).dispatch(actionCaptor.capture())
        assertEquals(FETCH_REVENUE_STATS, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchRevenueStatsPayload
        assertEquals(DAYS, payload.granularity)
    }

    @Test
    fun `Handles stats OnChanged result correctly`() {
        presenter.takeView(myStoreView)

        // Simulate OnChanged event from FluxC
        val onChanged = OnWCRevenueStatsChanged(
                1, DAYS, "2019-07-30", "2019-07-30"
        )
        onChanged.causeOfChange = FETCH_REVENUE_STATS
        presenter.onWCRevenueStatsChanged(onChanged)

        verify(myStoreView).showStats(anyOrNull(), eq(DAYS))
    }

    @Test
    fun `Handles stats OnChanged error result correctly`() {
        presenter.takeView(myStoreView)

        // Simulate OnChanged event from FluxC
        val onChanged = OnWCRevenueStatsChanged(1, granularity = DAYS).apply {
            causeOfChange = FETCH_REVENUE_STATS
            error = OrderStatsError(OrderStatsErrorType.INVALID_PARAM)
        }
        presenter.onWCRevenueStatsChanged(onChanged)
        verify(myStoreView, times(1)).showStatsError(DAYS)
    }

    @Test
    fun `Handles FETCH-ORDERS order event correctly`() {
        presenter.takeView(myStoreView)

        // Simulate onOrderChanged event: FETCH-ORDERS - My Store TAB should refresh
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDERS })
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `Handles UPDATE-ORDER-STATUS order event correctly`() {
        presenter.takeView(myStoreView)

        // Simulate onOrderChanged event: UPDATE-ORDER-STATUS - My Store TAB should refresh
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `Handles FETCH-ORDER-NOTES order event correctly`() {
        presenter.takeView(myStoreView)

        // Simulate onOrderChanged event: FETCH-ORDER-NOTES - My Store TAB should ignore
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDER_NOTES })
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = true)
    }

    @Test
    fun `Handles FETCH-ORDERS order event with error correctly`() {
        presenter.takeView(myStoreView)

        // Simulate onOrderChanged event: FETCH-ORDERS w/error - My Store TAB should ignore
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_ORDERS
            error = OrderError()
        })
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `Refreshes my store on network connected event if needed`() {
        presenter.takeView(myStoreView)
        doReturn(true).whenever(myStoreView).isRefreshPending

        // Simulate the network connected event
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(myStoreView, times(1)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `Does not refresh my store on network connected event if not needed`() {
        presenter.takeView(myStoreView)
        doReturn(false).whenever(myStoreView).isRefreshPending

        // Simulate the network connected event
        presenter.onEventMainThread(ConnectionChangeEvent(true))
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `Ignores network disconnected event correctly`() {
        presenter.takeView(myStoreView)

        // Simulate the network disconnected event
        presenter.onEventMainThread(ConnectionChangeEvent(false))
        verify(myStoreView, times(0)).refreshMyStoreStats(forced = any())
    }

    @Test
    fun `Requests top earners stats data correctly - forced`() {
        presenter.takeView(myStoreView)

        presenter.loadTopEarnerStats(DAYS, forced = true)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(FETCH_TOP_EARNERS_STATS, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchTopEarnersStatsPayload
        assertEquals(DAYS, payload.granularity)
        assertTrue(payload.forced)
    }

    @Test
    fun `Requests top earners stats data correctly - not forced`() {
        presenter.takeView(myStoreView)

        presenter.loadTopEarnerStats(DAYS, forced = false)
        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(FETCH_TOP_EARNERS_STATS, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchTopEarnersStatsPayload
        assertEquals(DAYS, payload.granularity)
    }

    @Test
    fun `Handles FETCH_TOP_EARNERS_STATS event correctly`() {
        presenter.takeView(myStoreView)

        val topEarners = ArrayList<WCTopEarnerModel>()
        topEarners.add(WCTopEarnerModel())
        presenter.onWCTopEarnersChanged(OnWCTopEarnersChanged(topEarners, DAYS).apply {
            causeOfChange = FETCH_TOP_EARNERS_STATS
        })
        verify(myStoreView, times(1)).showTopEarners(topEarners, DAYS)
    }

    @Test
    fun `Handles FETCH_TOP_EARNERS_STATS error event correctly`() {
        presenter.takeView(myStoreView)

        presenter.onWCTopEarnersChanged(OnWCTopEarnersChanged(emptyList(), DAYS).apply {
            causeOfChange = FETCH_TOP_EARNERS_STATS
            error = OrderStatsError(OrderStatsErrorType.INVALID_PARAM)
        })
        verify(myStoreView, times(1)).showTopEarnersError(DAYS)
    }

    @Test
    fun `Handles FETCH_HAS_ORDERS when there aren't any orders`() {
        presenter.takeView(myStoreView)
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_HAS_ORDERS })
        verify(myStoreView, times(1)).showEmptyView(true)
    }

    @Test
    fun `Handles FETCH_HAS_ORDERS when there are orders`() {
        presenter.takeView(myStoreView)
        presenter.onOrderChanged(OnOrderChanged(1).apply { causeOfChange = FETCH_HAS_ORDERS })
        verify(myStoreView, times(1)).showEmptyView(false)
    }

    @Test
    fun `Handles FETCH_NEW_VISITOR_STATS event correctly`() {
        presenter.takeView(myStoreView)

        val onChanged = OnWCStatsChanged(1, granularity = DAYS)
        onChanged.causeOfChange = FETCH_NEW_VISITOR_STATS

        presenter.onWCStatsChanged(onChanged)
        verify(myStoreView, times(1)).showVisitorStats(mapOf(), DAYS)
    }

    @Test
    fun `Handles FETCH_NEW_VISITOR_STATS error event correctly`() {
        presenter.takeView(myStoreView)

        val onChanged = OnWCStatsChanged(1, granularity = DAYS)
        onChanged.causeOfChange = FETCH_NEW_VISITOR_STATS
        onChanged.error = OrderStatsError(OrderStatsErrorType.INVALID_PARAM)

        presenter.onWCStatsChanged(onChanged)
        verify(myStoreView, times(1)).showVisitorStatsError(DAYS)
    }

    @Test
    fun `Show and hide stats skeleton correctly`() {
        presenter.takeView(myStoreView)
        presenter.loadStats(DAYS, forced = true)
        verify(myStoreView, times(1)).showChartSkeleton(true)

        val onChanged = OnWCRevenueStatsChanged(
                1, granularity = DAYS, startDate = "2019-07-30", endDate = "2019-07-30"
        )
        onChanged.causeOfChange = FETCH_REVENUE_STATS
        presenter.onWCRevenueStatsChanged(onChanged)
        verify(myStoreView, times(1)).showChartSkeleton(false)
    }

    @Test
    fun `Show and hide top earners skeleton correctly`() {
        presenter.takeView(myStoreView)
        presenter.loadTopEarnerStats(DAYS, forced = true)
        verify(myStoreView, times(1)).showTopEarnersSkeleton(true)

        presenter.onWCTopEarnersChanged(OnWCTopEarnersChanged(emptyList(), DAYS).apply {
            causeOfChange = FETCH_TOP_EARNERS_STATS
        })
        verify(myStoreView, times(1)).showTopEarnersSkeleton(false)
    }
}
