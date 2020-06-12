package com.woocommerce.android.ui.widgets.stats.today

import androidx.annotation.LayoutRes
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.ui.widgets.WidgetColorMode
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class TodayWidgetListViewModel @Inject constructor(
    private val siteStore: SiteStore,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val repository: TodayWidgetListViewRepository,
    private val appPrefsWrapper: AppPrefsWrapper
) {
    private var siteId: Int? = null
    private var colorMode: WidgetColorMode = WidgetColorMode.LIGHT
    private var appWidgetId: Int? = null

    private val mutableData = mutableListOf<TodayWidgetListItem>()
    val data: List<TodayWidgetListItem> = mutableData

    fun start(siteId: Int, colorMode: WidgetColorMode, appWidgetId: Int) {
        this.siteId = siteId
        this.colorMode = colorMode
        this.appWidgetId = appWidgetId
    }

    fun onDataSetChanged(onError: (appWidgetId: Int) -> Unit) {
        // currently the stats widget data can only be fetched if the user is logged in. If user is not
        // logged in, display an error message in the widget
        if (!repository.userIsLoggedIn()) {
            appWidgetId?.let { onError(it) }
            return
        }

        if (siteId == null) {
            appWidgetId?.let { onError(it) }
            return
        }

        val site = siteId?.let { siteStore.getSiteByLocalId(it) }
        if (site == null) {
            appWidgetId?.let { onError(it) }
            return
        }

        // fetch current day stats from API. The data is fetched synchronously. The widget will remain
        // in its current state while work is being done here, and a loading view will show up in lieu
        // of the actual contents in the interim
        runBlocking {
            repository.fetchTodayStats(site)
        }

        // The v4 stats API is only available if user has the WC-Admin plugin or uses a WooCommerce version >= 4.0.
        // If v4 stats is not available, an error message is displayed in the widget
        if (!appPrefsWrapper.isUsingV4Api) {
            appWidgetId?.let { onError(it) }
            return
        }

        // get the current day stats from the local db and build a UI model for the widget now that
        // the v4 stats is available
        val revenueStats = repository.getTodayRevenueStats(site)
        val visitorStats = repository.getTodayVisitorStats(site)
        val currencyCode = repository.getStatsCurrency(site)
        val uiModels = buildListItemUiModel(revenueStats, visitorStats, currencyCode, site)
        if (uiModels != data) {
            mutableData.clear()
            mutableData.addAll(uiModels)
        }
    }

    private fun buildListItemUiModel(
        revenueStats: WCRevenueStatsModel?,
        visitorCount: String,
        currencyCode: String?,
        site: SiteModel
    ): List<TodayWidgetListItem> {
        val layout = when (colorMode) {
            WidgetColorMode.DARK -> R.layout.stats_widget_list_item_dark
            WidgetColorMode.LIGHT -> R.layout.stats_widget_list_item_light
        }
        val grossRevenue = revenueStats?.getTotal()?.totalSales ?: 0.0
        val orderCount = revenueStats?.getTotal()?.ordersCount ?: 0
        val localSiteId = site.siteId.toInt()

        val formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded

        return listOf(
            TodayWidgetListItem(
                layout,
                localSiteId,
                resourceProvider.getString(string.dashboard_stats_revenue),
                formatCurrencyForDisplay(grossRevenue, currencyCode.orEmpty())
            ),
            TodayWidgetListItem(
                layout,
                localSiteId,
                resourceProvider.getString(string.dashboard_stats_orders),
                orderCount.toString()
            ),
            TodayWidgetListItem(
                layout,
                localSiteId,
                resourceProvider.getString(string.dashboard_stats_visitors),
                visitorCount
            )
        )
    }

    data class TodayWidgetListItem(
        @LayoutRes val layout: Int,
        val localSiteId: Int,
        val key: String,
        val value: String
    )
}
