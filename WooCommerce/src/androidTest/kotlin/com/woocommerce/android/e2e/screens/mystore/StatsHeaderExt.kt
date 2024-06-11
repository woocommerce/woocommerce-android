package com.woocommerce.android.e2e.screens.mystore

import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.woocommerce.android.e2e.helpers.util.Screen
import com.woocommerce.android.ui.dashboard.stats.DashboardStatsTestTags

fun Screen.switchToStatsDateRange(
    rootTag: String,
    dateRangeName: Int,
    composeTestRule: ComposeTestRule
) {
    composeTestRule.onNodeWithTag(rootTag)
        .onChildren()
        .filterToOne(
            hasTestTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_BUTTON)
        )
        .performClick()

    composeTestRule.waitUntil(
        timeoutMillis = 1000,
        condition = {
            composeTestRule.onNodeWithTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_MENU)
                .isDisplayed()
        }
    )

    composeTestRule.onNodeWithTag(DashboardStatsTestTags.STATS_RANGE_DROPDOWN_MENU)
        .onChildren()
        .filterToOne(
            hasText(getTranslatedString(dateRangeName))
        )
        .performClick()
}
