package com.woocommerce.android.ui.widgets

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.widgets.stats.TodayWidgetListItem
import com.woocommerce.android.ui.widgets.stats.TodayWidgetListViewModel
import com.woocommerce.android.ui.widgets.stats.TodayWidgetListViewRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

class TodayWidgetListViewModelTest : BaseUnitTest() {
    private val resourceProvider: ResourceProvider = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val selectedSite: SelectedSite = mock()
    private val widgetRepository: TodayWidgetListViewRepository = mock()

    private val siteModel = SiteModel().apply { siteId = 1 }
    private val currencyCode = "$"
    private val appWidgetId: Int = 1

    private lateinit var viewModel: TodayWidgetListViewModel

    @Before
    fun setup() {
        viewModel = spy(
                TodayWidgetListViewModel(
                        resourceProvider,
                        currencyFormatter,
                        selectedSite,
                        widgetRepository,
                        appPrefsWrapper
                )
        )
        doReturn(siteModel).whenever(selectedSite).get()
        doReturn(currencyCode).whenever(widgetRepository).getStatsCurrency()
        doReturn(true).whenever(widgetRepository).userIsLoggedIn()
        doReturn(true).whenever(appPrefsWrapper).isUsingV4Api

        viewModel.start(appWidgetId)
    }

    @Test
    fun `builds widget ui model`() {
        val revenueKey = "Revenue"
        val visitorsKey = "Visitors"
        val ordersKey = "Orders"
        val revenue = 500
        val orders = 5
        val visitors = 100
        val revenueWithCurrency = "$currencyCode$revenue"

        whenever(resourceProvider.getString(R.string.dashboard_stats_revenue)).thenReturn(revenueKey)
        whenever(resourceProvider.getString(R.string.dashboard_stats_orders)).thenReturn(ordersKey)
        whenever(resourceProvider.getString(R.string.dashboard_stats_visitors)).thenReturn(visitorsKey)
        whenever(widgetRepository.getTodayRevenueStats()).thenReturn(
                WCRevenueStatsModel().apply {
                    this.localSiteId = siteModel.id
                    this.interval = StatsGranularity.DAYS.toString()
                    this.total = "{\"orders_count\":$orders,\"total_sales\":$revenue}"
                }
        )

        whenever(widgetRepository.getTodayVisitorStats()).thenReturn(visitors.toString())
        whenever(currencyFormatter.formatCurrencyRounded(any(), any())).thenReturn(revenueWithCurrency)

        viewModel.onDataSetChanged { }

        // verify if the widget data size is 3. The current day stats widget should include;
        // revenue, order count, visitor count in that order
        assertThat(viewModel.data).hasSize(3)
        assertListItem(viewModel.data[0], visitorsKey, visitors.toString())
        assertListItem(viewModel.data[2], revenueKey, revenueWithCurrency)
        assertListItem(viewModel.data[1], ordersKey, orders.toString())
    }

    @Test
    fun `shows error when user not logged in`() {
        whenever(widgetRepository.userIsLoggedIn()).thenReturn(false)

        var onError: Int? = null
        viewModel.onDataSetChanged { appWidgetId -> onError = appWidgetId }

        assertThat(onError).isEqualTo(appWidgetId)
    }

    private fun assertListItem(listItem: TodayWidgetListItem, key: String, value: String) {
        assertThat(listItem.layout).isEqualTo(R.layout.stats_widget_list_item)
        assertThat(listItem.localSiteId).isEqualTo(siteModel.siteId)
        assertThat(listItem.key).isEqualTo(key)
        assertThat(listItem.value).isEqualTo(value)
    }
}
