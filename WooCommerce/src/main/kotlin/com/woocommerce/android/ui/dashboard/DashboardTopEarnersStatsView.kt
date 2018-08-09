package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.design.widget.TabLayout
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import kotlinx.android.synthetic.main.dashboard_stats.view.*
import kotlinx.android.synthetic.main.dashboard_top_earners.view.*
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.Timer
import java.util.TimerTask

class DashboardTopEarnersStatsView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_top_earners, this)
    }

    companion object {
        private val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
        private const val PROGRESS_DELAY_TIME_MS = 200L
    }

    var activeGranularity: StatsGranularity = DEFAULT_STATS_GRANULARITY
        get() {
            return tab_layout.getTabAt(tab_layout.selectedTabPosition)?.let {
                it.tag as StatsGranularity
            } ?: DEFAULT_STATS_GRANULARITY
        }

    private lateinit var selectedSite: SelectedSite

    private var chartRevenueStats = mapOf<String, Double>()
    private var chartCurrencyCode: String? = null

    private var progressDelayTimer: Timer? = null
    private var progressDelayTimerTask: TimerTask? = null

    fun initView(
        period: StatsGranularity = DEFAULT_STATS_GRANULARITY,
        listener: DashboardStatsListener,
        selectedSite: SelectedSite
    ) {
        this.selectedSite = selectedSite

        StatsGranularity.values().forEach { granularity ->
            val tab = tab_layout.newTab().apply {
                setText(DashboardUtils.getStringForGranularity(granularity))
                tag = granularity
            }
            topEarners_tab_layout.addTab(tab)

            // Start with the given time period selected
            if (granularity == period) {
                tab.select()
            }
        }

        topEarners_tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                listener.onRequestLoadTopEarnerStats(tab.tag as StatsGranularity)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }
}
