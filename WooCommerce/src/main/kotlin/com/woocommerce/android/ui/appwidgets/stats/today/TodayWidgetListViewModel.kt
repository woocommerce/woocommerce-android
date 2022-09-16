package com.woocommerce.android.ui.appwidgets.stats.today

import androidx.annotation.LayoutRes
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCStatsStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class TodayWidgetListViewModel @Inject constructor(
    private val selectedSite: SelectedSite,
    private val resourceProvider: ResourceProvider,
    private val currencyFormatter: CurrencyFormatter,
    private val wooCommerceStore: WooCommerceStore,
    private val getWidgetStats: GetWidgetStats,
) {
    private var appWidgetId: Int? = null

    private val mutableData = mutableListOf<TodayWidgetListItem>()
    val data: List<TodayWidgetListItem> = mutableData

    fun start(appWidgetId: Int) {
        this.appWidgetId = appWidgetId
    }

    fun onDataSetChanged(onError: (appWidgetId: Int) -> Unit) {
        val site = selectedSite.getIfExists()
        if (site == null) {
            appWidgetId?.let { onError(it) }
            return
        }

        // fetch current day stats from API. The data is fetched synchronously. The widget will remain
        // in its current state while work is being done here, and a loading view will show up in lieu
        // of the actual contents in the interim
        val todayStatsResult = runBlocking { getWidgetStats(WCStatsStore.StatsGranularity.DAYS, site) }

        if (todayStatsResult.isError) {
            appWidgetId?.let { onError(it) }
        } else {
            val uiModels = buildListItemUiModel(
                site = site,
                stats = todayStatsResult.model!!
            )
            if (uiModels != data) {
                mutableData.clear()
                mutableData.addAll(uiModels)
            }
        }
    }

    private fun buildListItemUiModel(
        site: SiteModel,
        stats: GetWidgetStats.WidgetStats
    ): List<TodayWidgetListItem> {
        val layout = R.layout.stats_widget_list_item
        val localSiteId = site.siteId.toInt()
        val currencyCode = wooCommerceStore.getSiteSettings(site)?.currencyCode.orEmpty()

        return listOf(
            TodayWidgetListItem(
                layout,
                localSiteId,
                resourceProvider.getString(string.dashboard_stats_revenue),
                currencyFormatter.getFormattedAmountZeroRounded(
                    stats.revenueGross,
                    currencyCode
                )
            ),
            TodayWidgetListItem(
                layout,
                localSiteId,
                resourceProvider.getString(string.dashboard_stats_orders),
                stats.ordersTotal.toString()
            ),
            TodayWidgetListItem(
                layout,
                localSiteId,
                resourceProvider.getString(string.dashboard_stats_visitors),
                stats.visitorsTotal.toString()
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
