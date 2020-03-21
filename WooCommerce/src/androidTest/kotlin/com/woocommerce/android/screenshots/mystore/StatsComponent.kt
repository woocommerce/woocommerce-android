package com.woocommerce.android.screenshots.mystore

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class StatsComponent : Screen {
    companion object {
        const val STATS_DASHBOARD = R.id.dashboard_stats
        const val STATS_DASHBOARD_DATE_RANGE = R.id.dashboard_date_range_value
    }

    constructor(): super(STATS_DASHBOARD)

    override fun recover() {
        super.recover()
        clickOn(R.id.reviews)
        clickOn(R.id.dashboard)
    }

    fun switchToStatsDashboardYearsTab() {
        selectItemWithTitleInTabLayout(R.string.dashboard_stats_granularity_years, R.id.tab_layout, STATS_DASHBOARD)
        idleFor(1000) // let stats load
    }
}