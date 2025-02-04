package org.wordpress.android.fluxc.wc.stats

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.persistence.WCStatsSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCStatsSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(WCRevenueStatsModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    @Suppress("LongMethod")
    fun testSimpleInsertionAndRetrievalOfRevenueStats() {
        // insert a first stats entry and verify that it is stored correctly
        val revenueStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel()
        WCStatsSqlUtils.insertOrUpdateRevenueStats(revenueStatsModel)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(1, size)
            assertEquals(revenueStatsModel.interval, first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
        }

        // Create a second stats entry for this site with same start & end date but with different interval
        val revenueStatsModel2 =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = StatsGranularity.MONTHS.toString(), data = "fake-data"
                )
        WCStatsSqlUtils.insertOrUpdateRevenueStats(revenueStatsModel2)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(2, size)
            assertEquals(revenueStatsModel.interval, first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals(revenueStatsModel2.interval, get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
        }

        // Create a third stats entry for this site with same interval but different start & end date
        val revenueStatsModel3 =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        data = "fake-data2", startDate = "2019-07-07 00:00:00", endDate = "2019-07-13 23:59:59"
                )
        WCStatsSqlUtils.insertOrUpdateRevenueStats(revenueStatsModel3)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals(revenueStatsModel.interval, first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals(revenueStatsModel2.interval, get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
            assertEquals(revenueStatsModel3.interval, get(2).interval)
            assertEquals(revenueStatsModel3.startDate, get(2).startDate)
            assertEquals(revenueStatsModel3.endDate, get(2).endDate)
        }

        // Overwrite an existing entry and verify that update is happening correctly
        val revenueStatsModel4 = WCStatsTestUtils.generateSampleRevenueStatsModel()
        WCStatsSqlUtils.insertOrUpdateRevenueStats(revenueStatsModel4)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(3, size)
            assertEquals(revenueStatsModel.interval, first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals(revenueStatsModel2.interval, get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
            assertEquals(revenueStatsModel3.interval, get(2).interval)
            assertEquals(revenueStatsModel3.startDate, get(2).startDate)
            assertEquals(revenueStatsModel3.endDate, get(2).endDate)
        }

        // Add another "day" entry, but for another site
        val revenueStatsModel5 = WCStatsTestUtils.generateSampleRevenueStatsModel(localSiteId = 8)
        WCStatsSqlUtils.insertOrUpdateRevenueStats(revenueStatsModel5)

        with(WellSql.select(WCRevenueStatsModel::class.java).asModel) {
            assertEquals(4, size)
            assertEquals(revenueStatsModel.interval, first().interval)
            assertEquals(revenueStatsModel.startDate, first().startDate)
            assertEquals(revenueStatsModel.endDate, first().endDate)
            assertEquals(revenueStatsModel2.interval, get(1).interval)
            assertEquals(revenueStatsModel2.startDate, get(1).startDate)
            assertEquals(revenueStatsModel2.endDate, get(1).endDate)
            assertEquals(revenueStatsModel3.interval, get(2).interval)
            assertEquals(revenueStatsModel3.startDate, get(2).startDate)
            assertEquals(revenueStatsModel3.endDate, get(2).endDate)
            assertEquals(revenueStatsModel5.interval, get(3).interval)
            assertEquals(revenueStatsModel5.localSiteId, get(3).localSiteId)
            assertEquals(revenueStatsModel5.startDate, get(3).startDate)
            assertEquals(revenueStatsModel5.endDate, get(3).endDate)
        }
    }

    @Test
    @Suppress("LongMethod")
    fun testGetRawRevenueStatsForSiteAndUnit() {
        // revenue stats model for current day
        val currentDayStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel()
        val site = SiteModel().apply { id = currentDayStatsModel.localSiteId }
        WCStatsSqlUtils.insertOrUpdateRevenueStats(currentDayStatsModel)

        // revenue stats model for this week
        val currentWeekStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = StatsGranularity.WEEKS.toString(),
                        data = "fake-data", startDate = "2019-07-07", endDate = "2019-07-09"
                )
        WCStatsSqlUtils.insertOrUpdateRevenueStats(currentWeekStatsModel)

        // revenue stats model for this month
        val currentMonthStatsModel =
                WCStatsTestUtils.generateSampleRevenueStatsModel(
                        interval = StatsGranularity.MONTHS.toString(),
                        data = "fake-data", startDate = "2019-07-01", endDate = "2019-07-09"
                )
        WCStatsSqlUtils.insertOrUpdateRevenueStats(currentMonthStatsModel)

        // current day stats for alternate site
        val site2 = SiteModel().apply { id = 8 }
        val altSiteOrderStatsModel = WCStatsTestUtils.generateSampleRevenueStatsModel(
                localSiteId = site2.id
        )
        WCStatsSqlUtils.insertOrUpdateRevenueStats(altSiteOrderStatsModel)

        val currentDayStats = WCStatsSqlUtils.getRevenueStatsForSiteIntervalAndDate(
                site, StatsGranularity.DAYS, currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )
        assertNotNull(currentDayStats)
        with(currentDayStats) {
            assertEquals(currentDayStatsModel.interval, interval)
            assertEquals(currentDayStatsModel.startDate, startDate)
            assertEquals(currentDayStatsModel.endDate, endDate)
            assertEquals(currentDayStatsModel.localSiteId, localSiteId)
        }

        val currentWeekStats = WCStatsSqlUtils.getRevenueStatsForSiteIntervalAndDate(
                site, StatsGranularity.WEEKS, currentWeekStatsModel.startDate, currentWeekStatsModel.endDate
        )
        assertNotNull(currentWeekStats)
        with(currentWeekStats) {
            assertEquals(currentWeekStatsModel.interval, interval)
            assertEquals(currentWeekStatsModel.startDate, startDate)
            assertEquals(currentWeekStatsModel.endDate, endDate)
            assertEquals(currentWeekStatsModel.localSiteId, localSiteId)
        }

        val currentMonthStats = WCStatsSqlUtils.getRevenueStatsForSiteIntervalAndDate(
                site, StatsGranularity.MONTHS, currentMonthStatsModel.startDate, currentMonthStatsModel.endDate
        )
        assertNotNull(currentMonthStats)
        with(currentMonthStats) {
            assertEquals(currentMonthStatsModel.interval, interval)
            assertEquals(currentMonthStatsModel.startDate, startDate)
            assertEquals(currentMonthStatsModel.endDate, endDate)
            assertEquals(currentMonthStatsModel.localSiteId, localSiteId)
        }

        val altCurrentDayStats = WCStatsSqlUtils.getRevenueStatsForSiteIntervalAndDate(
                site2, StatsGranularity.DAYS, altSiteOrderStatsModel.startDate, altSiteOrderStatsModel.endDate
        )
        assertNotNull(altCurrentDayStats)
        with(altCurrentDayStats) {
            assertEquals(altCurrentDayStats.interval, interval)
            assertEquals(altSiteOrderStatsModel.startDate, startDate)
            assertEquals(altSiteOrderStatsModel.endDate, endDate)
            assertEquals(altSiteOrderStatsModel.localSiteId, localSiteId)
        }

        val nonExistentSite = WCStatsSqlUtils.getRevenueStatsForSiteIntervalAndDate(
                SiteModel().apply { id = 88 },
                StatsGranularity.DAYS, currentDayStatsModel.startDate, currentDayStatsModel.endDate
        )
        assertNull(nonExistentSite)

        val missingData = WCStatsSqlUtils.getRevenueStatsForSiteIntervalAndDate(
                site, StatsGranularity.YEARS, currentDayStatsModel.startDate, currentDayStatsModel.endDate)
        assertNull(missingData)
    }
}
