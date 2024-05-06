package com.woocommerce.android.e2e.screens.mystore

import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsTestTags

class StatsComponent : Screen(R.id.dashboardStats_root) {
    override fun recover() {
        super.recover()
        clickOn(R.id.dashboard)
    }

    private fun waitForGraphToLoad() {
        // One option to ensure stats load is to idle for n seconds to give time to the network request to
        // finish. The timeout duration may or may not be enough though. Here's an option that hopes to be
        // a bit more flexible. Conversion rate value relies on orders and visitors (2 independent async requests)
        // to be loaded, so its a pretty reliable source to determine that the stats have finished
        // loading. I'm leaving the previous one and this comment for reference, just in case
        // the option doesn't prove to more reliable.
        // idleFor(1000)
        if (!waitForElementToBeDisplayedWithoutFailure(R.id.conversionValueTextView)) {
            recover()
            scrollTo(R.id.stats_view_row)
            waitForElementToBeDisplayed(R.id.conversionValueTextView)
            // idle for a bit in order to load labels as well
            idleFor(3000)
        } else {
            // idle for a bit in order to load labels as well
            idleFor(3000)
        }
    }

    fun switchToStatsDashboardTodayTab(composeTestRule: ComposeTestRule): MyStoreScreen {
        return composeTestRule.switchToStatsDashboardTab(R.string.today)
    }

    fun switchToStatsDashboardWeekTab(composeTestRule: ComposeTestRule): MyStoreScreen {
        return composeTestRule.switchToStatsDashboardTab(R.string.this_week)
    }

    fun switchToStatsDashboardMonthTab(composeTestRule: ComposeTestRule): MyStoreScreen {
        return composeTestRule.switchToStatsDashboardTab(R.string.this_month)
    }

    fun switchToStatsDashboardYearTab(composeTestRule: ComposeTestRule): MyStoreScreen {
        return composeTestRule.switchToStatsDashboardTab(R.string.this_year)
    }

    private fun ComposeTestRule.switchToStatsDashboardTab(tabName: Int): MyStoreScreen {
        onNodeWithTag(DashboardStatsTestTags.DASHBOARD_STATS_CARD)
            .onChildren()
            .filterToOne(
                hasTestTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_BUTTON)
            )
            .performClick()

        waitUntil(
            timeoutMillis = 1000,
            condition = {
                onNodeWithTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_MENU)
                    .isDisplayed()
            }
        )

        onNodeWithTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_MENU)
            .onChildren()
            .filterToOne(
                hasText(getTranslatedString(tabName))
            )
            .performClick()

        waitForGraphToLoad()

        return MyStoreScreen()
    }
}
