package com.woocommerce.android.screenshots.mystore

import com.woocommerce.android.R
import com.woocommerce.android.screenshots.util.Screen

class StatsComponent : Screen(STATS_DASHBOARD) {
    companion object {
        const val STATS_DASHBOARD = R.id.dashboard_stats
    }

    override fun recover() {
        super.recover()
        clickOps.clickOn(R.id.reviews)
        clickOps.clickOn(R.id.dashboard)
    }

    private fun waitForGraphToLoad() {
        // One option to ensure stats load is to idle for n seconds to give time to the network request to
        // finish. The timeout duration may or may not be enough though. Here's an option that hopes to be
        // a bit more flexible. I'm leaving the previous one and this comment for reference, just in case
        // the option doesn't prove to more reliable.
        // idleFor(1000)
        if (!waitOps.waitForElementToBeDisplayedWithoutFailure(R.id.dashboard_recency_text)) {
            recover()
            waitOps.waitForElementToBeDisplayed(R.id.dashboard_recency_text)
            // idle for a bit in order to load labels as well
            waitOps.idleFor(3000)
        } else {
            // idle for a bit in order to load labels as well
            waitOps.idleFor(3000)
        }
    }

    fun switchToStatsDashboardYearsTab() {
        selectOps.selectItemWithTitleInTabLayout(R.string.dashboard_stats_granularity_years, R.id.tab_layout, STATS_DASHBOARD)
        waitForGraphToLoad()
    }
}
