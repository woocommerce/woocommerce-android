@file:Suppress("DEPRECATION")

package com.woocommerce.android.e2e.tests.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.rule.ActivityTestRule
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.e2e.helpers.InitializationRule
import com.woocommerce.android.e2e.helpers.TestBase
import com.woocommerce.android.e2e.helpers.util.MocksReader
import com.woocommerce.android.e2e.helpers.util.StatsSummaryData
import com.woocommerce.android.e2e.screens.TabNavComponent
import com.woocommerce.android.e2e.screens.login.WelcomeScreen
import com.woocommerce.android.e2e.screens.mystore.MyStoreScreen
import com.woocommerce.android.ui.login.LoginActivity
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class StatsUITest : TestBase() {
    @get:Rule(order = 0)
    val rule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    @get:Rule(order = 2)
    val initRule = InitializationRule()

    @get:Rule(order = 3)
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun setUp() {
        WelcomeScreen
            .skipCarouselIfNeeded()
            .selectLogin()
            .proceedWith(BuildConfig.SCREENSHOTS_URL)
            .proceedWith(BuildConfig.SCREENSHOTS_USERNAME)
            .proceedWith(BuildConfig.SCREENSHOTS_PASSWORD)

        TabNavComponent()
            .gotoMyStoreScreen()
    }

    private val todayStats = StatsSummaryData(
        revenue = "$111.00",
        orders = "44",
        visitors = "440",
    )

    private val weekStats = StatsSummaryData(
        revenue = "$6,470.00",
        orders = "440",
        visitors = "440",
    )

    private val weekMiddleStats = StatsSummaryData(
        revenue = "$620.00",
        orders = "5",
        visitors = "0",
    )

    private val yearStats = StatsSummaryData(
        revenue = "$10,391.92",
        orders = "6000",
        visitors = "12000",
    )

    @Test
    fun e2eStatsSummary() {
        MyStoreScreen()
            .stats.switchToStatsDashboardTodayTab()
            .assertStatsSummary(todayStats)
            .stats.switchToStatsDashboardWeekTab()
            .assertStatsSummary(weekStats)
            .stats.switchToStatsDashboardYearTab()
            .assertStatsSummary(yearStats)
    }

    @Test
    fun e2eStatsTopPerformers() {
        val topPerformersJSONArray = MocksReader().readStatsTopPerformersToArray()

        MyStoreScreen()
            .stats.switchToStatsDashboardTodayTab()
            .assertTopPerformers(topPerformersJSONArray)
    }

    @Ignore
    @Test
    fun e2eStatsTapChart() {
        MyStoreScreen()
            .stats.switchToStatsDashboardWeekTab()
            .assertStatsSummary(weekStats)
            .tapChartMiddle()
            .assertStatsSummary(weekMiddleStats)
    }
}
