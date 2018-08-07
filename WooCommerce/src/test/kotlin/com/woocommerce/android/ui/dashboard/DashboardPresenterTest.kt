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
import com.woocommerce.android.tools.SelectedSite
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS_COUNT
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCOrderStore.FetchOrdersCountPayload
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WCStatsStore.FetchOrderStatsPayload
import org.wordpress.android.fluxc.store.WCStatsStore.OnWCStatsChanged
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsError
import org.wordpress.android.fluxc.store.WCStatsStore.OrderStatsErrorType
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DashboardPresenterTest {
    private val dashboardView: DashboardContract.View = mock()
    private val dispatcher: Dispatcher = mock()
    private val wcStatsStore: WCStatsStore = mock()
    private val wcOrderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()

    private lateinit var presenter: DashboardPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        presenter = spy(DashboardPresenter(dispatcher, wcStatsStore, wcOrderStore, selectedSite))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()

        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Requests order stats data correctly`() {
        presenter.takeView(dashboardView)
        presenter.loadStats(StatsGranularity.DAYS)

        verify(dispatcher, times(1)).dispatch(actionCaptor.capture())
        assertEquals(WCStatsAction.FETCH_ORDER_STATS, actionCaptor.firstValue.type)

        val payload = actionCaptor.firstValue.payload as FetchOrderStatsPayload
        assertEquals(StatsGranularity.DAYS, payload.granularity)
        assertFalse(payload.forced)
    }

    @Test
    fun `Handles stats OnChanged result correctly`() {
        presenter.takeView(dashboardView)

        // Simulate OnChanged event from FluxC
        val onChanged = OnWCStatsChanged(1, granularity = StatsGranularity.DAYS)
        presenter.onWCStatsChanged(onChanged)

        verify(dashboardView).showStats(any(), any(), eq(StatsGranularity.DAYS))
    }

    @Test
    fun `Handles stats OnChanged error result correctly`() {
        presenter.takeView(dashboardView)

        // Simulate OnChanged event from FluxC
        val onChanged = OnWCStatsChanged(1, granularity = StatsGranularity.DAYS).apply {
            error = OrderStatsError(OrderStatsErrorType.INVALID_PARAM)
        }
        presenter.onWCStatsChanged(onChanged)

        verify(dashboardView).showStats(eq(emptyMap()), eq(emptyMap()), eq(StatsGranularity.DAYS))
    }

    @Test
    fun `Requests orders to fulfill count correctly`() {
        presenter.takeView(dashboardView)
        presenter.loadOrdersToFulfillCount()

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
        presenter.loadOrdersToFulfillCount()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        presenter.onOrderChanged(OnOrderChanged(totalOrders, filter).apply {
            causeOfChange = FETCH_ORDERS_COUNT
        })

        verify(dashboardView).showOrdersCard(totalOrders)
    }

    @Test
    fun `Hides orders card correctly`() {
        val totalOrders = 0
        val filter = CoreOrderStatus.PROCESSING.value
        presenter.takeView(dashboardView)
        presenter.loadOrdersToFulfillCount()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        presenter.onOrderChanged(OnOrderChanged(totalOrders, filter).apply {
            causeOfChange = FETCH_ORDERS_COUNT
        })

        verify(dashboardView).hideOrdersCard()
    }

    @Test
    fun `Hides orders card on error correctly`() {
        val totalOrders = 25
        val filter = CoreOrderStatus.PROCESSING.value
        presenter.takeView(dashboardView)
        presenter.loadOrdersToFulfillCount()
        verify(dispatcher, times(1)).dispatch(any<Action<FetchOrdersCountPayload>>())

        presenter.onOrderChanged(OnOrderChanged(totalOrders, filter).apply {
            causeOfChange = FETCH_ORDERS_COUNT
            error = OrderError()
        })

        verify(dashboardView).hideOrdersCard()
    }

    @Test
    fun `Handles FETCH-ORDERS order event correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: FETCH-ORDERS - Dashboard should refresh
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDERS })
        verify(dashboardView, times(0)).showOrdersCard(any())
        verify(dashboardView, times(0)).hideOrdersCard()
        verify(dashboardView, times(1)).refreshDashboard()
    }

    @Test
    fun `Handles UPDATE-ORDER-STATUS order event correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: UPDATE-ORDER-STATUS - Dashboard should refresh
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = UPDATE_ORDER_STATUS })
        verify(dashboardView, times(0)).showOrdersCard(any())
        verify(dashboardView, times(0)).hideOrdersCard()
        verify(dashboardView, times(1)).refreshDashboard()
    }

    @Test
    fun `Handles FETCH-ORDER-NOTES order event correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: FETCH-ORDER-NOTES - Dashboard should ignore
        presenter.onOrderChanged(OnOrderChanged(0).apply { causeOfChange = FETCH_ORDER_NOTES })
        verify(dashboardView, times(0)).showOrdersCard(any())
        verify(dashboardView, times(0)).hideOrdersCard()
        verify(dashboardView, times(0)).refreshDashboard()
    }

    @Test
    fun `Handles FETCH-ORDERS order event with error correctly`() {
        presenter.takeView(dashboardView)

        // Simulate onOrderChanged event: FETCH-ORDERS w/error - Dashboard should ignore
        presenter.onOrderChanged(OnOrderChanged(0).apply {
            causeOfChange = FETCH_ORDERS
            error = OrderError()
        })
        verify(dashboardView, times(0)).showOrdersCard(any())
        verify(dashboardView, times(0)).hideOrdersCard()
        verify(dashboardView, times(0)).refreshDashboard()
    }
}
