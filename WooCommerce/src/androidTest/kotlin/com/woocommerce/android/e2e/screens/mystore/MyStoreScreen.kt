package com.woocommerce.android.e2e.screens.mystore

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.helpers.util.StatsSummaryData
import org.hamcrest.Matchers
import org.json.JSONArray

class MyStoreScreen : Screen(MY_STORE) {
    companion object {
        const val MY_STORE = R.id.my_store_refresh_layout
    }

    val stats = StatsComponent()

    fun assertStatsSummary(summary: StatsSummaryData): MyStoreScreen {
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

    fun assertTopPerformers(topPerformersJSONArray: JSONArray): MyStoreScreen {
        scrollTo(R.id.topPerformers_recycler)
        // This idle is not needed for execution. Without it, the scroll,
        // assertion, and scroll back (at the end) happen so fast, that it'll
        // be impossible to see what's going on on the screen in FTL recordings,
        // if needed. The test will pass w/o it.
        idleFor(1000)

        for (i in 0 until topPerformersJSONArray.length()) {
            val innerArray = topPerformersJSONArray.getJSONArray(i)
            val topPerformerName = innerArray.getJSONObject(0).getString("value")
            val topPerformerSales = innerArray.getJSONObject(2).getString("value")

            Espresso.onView(
                Matchers.allOf(
                    // Assert there's a Top Performers container element
                    ViewMatchers.withId(R.id.topPerformers_recycler),

                    ViewMatchers.hasDescendant(
                        Matchers.allOf(
                            // Which has a product container
                            ViewMatchers.withId(R.id.product_container),
                            ViewMatchers.withChild(
                                Matchers.allOf(
                                    // With expected product name value as a child
                                    ViewMatchers.withId(R.id.text_ProductName),
                                    ViewMatchers.withText(topPerformerName)
                                )
                            ),
                            ViewMatchers.withChild(
                                Matchers.allOf(
                                    // And with expected product net sales value as a child
                                    ViewMatchers.withId(R.id.netSalesTextView),
                                    ViewMatchers.withText("Net sales: $$topPerformerSales.00")
                                )
                            )
                        )
                    ),
                )
            )
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        }

        scrollTo(R.id.stats_view_row)
        return this
    }
}
