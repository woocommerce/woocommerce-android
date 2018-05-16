package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.woocommerce.android.R

class DashboardStatsView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : RelativeLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_stats, this)
    }

    enum class StatsTimeframe { THIS_WEEK, THIS_MONTH, THIS_YEAR, YEARS }

    fun initView() {}

    fun getActiveTimeframe(): StatsTimeframe {
        // TODO: Return state of timeframe selector
        return StatsTimeframe.THIS_MONTH
    }
}
