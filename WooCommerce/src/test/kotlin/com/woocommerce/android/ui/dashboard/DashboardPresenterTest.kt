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
import com.woocommerce.android.ui.dashboard.DashboardStatsView.StatsTimeframe
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCStatsAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
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
    private val selectedSite: SelectedSite = mock()

    private lateinit var presenter: DashboardPresenter

    private lateinit var actionCaptor: KArgumentCaptor<Action<*>>

    @Before
    fun setup() {
        presenter = spy(DashboardPresenter(dispatcher, wcStatsStore, selectedSite))
        // Use a dummy selected site
        doReturn(SiteModel()).whenever(selectedSite).get()

        actionCaptor = argumentCaptor()
    }

    @Test
    fun `Requests order stats data correctly`() {
        presenter.takeView(dashboardView)
        presenter.loadStats(StatsTimeframe.THIS_MONTH)

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
}
