package com.woocommerce.android.e2e.screens.mystore

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.helpers.util.StatsSummaryData
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsTestTags
import org.hamcrest.Matchers
import org.json.JSONArray

class DashboardScreen : Screen(R.id.my_store_refresh_layout) {
    val stats = StatsComponent()
    val topPerformers = TopPerformersComponent()

    fun tapChartMiddle(): DashboardScreen {
        scrollTo(R.id.chart)
        clickOn(R.id.chart)
        return this
    }

    fun assertStatsSummary(summary: StatsSummaryData): DashboardScreen {
        Espresso.onView(
            Matchers.allOf(
                // Assert there's a Stats container element
                ViewMatchers.withId(R.id.stats_view_row),

                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        // Which has a descendant with "Revenue" label
                        ViewMatchers.withText(R.string.dashboard_stats_revenue),
                        // Which has a sibling (meaning they both belong to the same immediate container)
                        // with expected Revenue value
                        ViewMatchers.hasSibling(
                            Matchers.allOf(
                                ViewMatchers.withId(R.id.totalRevenueTextView),
                                ViewMatchers.withText(summary.revenue)
                            )
                        )
                    )
                ),

                // Same as above but for Orders
                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.ordersLabel),
                        ViewMatchers.withText(R.string.dashboard_stats_orders),
                        ViewMatchers.hasSibling(
                            Matchers.allOf(
                                ViewMatchers.withId(R.id.ordersValueTextView),
                                ViewMatchers.withText(summary.orders)
                            )
                        )
                    )
                ),

                // Same as above but for Visitors
                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.visitorsLabelTextview),
                        ViewMatchers.withText(R.string.dashboard_stats_visitors),
                        ViewMatchers.hasSibling(
                            Matchers.allOf(
                                ViewMatchers.withId(R.id.visitorsValueTextview),
                                ViewMatchers.withText(summary.visitors)
                            )
                        )
                    )
                ),

                // Same as above but for Conversion
                ViewMatchers.hasDescendant(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.conversionLabelTextView),
                        ViewMatchers.withText(R.string.dashboard_stats_conversion),
                        ViewMatchers.hasSibling(
                            Matchers.allOf(
                                ViewMatchers.withId(R.id.conversionValueTextView),
                                ViewMatchers.withText(summary.conversion)
                            )
                        )
                    )
                ),
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        return this
    }

    fun assertTopPerformers(
        topPerformersJSONArray: JSONArray,
        composeTestRule: ComposeTestRule
    ): DashboardScreen {
        composeTestRule.onNodeWithTag(DashboardStatsTestTags.DASHBOARD_TOP_PERFORMERS_CARD)
            .assertIsDisplayed()

        for (i in 0 until topPerformersJSONArray.length()) {
            val innerArray = topPerformersJSONArray.getJSONArray(i)
            val topPerformerName = innerArray.getJSONObject(0).getString("value")
            val topPerformerSales = innerArray.getJSONObject(2).getString("value")

            composeTestRule.onNodeWithText(topPerformerName)
                .assertIsDisplayed()
            composeTestRule.onNodeWithText("Net sales: $$topPerformerSales.00")
                .assertIsDisplayed()
        }

        return this
    }
}
