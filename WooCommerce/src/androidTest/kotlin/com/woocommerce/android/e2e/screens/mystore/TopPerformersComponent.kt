package com.woocommerce.android.e2e.screens.mystore

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsTestTags

class TopPerformersComponent : Screen(R.id.dashboardStats_root) {
    override fun recover() {
        super.recover()
        clickOn(R.id.dashboard)
    }

    fun switchToStatsDashboardTodayTab(composeTestRule: ComposeTestRule): DashboardScreen {
        composeTestRule.scrollToNodeThatMatches(
            hasTestTag(DashboardStatsTestTags.DASHBOARD_TOP_PERFORMERS_CARD)
        )

        switchToStatsDateRange(
            rootTag = DashboardStatsTestTags.DASHBOARD_TOP_PERFORMERS_CARD,
            dateRangeName = R.string.today,
            composeTestRule = composeTestRule
        )

        return DashboardScreen()
    }
}
