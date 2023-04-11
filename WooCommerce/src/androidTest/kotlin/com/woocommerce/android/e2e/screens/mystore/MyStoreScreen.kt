package com.woocommerce.android.e2e.screens.mystore

import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import com.woocommerce.android.R
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.e2e.helpers.util.StatsSummaryData
import com.woocommerce.android.e2e.helpers.util.StatsTopPerformerData
import org.hamcrest.Matchers

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

    fun assertTopPerformer(topPerformer: StatsTopPerformerData): MyStoreScreen {
        scrollTo(R.id.topPerformers_recycler)
        idleFor(1000)

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
                                ViewMatchers.withText(topPerformer.name)
                            )
                        ),
                        ViewMatchers.withChild(
                            Matchers.allOf(
                                // With expected product net sales value as a child
                                ViewMatchers.withId(R.id.netSalesTextView),
                                ViewMatchers.withText(topPerformer.netSales)
                            )
                        )
                    )
                ),
            )
        )
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        scrollTo(R.id.stats_view_row)
        return this
    }
}
