package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.dashboard_stats.view.*

class DashboardStatsView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : RelativeLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_stats, this)
    }

    enum class StatsTimeframe { THIS_WEEK, THIS_MONTH, THIS_YEAR, YEARS }

    fun initView() {
        barchart_progress.visibility = View.VISIBLE
        // TODO: Init tab layout with StatsTimeframe values
    }

    fun populateView(
        revenueStats: Map<String, Double>,
        orderStats: Map<String, Int>,
        timeframe: StatsTimeframe = getActiveTimeframe()
    ) {
        barchart_progress.visibility = View.GONE

        if (revenueStats.isEmpty()) {
            // TODO Replace with custom empty view
            chart.setNoDataTextColor(ContextCompat.getColor(context, R.color.default_text_color))
            chart.setNoDataText(context.getString(R.string.dashboard_state_no_data))
            chart.clear()
            return
        }
    }

    fun getActiveTimeframe(): StatsTimeframe {
        // TODO: Return state of timeframe selector
        return StatsTimeframe.THIS_MONTH
    }
}
